const api = require('../../../utils/api')

require('../../../utils/page')({
  data: { mode: 'review', stars: [1, 2, 3, 4, 5], rating: 5, content: '', amount: '', maxAmount: 0, submitting: false },
  onLoad(options) {
    this.orderId = Number(options.id)
    const maxAmount = Number(options.amount || 0)
    this.setData({ mode: options.mode || 'review', amount: maxAmount ? maxAmount.toFixed(2) : '', maxAmount })
  },
  rate(e) { this.setData({ rating: Number(e.currentTarget.dataset.value) }) },
  input(e) { this.setData({ content: e.detail.value }) },
  amount(e) { this.setData({ amount: e.detail.value }) },
  async submit() {
    if (this.data.submitting) return
    if (!this.data.content.trim()) return wx.showToast({ title: '请填写具体说明', icon: 'none' })
    const amountCents = Math.round(Number(this.data.amount) * 100)
    if (this.data.mode === 'aftersale' && (!Number.isFinite(amountCents) || amountCents < 1 || Number(this.data.amount) > this.data.maxAmount)) {
      return wx.showToast({ title: `退款金额应在 0.01 至 ${this.data.maxAmount.toFixed(2)} 元之间`, icon: 'none' })
    }
    this.setData({ submitting: true })
    try {
      if (this.data.mode === 'review') await api.submitReview({ orderId: this.orderId, rating: this.data.rating, content: this.data.content })
      else await api.submitAftersale({ orderId: this.orderId, amountCents, reason: this.data.content })
      wx.showToast({ title: this.data.mode === 'review' ? '评价已提交' : '售后已申请', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 700)
    } catch (_) {
      wx.showToast({ title: '提交失败，请检查后重试', icon: 'none' })
    } finally { this.setData({ submitting: false }) }
  }
})
