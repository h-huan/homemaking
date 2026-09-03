const api = require('../../../utils/api')

require('../../../utils/page')({
  data: {
    loading: true,
    title: '',
    contentType: '',
    content: null
  },
  onLoad(options) {
    const contentType = options.contentType || ''
    const title = options.title ? decodeURIComponent(options.title) : ''
    this.setData({ contentType, title })
    if (title) {
      wx.setNavigationBarTitle({ title })
    }
    this.loadContent()
  },
  async loadContent() {
    if (!this.data.contentType) {
      this.setData({ loading: false })
      wx.navigateTo({
        url: '/pages/state/load-failed/index?title=内容加载失败&target=/pages/mine/index'
      })
      return
    }
    try {
      const content = await api.getContent(this.data.contentType)
      const pageTitle = content.title || this.data.title || '内容详情'
      wx.setNavigationBarTitle({ title: pageTitle })
      this.setData({
        loading: false,
        title: pageTitle,
        content
      })
    } catch (error) {
      this.setData({ loading: false })
      wx.navigateTo({
        url: '/pages/state/load-failed/index?title=内容加载失败&target=/pages/mine/index'
      })
    }
  }
})
