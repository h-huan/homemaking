const api = require('../../utils/api')
const { resolveIcon } = require('../../utils/icon')

function toValidId(value) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

function pickImage(...values) {
  return values.find(item => item) || ''
}

function mapNavList(home, categoryMap) {
  const serviceNavList = home.serviceNavList || []
  if (serviceNavList.length) {
    return serviceNavList.map(item => {
      const id = toValidId(item.id || item.categoryId)
      const category = id ? categoryMap[id] : null
      return {
        ...item,
        id,
        path: item.path || item.navPath || '',
        title: item.title || item.name || item.categoryName || (category && category.categoryName) || '',
        iconUrl: resolveIcon(item.iconCode || item.iconUrl || item.icon || (category && category.categoryIcon))
      }
    })
  }
  return (home.categoryList || []).slice(0, 12).map(item => ({
    id: item.categoryId,
    title: item.categoryName,
    path: '',
    iconUrl: resolveIcon(item.categoryIcon)
  }))
}

function mapPromoCards(home, categoryMap) {
  const promoCardList = home.categoryBannerList || home.promoCardList || []
  if (promoCardList.length) {
    return promoCardList.map(item => {
      const categoryId = toValidId(item.categoryId || item.id)
      const category = categoryId ? categoryMap[categoryId] : null
      return {
        ...item,
        title: item.title || '精选服务',
        subTitle: item.subTitle || '更快预约，更稳履约',
        imageUrl: pickImage(item.imageUrl, item.coverImage, category && category.bannerImage)
      }
    })
  }
  return []
}

function buildSections(home, serviceList) {
  const sectionList = home.categorySectionList || []
  if (sectionList.length) {
    return sectionList.map((item, index) => ({
      title: item.title || `推荐分组${index + 1}`,
      list: serviceList.slice(item.start || index * 4, (item.start || index * 4) + (item.limit || 4))
    })).filter(item => item.list.length)
  }
  return [
    { title: '新人优惠', list: serviceList.slice(0, 4) },
    { title: '热门TOP', list: serviceList.slice(4, 10) },
    { title: '新品上线', list: serviceList.slice(10, 14) }
  ].filter(item => item.list.length)
}

function mapServiceList(list) {
  return (list || []).map(item => ({
    ...item,
    serviceCover: item.serviceCover || ''
  }))
}

require('../../utils/page')({
  data: {
    home: {},
    categoryList: [],
    activeCategoryId: null,
    serviceList: [],
    navList: [],
    promoCardList: [],
    sectionList: []
  },
  async onLoad() {
    await this.loadBaseData()
  },
  async loadBaseData() {
    try {
      const home = await api.getHome()
      const categoryList = home.categoryList || []
      const categoryMap = categoryList.reduce((acc, item) => {
        acc[item.categoryId] = item
        return acc
      }, {})
      const activeCategory = categoryList[0] || null
      this.setData({
        home,
        categoryList,
        navList: mapNavList(home, categoryMap),
        promoCardList: mapPromoCards(home, categoryMap),
        activeCategoryId: activeCategory ? activeCategory.categoryId : null
      })
      if (activeCategory) {
        await this.loadCategoryServices(activeCategory.categoryId)
      }
    } catch (error) {
      wx.navigateTo({ url: '/pages/state/load-failed/index?title=分类页加载失败&target=/pages/category/index' })
    }
  },
  async loadCategoryServices(categoryId) {
    const validCategoryId = toValidId(categoryId)
    const res = await api.listServices({ categoryId: validCategoryId, pageNum: 1, pageSize: 30 })
    const serviceList = mapServiceList(res.rows || [])
    this.setData({
      serviceList,
      sectionList: buildSections(this.data.home, serviceList)
    })
  },
  async switchCategory(e) {
    const categoryId = toValidId(e.currentTarget.dataset.id)
    this.setData({ activeCategoryId: categoryId })
    await this.loadCategoryServices(categoryId)
  },
  openNav(e) {
    const categoryId = toValidId(e.currentTarget.dataset.id)
    const path = e.currentTarget.dataset.path
    if (path) {
      wx.navigateTo({ url: path })
      return
    }
    if (categoryId) {
      this.setData({ activeCategoryId: categoryId })
      this.loadCategoryServices(categoryId)
    }
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/service/detail/index?id=${id}` })
  },
  goList() {
    const categoryId = this.data.activeCategoryId
    const url = categoryId ? `/pages/service/list/index?categoryId=${categoryId}` : '/pages/service/list/index'
    wx.navigateTo({ url })
  }
})
