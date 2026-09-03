const api = require('../../utils/api')
const { hasLogin, requireLogin } = require('../../utils/auth')
const { resolveIcon } = require('../../utils/icon')

function toValidId(value) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

function pickImage(...values) {
  return values.find(item => item) || ''
}

function buildSearchConfig(home) {
  const config = home.searchConfig || {}
  return {
    cityName: config.cityName || '深圳市',
    placeholder: config.placeholder || '搜索服务、项目关键词',
    keyword: config.keyword || ''
  }
}

function buildSectionConfig(home) {
  const config = home.sectionConfig || {}
  return {
    familyTitle: config.familyTitle || '家务全家桶',
    familyMoreText: config.familyMoreText || '查看更多',
    recommendTitle: config.recommendTitle || '热门推荐',
    recommendMoreText: config.recommendMoreText || '全部分类',
    advantageTitle: config.advantageTitle || '平台保障',
    quickTitle: config.quickTitle || '快捷入口'
  }
}

function normalizeNavItem(item, categoryMap) {
  const categoryId = toValidId(item.id || item.categoryId)
  const category = categoryId ? categoryMap[categoryId] : null
  const title = item.title || item.name || item.categoryName || (category && category.categoryName) || ''
  return {
    ...item,
    id: categoryId,
    path: item.path || item.navPath || '',
    title,
    iconUrl: resolveIcon(item.iconCode || item.iconUrl || item.icon || item.navIcon || (category && category.categoryIcon)),
    badgeText: item.badgeText || ''
  }
}

function buildNavList(home, categoryMap) {
  if ((home.serviceNavList || []).length) {
    return home.serviceNavList.map(item => normalizeNavItem(item, categoryMap))
  }
  return (home.categoryList || []).slice(0, 10).map(item => normalizeNavItem({
    id: item.categoryId,
    title: item.categoryName,
    iconCode: item.categoryIcon
  }, categoryMap))
}

function buildPriceTabs(home) {
  const hotList = home.hotServiceList || []
  return hotList.slice(0, 4).map(item => ({
    title: item.serviceName,
    price: item.basePrice,
    serviceItemId: item.serviceItemId
  }))
}

function buildPromoList(home, serviceMap) {
  if ((home.promoCardList || []).length) {
    return home.promoCardList.map(item => {
      const service = serviceMap[item.serviceItemId]
      return {
        ...item,
        imageUrl: pickImage(item.imageUrl, item.coverImage, service && service.serviceCover)
      }
    })
  }
  return (home.hotServiceList || []).slice(0, 4).map(item => ({
    title: item.serviceName,
    subTitle: item.serviceSubTitle || '优选上门家政服务',
    serviceItemId: item.serviceItemId,
    imageUrl: item.serviceCover || ''
  }))
}

function mapServiceList(list) {
  return (list || []).map(item => ({
    ...item,
    serviceCover: item.serviceCover || ''
  }))
}

function mapAdvantageList(list) {
  return (list || []).map(item => {
    if (typeof item === 'string') {
      return { text: item, iconUrl: '' }
    }
    return {
      text: item.text || '',
      iconUrl: item.iconUrl || ''
    }
  })
}

require('../../utils/page')({
  data: {
    home: { bannerList: [], categoryList: [], hotServiceList: [], advantageList: [], recommendWorkerList: [] },
    profile: {},
    modules: {},
    bindPopupVisible: false,
    serviceNavList: [],
    priceTabList: [],
    promoCardList: [],
    searchConfig: buildSearchConfig({}),
    sectionConfig: buildSectionConfig({})
  },
  onShow() {
    this.loadData()
  },
  async loadData() {
    try {
      const home = await api.getHome()
      const modules = (home.homeModules || ['services', 'stores', 'workers']).reduce((enabled, key) => { enabled[key] = true; return enabled }, {})
      if (home.brandName) wx.setNavigationBarTitle({ title: home.brandName })
      const categoryMap = (home.categoryList || []).reduce((acc, item) => {
        acc[item.categoryId] = item
        return acc
      }, {})
      const hotServiceList = mapServiceList(home.hotServiceList)
      const serviceMap = hotServiceList.reduce((acc, item) => {
        acc[item.serviceItemId] = item
        return acc
      }, {})
      const nextData = {
        modules,
        home: {
          ...home,
          hotServiceList,
          advantageList: mapAdvantageList(home.advantageList)
        },
        serviceNavList: buildNavList(home, categoryMap),
        priceTabList: buildPriceTabs(home),
        promoCardList: buildPromoList(home, serviceMap),
        searchConfig: buildSearchConfig(home),
        sectionConfig: buildSectionConfig(home)
      }
      if (!hasLogin()) {
        this.setData({
          ...nextData,
          profile: {},
          bindPopupVisible: false
        })
        return
      }
      const profile = await api.getProfile()
      this.setData({
        ...nextData,
        profile,
        bindPopupVisible: !profile.isBindMobile
      })
    } catch (error) {
      wx.navigateTo({ url: '/pages/state/load-failed/index?title=首页加载失败&target=/pages/home/index' })
    }
  },
  goServiceList(e) {
    const categoryId = e.currentTarget.dataset.id
    const path = e.currentTarget.dataset.path
    if (path) {
      wx.navigateTo({ url: path })
      return
    }
    const url = categoryId ? `/pages/service/list/index?categoryId=${categoryId}` : '/pages/service/list/index'
    wx.navigateTo({ url })
  },
  openSearch() {
    const keyword = this.data.searchConfig.keyword || ''
    const url = keyword ? `/pages/service/list/index?keyword=${encodeURIComponent(keyword)}` : '/pages/service/list/index'
    wx.navigateTo({ url })
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/service/detail/index?id=${id}` })
  },
  goCategoryTab() {
    wx.switchTab({ url: '/pages/category/index' })
  },
  openOrderTab() {
    if (!requireLogin('/pages/order/list/index')) {
      return
    }
    wx.navigateTo({ url: '/pages/order/list/index' })
  },
  openAddressTab() {
    if (!requireLogin('/pages/address/list/index')) {
      return
    }
    wx.navigateTo({ url: '/pages/address/list/index' })
  },
  openMineTab() {
    wx.switchTab({ url: '/pages/mine/index' })
  },
  async handleBindPhone(e) {
    const phoneCode = e.detail && e.detail.code
    if (!phoneCode) {
      wx.showToast({ title: '请先授权微信手机号', icon: 'none' })
      return
    }
    try {
      await api.bindMobile({ phoneCode })
      const profile = await api.getProfile()
      this.setData({ profile, bindPopupVisible: false })
      wx.showToast({ title: '手机号绑定成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: (error && error.msg) || '绑定失败', icon: 'none' })
    }
  },
  closeBindPopup() {
    this.setData({ bindPopupVisible: false })
  },
  callStore(e) {
    const phone = e.currentTarget.dataset.phone
    if (phone) wx.makePhoneCall({ phoneNumber: phone })
  }
})
