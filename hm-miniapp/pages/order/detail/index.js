const api = require('../../../utils/api')
const { getOrderStatusMeta } = require('../../../utils/order')

Page({
  data: { detail: {}, paying: false, subscriptionTemplates: [], reminderEnabled: true, allowSms: false, savingPreference: false },
  async onLoad(options) {
    const detail = await api.getOrder(options.id)
    detail.statusMeta = getOrderStatusMeta(detail.orderStatus)
    this.setData({ detail })
    api.getSubscriptionTemplates().then(subscriptionTemplates => this.setData({ subscriptionTemplates })).catch(() => {})
    api.getNotificationPreferences().then(preferences => {
      const reminder = preferences.find(item => item.event_type === 'SERVICE_REMINDER')
      if (reminder) this.setData({ reminderEnabled: !!reminder.enabled, allowSms: !!reminder.allow_sms })
    }).catch(() => {})
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
    if (this.data.paying) return
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
      const detail = await api.getOrder(this.data.detail.orderId)
      detail.statusMeta = getOrderStatusMeta(detail.orderStatus)
      this.setData({ detail })
      wx.showToast({ title: detail.orderStatus === '10' ? '支付结果确认中' : '付款成功', icon: 'none' })
    } catch (error) {
      wx.showToast({ title: error.errMsg && error.errMsg.includes('cancel') ? '已取消支付' : '支付未完成，请刷新订单', icon: 'none' })
    } finally { this.setData({ paying: false }) }
  },
  async handleCancel() {
    await api.cancelOrder(this.data.detail.orderId, '用户取消预约')
    wx.showToast({ title: '已取消', icon: 'success' })
    const detail = await api.getOrder(this.data.detail.orderId)
    detail.statusMeta = getOrderStatusMeta(detail.orderStatus)
    this.setData({ detail })
  }
})
