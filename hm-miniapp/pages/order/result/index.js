const { getOrderStatusMeta } = require('../../../utils/order')

require('../../../utils/page')({
  data: {
    orderId: '',
    orderNo: '',
    payAmount: '',
    statusLabel: '',
    statusDesc: ''
  },
  onLoad(options) {
    const statusMeta = getOrderStatusMeta(options.orderStatus)
    this.setData({
      orderId: options.orderId || '',
      orderNo: options.orderNo || '',
      payAmount: options.payAmount || '',
      statusLabel: statusMeta.label,
      statusDesc: statusMeta.desc
    })
  },
  goOrderDetail() {
    wx.redirectTo({ url: `/pages/order/detail/index?id=${this.data.orderId}` })
  },
  goHome() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
