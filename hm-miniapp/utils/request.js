const app = getApp()

function buildQuery(params) {
  const searchParams = Object.keys(params || {})
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
  return searchParams.length ? `?${searchParams.join('&')}` : ''
}

function request(options) {
  const token = wx.getStorageSync('miniToken')
  const method = options.method || 'GET'
  const params = options.params || {}
  const data = options.data || {}
  const requestUrl = `${app.globalData.baseUrl}${options.url}${method === 'GET' ? buildQuery({ ...params, ...data }) : buildQuery(params)}`
  return new Promise((resolve, reject) => {
    wx.request({
      url: requestUrl,
      method,
      data: method === 'GET' ? {} : data,
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        const data = res.data || {}
        if (data.code === 200) {
          resolve(data.data)
          return
        }
        if (data.code === 401) {
          wx.removeStorageSync('miniToken')
          wx.showToast({ title: data.msg || '登录已失效', icon: 'none' })
          setTimeout(() => {
            wx.reLaunch({ url: '/pages/login/index' })
          }, 300)
          reject(data)
          return
        }
        wx.showToast({ title: data.msg || '请求失败', icon: 'none' })
        reject(data)
      },
      fail(err) {
        wx.showToast({ title: '网络开小差了', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request }
