const api = require('../../../utils/api')
function dates() {
  const result = []
  for (let i = 0; i <= 30; i += 1) {
    const d = new Date(); d.setDate(d.getDate() + i)
    const m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0')
    result.push({ value: d.getFullYear() + '-' + m + '-' + day, label: i === 0 ? '今天' : i === 1 ? '明天' : m + '/' + day })
  }
  return result
}
require('../../../utils/page')({
  data: { dates: dates(), date: '', slots: [], slot: '', reason: '客户调整时间', loading: false, submitting: false },
  async onLoad(options) { this.orderId = Number(options.id); this.setData({ date: this.data.dates[0].value }); await this.loadCapacity() },
  async loadCapacity() {
    const version = this.capacityVersion = (this.capacityVersion || 0) + 1
    this.setData({ loading: true, slots: [], slot: '' })
    try {
      const rows = await api.getOrderCapacity(this.orderId, this.data.date)
      if (version !== this.capacityVersion) return
      const slots = rows.filter(item => item.available).map(item => item.label)
      this.setData({ slots, slot: slots[0] || '' })
    } finally { if (version === this.capacityVersion) this.setData({ loading: false }) }
  },
  async chooseDate(e) { this.setData({ date: e.currentTarget.dataset.value }); await this.loadCapacity() },
  chooseSlot(e) { this.setData({ slot: e.currentTarget.dataset.value }) },
  inputReason(e) { this.setData({ reason: e.detail.value }) },
  async submit() {
    if (this.data.loading || this.data.submitting) return
    if (!this.data.slot || !this.data.reason.trim()) return wx.showToast({ title: '请选择时间并填写改期原因', icon: 'none' })
    this.setData({ submitting: true })
    try {
      await api.rescheduleOrder(this.orderId, { appointmentDate: this.data.date, appointmentTimeSlot: this.data.slot, reason: this.data.reason })
      wx.showToast({ title: '改期成功', icon: 'success' }); setTimeout(() => wx.navigateBack(), 700)
    } finally { this.setData({ submitting: false }) }
  }
})
