// Shared page theme. Tenant colors are validated before being used in styles.
module.exports = function themedPage(options) {
  const onShow = options.onShow
  return Page({
    ...options,
    data: { ...options.data, themeStyle: '--hm-primary:#174f43;' },
    onShow(...args) {
      getApp().loadBrand().then(brand => {
        const color = /^#[0-9a-f]{6}$/i.test(brand.primaryColor || '') ? brand.primaryColor : '#174f43'
        this.setData({ themeStyle: '--hm-primary:' + color + ';' })
        wx.setTabBarStyle({ selectedColor: color, fail: () => {} })
      })
      if (onShow) return onShow.apply(this, args)
    }
  })
}
