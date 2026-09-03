App({
  loadBrand() {
    if (this.brand && Date.now() - this.brandLoadedAt < 300000) return Promise.resolve(this.brand)
    if (this.brandRequest) return this.brandRequest
    this.brandRequest = new Promise(resolve => wx.request({
      url: this.globalData.baseUrl + '/mini/home/index',
      header: { 'tenant-id': String(this.globalData.tenantId) },
      success: response => {
        if (response.data && response.data.code === 0) {
          this.brand = response.data.data
          this.brandLoadedAt = Date.now()
        }
        resolve(this.brand || {})
      },
      fail: () => resolve(this.brand || {})
    })).finally(() => { this.brandRequest = null })
    return this.brandRequest
  },
  globalData: {
    baseUrl: 'http://127.0.0.1:48080/app-api',
    tenantId: 1
  }
})
