const api = require('../../utils/api')

Page({
  data: {
    loading: false,
    agreementChecked: true,
    redirect: ''
  },
  onLoad(options) {
    this.setData({
      redirect: options.redirect ? decodeURIComponent(options.redirect) : ''
    })
  },
  toggleAgreement() {
    this.setData({ agreementChecked: !this.data.agreementChecked })
  },
  async doLogin() {
    if (!this.data.agreementChecked) {
      wx.showToast({ title: '请先阅读并勾选服务协议', icon: 'none' })
      return null
    }
    const loginRes = await wx.login()
    if (!loginRes.code) {
      throw new Error('wx.login failed')
    }
    const res = await api.login(loginRes.code)
    wx.setStorageSync('miniToken', res.token)
    return res
  },
  jumpAfterLogin() {
    const redirect = this.data.redirect
    if (!redirect) {
      wx.switchTab({ url: '/pages/home/index' })
      return
    }
    if (
      redirect.indexOf('/pages/home/index') === 0 ||
      redirect.indexOf('/pages/category/index') === 0 ||
      redirect.indexOf('/pages/schedule/index') === 0 ||
      redirect.indexOf('/pages/mine/index') === 0
    ) {
      wx.switchTab({ url: redirect.split('?')[0] })
      return
    }
    wx.redirectTo({ url: redirect })
  },
  async handleQuickLogin(e) {
    this.setData({ loading: true })
    try {
      const phoneCode = e.detail && e.detail.code
      const loginRes = await this.doLogin()
      if (!loginRes) {
        return
      }
      if (phoneCode) {
        await api.bindMobile({ phoneCode })
      }
      this.jumpAfterLogin()
    } catch (error) {
      wx.showToast({
        title: (error && error.msg) || '登录失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },
  async handleWechatLogin() {
    this.setData({ loading: true })
    try {
      const loginRes = await this.doLogin()
      if (!loginRes) {
        return
      }
      this.jumpAfterLogin()
    } catch (error) {
      wx.showToast({
        title: (error && error.msg) || '登录失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  }
})
