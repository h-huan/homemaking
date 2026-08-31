const api = require('../../../utils/api')
const { getOrderStatusMeta } = require('../../../utils/order')
const { requireLogin } = require('../../../utils/auth')

Page({
  data: {
    tabs: [
      { label: '全部', value: '' },
      { label: '待确认', value: '20' },
      { label: '待派单', value: '30' },
      { label: '待上门', value: '40' },
      { label: '已完成', value: '70' },
      { label: '已取消', value: '80' }
    ],
    activeStatus: '',
    rows: []
  },
  onLoad(options) {
    if (options.status !== undefined) {
      this.setData({ activeStatus: options.status })
    }
  },
  onShow() {
    if (!requireLogin('/pages/order/list/index')) {
      return
    }
    this.loadData()
  },
  async loadData() {
    const res = await api.listOrders({ pageNum: 1, pageSize: 50, orderStatus: this.data.activeStatus || undefined })
    const rows = (res.rows || []).map(item => ({
      ...item,
      statusMeta: getOrderStatusMeta(item.orderStatus)
    }))
    this.setData({ rows })
  },
  switchTab(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.value })
    this.loadData()
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/order/detail/index?id=${e.currentTarget.dataset.id}` })
  },
  async handleCancel(e) {
    const orderId = Number(e.currentTarget.dataset.id)
    wx.showModal({
      title: '取消预约',
      content: '确认取消这笔预约吗？',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        await api.cancelOrder(orderId, '用户取消预约')
        wx.showToast({ title: '已取消', icon: 'success' })
        this.loadData()
      }
    })
  }
})
