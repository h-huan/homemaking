const api = require('../../utils/api')
const { hasLogin, requireLogin } = require('../../utils/auth')
const { resolveIcon } = require('../../utils/icon')

function buildDefaultMenus() {
  return [
    { key: 'order', title: '我的订单', desc: '查看预约进度与状态', path: '/pages/order/list/index', group: 'service', iconCode: 'order' },
    { key: 'schedule', title: '服务日程', desc: '按月查看上门安排', path: '/pages/schedule/index', group: 'service', iconCode: 'schedule' },
    { key: 'address', title: '地址管理', desc: '维护服务地址', path: '/pages/address/list/index', group: 'service', iconCode: 'address' },
    { key: 'contact', title: '联系客服', desc: '查看平台联系方式', action: 'contact', group: 'service', iconCode: 'contact' },
    { key: 'privacy', title: '隐私政策', desc: '查看平台隐私说明', contentType: 'privacy_policy', group: 'content', iconCode: 'privacy' },
    { key: 'serviceGuide', title: '服务说明', desc: '查看预约与履约说明', contentType: 'service_guide', group: 'content', iconCode: 'guide' },
    { key: 'about', title: '关于我们', desc: '了解平台介绍', contentType: 'about_us', group: 'content', iconCode: 'about' }
  ]
}

function normalizeMenuList(menuList) {
  return (menuList || []).map(item => ({
    ...item,
    iconUrl: resolveIcon(item.iconCode || item.iconUrl || item.icon)
  }))
}

function splitMenuList(menuList) {
  const serviceMenuList = []
  const contentMenuList = []
  ;(menuList || []).forEach(item => {
    if (item.group === 'content' || item.contentType) {
      contentMenuList.push(item)
      return
    }
    serviceMenuList.push(item)
  })
  return { serviceMenuList, contentMenuList }
}

Page({
  data: {
    loggedIn: false,
    profile: {},
    orderStats: [
      { key: 'pending', label: '待处理', count: 0, statusList: ['20', '30'] },
      { key: 'serving', label: '服务中', count: 0, statusList: ['40', '50'] },
      { key: 'done', label: '已完成', count: 0, statusList: ['70'] },
      { key: 'all', label: '全部订单', count: 0, statusList: [] }
    ],
    menuList: normalizeMenuList(buildDefaultMenus()),
    serviceMenuList: [],
    contentMenuList: [],
    contactConfig: {}
  },
  async onShow() {
    const loggedIn = hasLogin()
    this.setData({ loggedIn })
    const home = await api.getHome()
    const menuList = normalizeMenuList((home.mineMenuList || []).length ? home.mineMenuList : buildDefaultMenus())
    const menuGroup = splitMenuList(menuList)
    this.setData({
      menuList,
      serviceMenuList: menuGroup.serviceMenuList,
      contentMenuList: menuGroup.contentMenuList,
      contactConfig: home.contactConfig || {}
    })
    if (!loggedIn) {
      this.setData({
        profile: {},
        orderStats: this.data.orderStats.map(item => ({ ...item, count: 0 }))
      })
      return
    }
    await this.loadProfileAndOrders()
  },
  async loadProfileAndOrders() {
    const [profile, orderRes] = await Promise.all([
      api.getProfile(),
      api.listOrders({ pageNum: 1, pageSize: 100 })
    ])
    const rows = orderRes.rows || []
    const orderStats = this.data.orderStats.map(item => ({
      ...item,
      count: item.statusList.length ? rows.filter(order => item.statusList.indexOf(order.orderStatus) >= 0).length : rows.length
    }))
    profile.avatarText = profile.nickname ? profile.nickname.substring(0, 1) : '家'
    this.setData({ profile, orderStats })
  },
  goLogin() {
    requireLogin('/pages/mine/index')
  },
  openStat(e) {
    const status = e.currentTarget.dataset.status
    const url = status ? `/pages/order/list/index?status=${status}` : '/pages/order/list/index'
    if (!requireLogin(url)) {
      return
    }
    wx.navigateTo({ url })
  },
  getMenuItemByKey(key) {
    return (this.data.menuList || []).find(item => item.key === key)
  },
  handleMenuTap(e) {
    const item = this.getMenuItemByKey(e.currentTarget.dataset.key)
    if (!item) {
      return
    }
    if (item.contentType) {
      wx.navigateTo({
        url: `/pages/content/detail/index?contentType=${item.contentType}&title=${encodeURIComponent(item.title || '内容详情')}`
      })
      return
    }
    if (item.action === 'contact') {
      const phone = this.data.contactConfig.phone || ''
      wx.showModal({
        title: '联系客服',
        content: phone ? `联系电话：${phone}` : '后台暂未配置客服联系方式',
        showCancel: false
      })
      return
    }
    if (item.path) {
      if (item.path.indexOf('/pages/schedule/index') === 0 || item.path.indexOf('/pages/home/index') === 0 || item.path.indexOf('/pages/category/index') === 0 || item.path.indexOf('/pages/mine/index') === 0) {
        wx.switchTab({ url: item.path })
        return
      }
      if (!requireLogin(item.path)) {
        return
      }
      wx.navigateTo({ url: item.path })
    }
  }
})
