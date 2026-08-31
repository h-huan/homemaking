const api = require('../../../utils/api')
const { requireLogin } = require('../../../utils/auth')

Page({
  data: { list: [], selectMode: false, selectedAddressId: null },
  onLoad(options) {
    this.setData({
      selectMode: options.select === '1'
    })
  },
  onShow() {
    if (!requireLogin('/pages/address/list/index')) {
      return
    }
    this.loadData()
  },
  async loadData() {
    try {
      const list = await api.listAddresses()
      this.setData({ list })
    } catch (error) {
      wx.showToast({ title: '地址加载失败', icon: 'none' })
    }
  },
  goEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: id ? `/pages/address/edit/index?id=${id}` : '/pages/address/edit/index' })
  },
  handleCardTap(e) {
    if (this.data.selectMode) {
      this.chooseAddress(e)
      return
    }
    this.goEdit(e)
  },
  chooseAddress(e) {
    if (!this.data.selectMode) {
      return
    }
    const id = Number(e.currentTarget.dataset.id)
    wx.setStorageSync('selectedAddressId', id)
    wx.navigateBack()
  },
  async handleDelete(e) {
    const id = Number(e.currentTarget.dataset.id)
    wx.showModal({
      title: '删除地址',
      content: '确认删除这个服务地址吗？',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await api.deleteAddress(id)
          wx.showToast({ title: '删除成功', icon: 'success' })
          this.loadData()
        } catch (error) {
          wx.showToast({ title: (error && error.msg) || '删除失败', icon: 'none' })
        }
      }
    })
  }
})
