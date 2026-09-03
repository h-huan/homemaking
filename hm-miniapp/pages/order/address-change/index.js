const api = require('../../../utils/api')
require('../../../utils/page')({
  data: { order: {}, addresses: [], labels: [], selected: -1, reason: '', preview: null, checking: false, submitting: false },
  async onLoad(options) {
    this.orderId = Number(options.id)
    this.requestKey = 'address_' + Date.now() + '_' + Math.random().toString(36).slice(2)
    const [order, addresses] = await Promise.all([api.getOrder(this.orderId), api.listAddresses()])
    this.setData({ order, addresses, labels: ['请选择已保存的地址', ...addresses.map(a => a.contactName + ' · ' + a.detailAddress)] })
  },
  invalidate() { this.quoteVersion = (this.quoteVersion || 0) + 1; this.checkedRequest = null; this.setData({ preview: null }) },
  selectAddress(e) { this.invalidate(); this.setData({ selected: Number(e.detail.value) - 1 }) },
  inputReason(e) { this.invalidate(); this.setData({ reason: e.detail.value }) },
  async checkQuote() {
    if (this.data.checking || this.data.submitting) return
    if (this.data.selected < 0 || !this.data.reason.trim()) return wx.showToast({ title: '请选择地址并填写原因', icon: 'none' })
    const version = this.quoteVersion = (this.quoteVersion || 0) + 1
    const body = { version: this.data.order.version, addressId: this.data.addresses[this.data.selected].addressId, reason: this.data.reason.trim(), requestKey: this.requestKey }
    this.setData({ checking: true })
    try {
      const preview = await api.previewAddressChange(this.orderId, body)
      if (version !== this.quoteVersion) return
      preview.oldAmount = (preview.before.price_cents / 100).toFixed(2)
      preview.newAmount = (preview.after.price_cents / 100).toFixed(2)
      preview.differenceAmount = (Math.abs(preview.differenceCents) / 100).toFixed(2)
      this.checkedRequest = { ...body, expectedPriceCents: preview.after.price_cents }; this.setData({ preview })
    } finally { this.setData({ checking: false }) }
  },
  async submit() {
    if (!this.checkedRequest || !this.data.preview || this.data.submitting || this.data.checking) return
    this.setData({ submitting: true })
    try {
      await api.changeOrderAddress(this.orderId, this.checkedRequest)
      wx.showToast({ title: this.data.preview.requiresSettlement ? '待门店处理差额' : '地址已变更', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 700)
    } finally { this.setData({ submitting: false }) }
  }
})
