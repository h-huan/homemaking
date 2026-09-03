const api = require('../../../utils/api')

require('../../../utils/page')({
  data: {
    form: {
      addressId: null,
      contactName: '',
      contactMobile: '',
      provinceName: '',
      cityName: '',
      districtName: '',
      detailAddress: '',
      isDefault: '0'
    }
  },
  async onLoad(options) {
    if (options.id) {
      const form = await api.getAddress(options.id)
      this.setData({ form })
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onSwitchChange(e) {
    this.setData({ 'form.isDefault': e.detail.value ? '1' : '0' })
  },
  async submit() {
    const form = this.data.form
    if (!form.contactName || !form.contactMobile || !form.provinceName || !form.cityName || !form.districtName || !form.detailAddress) {
      wx.showToast({ title: '请完整填写地址信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.contactMobile)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    await api.saveAddress(form)
    wx.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => wx.navigateBack(), 300)
  }
})
