const api = require('../../../utils/api')

function toValidId(value) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

function mapServiceList(rows) {
  return (rows || []).map(item => ({
    ...item,
    serviceCover: item.serviceCover || ''
  }))
}

require('../../../utils/page')({
  data: {
    categoryList: [],
    rows: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loadingMore: false,
    activeCategoryId: undefined,
    keyword: ''
  },
  onLoad(options) {
    this.setData({
      activeCategoryId: toValidId(options.categoryId),
      keyword: options.keyword || ''
    })
    this.loadBaseData()
  },
  onReachBottom() {
    const { rows, total, loadingMore } = this.data
    if (loadingMore || rows.length >= total) {
      return
    }
    this.loadList(this.data.pageNum + 1, true)
  },
  async loadBaseData() {
    try {
      const home = await api.getHome()
      this.setData({ categoryList: home.categoryList || [] })
      await this.loadList(1)
    } catch (error) {
      wx.navigateTo({ url: '/pages/state/load-failed/index?title=服务列表加载失败&target=/pages/service/list/index' })
    }
  },
  async loadList(pageNum, append = false) {
    this.setData({ loadingMore: true })
    try {
      const res = await api.listServices({
        pageNum,
        pageSize: this.data.pageSize,
        categoryId: this.data.activeCategoryId,
        keyword: this.data.keyword || undefined
      })
      const mappedRows = mapServiceList(res.rows || [])
      const nextRows = append ? this.data.rows.concat(mappedRows) : mappedRows
      this.setData({
        rows: nextRows,
        total: res.total || 0,
        pageNum
      })
    } finally {
      this.setData({ loadingMore: false })
    }
  },
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },
  handleSearch() {
    this.loadList(1)
  },
  switchCategory(e) {
    this.setData({ activeCategoryId: toValidId(e.currentTarget.dataset.id) })
    this.loadList(1)
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/service/detail/index?id=${e.currentTarget.dataset.id}` })
  }
})
