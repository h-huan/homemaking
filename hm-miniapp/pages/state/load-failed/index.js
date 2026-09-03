require('../../../utils/page')({
  data: {
    title: '页面加载失败',
    target: ''
  },
  onLoad(options) {
    this.setData({
      title: options.title || '页面加载失败',
      target: options.target || ''
    })
  },
  retry() {
    if (!this.data.target) {
      wx.navigateBack()
      return
    }
    wx.reLaunch({ url: this.data.target })
  },
  goHome() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
