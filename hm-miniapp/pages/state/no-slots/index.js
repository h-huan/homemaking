Page({
  goBack() {
    wx.navigateBack()
  },
  goHome() {
    wx.switchTab({ url: '/pages/home/index' })
  }
})
