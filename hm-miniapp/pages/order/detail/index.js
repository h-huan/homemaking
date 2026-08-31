const api = require('../../../utils/api')
const { getOrderStatusMeta } = require('../../../utils/order')

Page({
  data: { detail: {} },
  async onLoad(options) {
    const detail = await api.getOrder(options.id)
    detail.statusMeta = getOrderStatusMeta(detail.orderStatus)
    this.setData({ detail })
  },
  async handleCancel() {
    await api.cancelOrder(this.data.detail.orderId, '用户取消预约')
    wx.showToast({ title: '已取消', icon: 'success' })
    const detail = await api.getOrder(this.data.detail.orderId)
    detail.statusMeta = getOrderStatusMeta(detail.orderStatus)
    this.setData({ detail })
  }
})
