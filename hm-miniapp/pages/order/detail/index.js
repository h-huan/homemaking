const api = require('../../../utils/api')
const { getOrderStatusMeta } = require('../../../utils/order')

require('../../../utils/page')({
  data: { detail: { statusMeta: {}, operateLogs: [], aftersales: [] }, evidence: [], paying: false, subscriptionTemplates: [], reminderEnabled: true, allowSms: false, savingPreference: false },
  async onLoad(options) {
    this.orderId = Number(options.id)
    await this.loadDetail()
    api.getSubscriptionTemplates().then(subscriptionTemplates => this.setData({ subscriptionTemplates })).catch(() => {})
    api.getNotificationPreferences().then(preferences => {
      const reminder = preferences.find(item => item.event_type === 'SERVICE_REMINDER')
      if (reminder) this.setData({ reminderEnabled: !!reminder.enabled, allowSms: !!reminder.allow_sms })
    }).catch(() => {})
  },
  onShow() {
    if (this.orderId && this.loadedOnce) this.loadDetail().catch(() => {})
    this.loadedOnce = true
  },
  async loadDetail() {
    const detail = await api.getOrder(this.orderId)
    detail.statusMeta = getOrderStatusMeta(detail.orderStatus, detail.paymentOptions)
    detail.paymentMethodLabel = ({ OFFLINE: '线下付款', ONLINE: '微信在线支付', LEGACY: '历史付款' })[detail.paymentMethod] || '待确认'
    detail.operateLogs = detail.operateLogs || []
    detail.aftersales = detail.aftersales || []
    const states = { PENDING_PAYMENT: '待补款', PENDING_REFUND: '待退差额', APPLIED: '已生效', CANCELLED: '已撤销' }
    detail.changes = (detail.changes || []).map(change => ({ ...change, stateLabel: states[change.status], oldAmount: (change.old_price_cents / 100).toFixed(2), newAmount: (change.new_price_cents / 100).toFixed(2) }))
    detail.canAmend = !detail.pendingChangeId && ['10', '30', '40'].includes(detail.orderStatus) && ['WAITING', 'ACCEPTED'].includes(detail.fulfillmentStatus)
    if (detail.pendingChange) detail.pendingChange.amount = (Math.abs(detail.pendingChange.difference_cents) / 100).toFixed(2)
    detail.fulfillmentLabel = ({ WAITING: '等待接单', ACCEPTED: '人员已接单', ARRIVED: '人员已到达', STARTED: '正在服务', COMPLETED: '服务已完成' })[detail.fulfillmentStatus] || '等待安排'
    this.setData({ detail })
    const evidence = await api.getEvidence(this.orderId)
    this.setData({ evidence })
  },
  previewEvidence(e) {
    const app = getApp()
    wx.downloadFile({
      url: app.globalData.baseUrl + '/homemaking/orders/' + this.orderId + '/evidence/' + e.currentTarget.dataset.id + '/content',
      header: { 'tenant-id': String(app.globalData.tenantId), Authorization: 'Bearer ' + wx.getStorageSync('miniToken') },
      success: result => { if (result.statusCode === 200) wx.previewImage({ urls: [result.tempFilePath] }); else wx.showToast({ title: '照片读取失败，请重新登录后重试', icon: 'none' }) },
      fail: () => wx.showToast({ title: '照片读取失败', icon: 'none' })
    })
  },
  handleSubscribe() {
    const ids = this.data.subscriptionTemplates
    if (!ids.length) return
    wx.requestSubscribeMessage({ tmplIds: ids, success: async result => {
      try {
        await Promise.all(ids.map(id => api.saveSubscription(id, result[id] === 'accept')))
        wx.showToast({ title: ids.some(id => result[id] === 'accept') ? '订阅设置已保存' : '未开启订阅', icon: 'none' })
      } catch (_) { wx.showToast({ title: '订阅设置保存失败', icon: 'none' }) }
    }, fail: () => wx.showToast({ title: '未开启订阅', icon: 'none' }) })
  },
  async handlePreference(e) {
    if (this.data.savingPreference) return
    const key = e.currentTarget.dataset.key
    if (!['reminderEnabled', 'allowSms'].includes(key)) return
    const previous = this.data[key]
    this.setData({ [key]: e.detail.value, savingPreference: true })
    try {
      await api.saveNotificationPreference({ event: 'SERVICE_REMINDER', enabled: this.data.reminderEnabled, allowSms: this.data.allowSms })
    } catch (_) { this.setData({ [key]: previous }) }
    finally { this.setData({ savingPreference: false }) }
  },
  async handlePay() {
    if (this.data.paying || !this.data.detail.paymentOptions || !this.data.detail.paymentOptions.onlineAvailable) return
    this.setData({ paying: true })
    try {
      const payment = await api.preparePayment(this.data.detail.orderId)
      if (payment.status !== 10) {
        const params = JSON.parse(payment.displayContent)
        await new Promise((resolve, reject) => wx.requestPayment({
          timeStamp: params.timeStamp, nonceStr: params.nonceStr,
          package: params.packageValue || params.package, signType: params.signType,
          paySign: params.paySign, success: resolve, fail: reject
        }))
      }
      await api.syncPayment(this.data.detail.orderId)
      await this.loadDetail()
      wx.showToast({ title: this.data.detail.orderStatus === '10' ? '支付结果确认中' : '付款成功', icon: 'none' })
    } catch (error) {
      wx.showToast({ title: error.errMsg && error.errMsg.includes('cancel') ? '已取消支付' : '支付未完成，请刷新订单', icon: 'none' })
    } finally { this.setData({ paying: false }) }
  },
  async handleCancel() {
    await api.cancelOrder(this.data.detail.orderId, '用户取消预约')
    wx.showToast({ title: '已取消', icon: 'success' })
    await this.loadDetail()
  },
  goReschedule() { wx.navigateTo({ url: `/pages/order/reschedule/index?id=${this.data.detail.orderId}` }) },
  goAddressChange() { if (this.data.detail.canAmend) wx.navigateTo({ url: `/pages/order/address-change/index?id=${this.data.detail.orderId}` }) },
  cancelChange() {
    const change = this.data.detail.pendingChange
    if (!change || change.actor_type !== 1) return
    wx.showModal({ title: '撤销地址变更', content: '未生效的新地址和价格将作废，继续保留原约定。', success: async result => {
      if (!result.confirm) return
      await api.cancelOrderChange(this.orderId, change.id, '客户撤销尚未生效的地址变更')
      await this.loadDetail()
    } })
  },
  goReview() { wx.navigateTo({ url: `/pages/order/feedback/index?id=${this.data.detail.orderId}&mode=review` }) },
  goAftersale() { wx.navigateTo({ url: `/pages/order/feedback/index?id=${this.data.detail.orderId}&mode=aftersale&amount=${this.data.detail.refundableAmount}` }) }
})
