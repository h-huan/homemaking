function hasLogin() {
  return !!wx.getStorageSync('miniToken')
}

function requireLogin(redirectUrl) {
  if (hasLogin()) {
    return true
  }
  const url = redirectUrl
    ? `/pages/login/index?redirect=${encodeURIComponent(redirectUrl)}`
    : '/pages/login/index'
  wx.navigateTo({ url })
  return false
}

module.exports = {
  hasLogin,
  requireLogin
}
