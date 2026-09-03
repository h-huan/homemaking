require('../../../utils/page')({
  data: { booking: null, selectedExtras: [], submitting: false },
  onShow() {
    const booking = wx.getStorageSync('pendingBooking')
    if (!booking || !booking.form) {
      wx.showToast({ title: '预约信息已失效', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 300)
      return
    }
    this.setData({ booking, selectedExtras: (booking.detail.extraItemList || []).filter(item => (booking.selectedExtraIds || []).includes(item.extraItemId)) })
  },
  async submitOrder() {
    if (this.data.submitting || !this.data.booking) return
    const api = require('../../../utils/api')
    this.setData({ submitting: true })
    try {
      const res = await api.submitOrder(this.data.booking.form)
      wx.removeStorageSync('pendingBooking')
      wx.redirectTo({ url: `/pages/order/result/index?orderId=${res.orderId}&orderNo=${res.orderNo}&payAmount=${res.payAmount}&orderStatus=${res.orderStatus}` })
    } catch (error) {
      wx.showToast({ title: (error && error.msg) || '提交预约失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
