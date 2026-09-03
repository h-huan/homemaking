const api = require('../../../utils/api')

function buildDateOptions(rule) {
  const maxAdvanceDays = rule && rule.maxAdvanceDays ? Number(rule.maxAdvanceDays) : 7
  const allowSameDay = !rule || rule.allowSameDay !== '0'
  const startOffset = allowSameDay ? 0 : 1
  const result = []
  for (let i = startOffset; i <= maxAdvanceDays; i += 1) {
    const date = new Date()
    date.setDate(date.getDate() + i)
    const month = `${date.getMonth() + 1}`.padStart(2, '0')
    const day = `${date.getDate()}`.padStart(2, '0')
    result.push({
      value: `${date.getFullYear()}-${month}-${day}`,
      label: i === 0 ? '今天' : `${month}/${day}`
    })
  }
  return result
}

require('../../../utils/page')({
  data: {
    detail: {},
    addresses: [],
    selectedAddress: null,
    dateOptions: [],
    slotOptions: [],
    selectedSkuId: null,
    selectedExtraIds: [],
    selectedExtraMap: {},
    capacityLoading: false,
    priceLoading: false,
    form: {
      serviceItemId: null,
      skuId: null,
      addressId: null,
      appointmentDate: '',
      appointmentTimeSlot: '',
      customerRemark: '',
      extraItemList: []
    },
    calc: null
  },
  async onLoad(options) {
    this.setData({ 'form.requestKey': `hm-${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}` })
    this.serviceItemId = Number(options.serviceItemId)
    this.initSkuId = options.skuId ? Number(options.skuId) : null
    await this.loadData()
  },
  async onShow() {
    const selectedAddressId = wx.getStorageSync('selectedAddressId')
    if (selectedAddressId) {
      wx.removeStorageSync('selectedAddressId')
      await this.reloadAddresses(selectedAddressId)
    }
  },
  async loadData() {
    try {
      const [detail, addresses] = await Promise.all([
        api.getServiceDetail(this.serviceItemId),
        api.listAddresses()
      ])
      const dateOptions = buildDateOptions(detail.bookingRule)
      const defaultAddress = addresses.find(item => item.isDefault === '1') || addresses[0] || null
      const skuList = detail.skuList || []
      const selectedSku = skuList.find(item => item.skuId === this.initSkuId) || skuList[0] || null
      this.setData({
        detail,
        addresses,
        selectedAddress: defaultAddress,
        dateOptions,
        slotOptions: [],
        selectedSkuId: selectedSku ? selectedSku.skuId : null,
        'form.serviceItemId': this.serviceItemId,
        'form.skuId': selectedSku ? selectedSku.skuId : null,
        'form.addressId': defaultAddress ? defaultAddress.addressId : null,
        'form.appointmentDate': dateOptions[0] ? dateOptions[0].value : '',
        'form.appointmentTimeSlot': ''
      })
      if (defaultAddress) {
        await Promise.all([this.loadCapacity(), this.calcPrice()])
      }
    } catch (error) {
      const skuQuery = this.initSkuId ? `&skuId=${this.initSkuId}` : ''
      wx.navigateTo({ url: `/pages/state/load-failed/index?title=预约页加载失败&target=/pages/order/booking/index?serviceItemId=${this.serviceItemId}${skuQuery}` })
    }
  },
  async reloadAddresses(selectedAddressId) {
    const addresses = await api.listAddresses()
    const selectedAddress = addresses.find(item => item.addressId === selectedAddressId) || addresses[0] || null
    this.setData({
      addresses,
      selectedAddress,
      'form.addressId': selectedAddress ? selectedAddress.addressId : null
    })
    if (selectedAddress) {
      await Promise.all([this.loadCapacity(), this.calcPrice()])
    }
  },
  async loadCapacity() {
    const version = this.capacityVersion = (this.capacityVersion || 0) + 1
    const form = { ...this.data.form }
    if (!form.addressId || !form.appointmentDate) return
    this.setData({ capacityLoading: true, slotOptions: [], 'form.appointmentTimeSlot': '' })
    try {
      const rows = await api.getCapacity({ serviceItemId: form.serviceItemId, skuId: form.skuId || undefined, appointmentDate: form.appointmentDate, addressId: form.addressId })
      if (version !== this.capacityVersion) return
      const slotOptions = rows.filter(item => item.available).map(item => item.label)
      this.setData({ slotOptions, 'form.appointmentTimeSlot': slotOptions.includes(form.appointmentTimeSlot) ? form.appointmentTimeSlot : (slotOptions[0] || '') })
    } finally { if (version === this.capacityVersion) this.setData({ capacityLoading: false }) }
  },
  buildExtraItemList() {
    return this.data.selectedExtraIds.map(extraItemId => ({ extraItemId, quantity: 1 }))
  },
  async calcPrice() {
    if (!this.data.form.addressId) {
      return
    }
    const version = this.priceVersion = (this.priceVersion || 0) + 1
    const extraItemList = this.buildExtraItemList()
    this.setData({ priceLoading: true, calc: null })
    try {
      const calc = await api.calcOrder({ ...this.data.form, extraItemList })
      if (version === this.priceVersion) this.setData({ calc, 'form.extraItemList': extraItemList })
    } finally { if (version === this.priceVersion) this.setData({ priceLoading: false }) }
  },
  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/list/index?select=1' })
  },
  async selectDate(e) {
    this.setData({ 'form.appointmentDate': e.currentTarget.dataset.value })
    await this.loadCapacity()
  },
  selectSlot(e) {
    this.setData({ 'form.appointmentTimeSlot': e.currentTarget.dataset.value })
    this.calcPrice()
  },
  async selectSku(e) {
    const skuId = Number(e.currentTarget.dataset.id)
    this.setData({
      selectedSkuId: skuId,
      'form.skuId': skuId
    })
    await Promise.all([this.loadCapacity(), this.calcPrice()])
  },
  toggleExtra(e) {
    const extraId = Number(e.currentTarget.dataset.id)
    const selectedExtraIds = [...this.data.selectedExtraIds]
    const index = selectedExtraIds.indexOf(extraId)
    if (index >= 0) {
      selectedExtraIds.splice(index, 1)
    } else {
      selectedExtraIds.push(extraId)
    }
    this.setData({ selectedExtraIds, selectedExtraMap: Object.fromEntries(selectedExtraIds.map(id => [id, true])) })
    this.calcPrice()
  },
  onRemark(e) {
    this.setData({ 'form.customerRemark': e.detail.value })
  },
  goConfirm() {
    if (this.data.capacityLoading || this.data.priceLoading) return
    if (!this.data.form.addressId) {
      wx.showToast({ title: '请先选择服务地址', icon: 'none' })
      return
    }
    if (!this.data.form.appointmentTimeSlot) {
      wx.showToast({ title: '当天暂无可约时间，请选择其他日期', icon: 'none' })
      return
    }
    const pendingBooking = {
      detail: this.data.detail,
      selectedAddress: this.data.selectedAddress,
      selectedSkuId: this.data.selectedSkuId,
      selectedExtraIds: this.data.selectedExtraIds,
      form: {
        ...this.data.form,
        extraItemList: this.buildExtraItemList()
      },
      calc: this.data.calc
    }
    wx.setStorageSync('pendingBooking', pendingBooking)
    wx.navigateTo({ url: '/pages/order/confirm/index' })
  }
})
