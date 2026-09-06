const api = require('../../../utils/api')

require('../../../utils/page')({
  data: { mode: 'review', stars: [1, 2, 3, 4, 5], serviceRating: 5, workerRating: 5, content: '', amount: '', maxAmount: 0, submitting: false,
    reviewTags: ['准时到达', '专业细致', '沟通顺畅', '服务规范', '物有所值', '值得推荐'].map(label => ({ label, active: false })), reviewImages: [],
    types: [{ value: 'REWORK', label: '补做' }, { value: 'REASSIGN_WORKER', label: '换服务人员' }, { value: 'REVISIT', label: '重新上门' }, { value: 'PARTIAL_REFUND', label: '部分退款' }, { value: 'FULL_REFUND', label: '全额退款' }, { value: 'OTHER_COMPENSATION', label: '其他补偿' }], type: 'REWORK' },
  onLoad(options) {
    this.orderId = Number(options.id)
    this.reviewRequestKey = `review-${this.orderId}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
    const maxAmount = Number(options.amount || 0)
    this.setData({ mode: options.mode || 'review', amount: maxAmount ? maxAmount.toFixed(2) : '', maxAmount })
  },
  rate(e) { this.setData({ [e.currentTarget.dataset.field]: Number(e.currentTarget.dataset.value) }) },
  toggleTag(e) { const index = Number(e.currentTarget.dataset.index); this.setData({ reviewTags: this.data.reviewTags.map((item, i) => i === index ? { ...item, active: !item.active } : item) }) },
  chooseReviewImages() { const remaining = 6 - this.data.reviewImages.length; if (remaining < 1) return; wx.chooseMedia({ count: remaining, mediaType: ['image'], sizeType: ['compressed'], success: result => this.setData({ reviewImages: [...this.data.reviewImages, ...result.tempFiles.map(item => item.tempFilePath)].slice(0, 6) }) }) },
  removeReviewImage(e) { const index = Number(e.currentTarget.dataset.index); this.setData({ reviewImages: this.data.reviewImages.filter((_, i) => i !== index) }) },
  input(e) { this.setData({ content: e.detail.value }) },
  amount(e) { this.setData({ amount: e.detail.value }) },
  chooseType(e) { this.setData({ type: e.currentTarget.dataset.value }) },
  async submit() {
    if (this.data.submitting) return
    if (!this.data.content.trim()) return wx.showToast({ title: '请填写具体说明', icon: 'none' })
    const amountCents = this.data.type === 'PARTIAL_REFUND' ? Math.round(Number(this.data.amount) * 100) : null
    if (this.data.mode === 'aftersale' && this.data.type === 'PARTIAL_REFUND' && (!Number.isFinite(amountCents) || amountCents < 1 || Number(this.data.amount) >= this.data.maxAmount)) {
      return wx.showToast({ title: `部分退款须低于 ${this.data.maxAmount.toFixed(2)} 元，全退请选择全额退款`, icon: 'none' })
    }
    this.setData({ submitting: true })
    try {
      if (this.data.mode === 'review') {
        const reviewId = await api.saveReviewDraft({ orderId: this.orderId, serviceRating: this.data.serviceRating, workerRating: this.data.workerRating, tags: this.data.reviewTags.filter(item => item.active).map(item => item.label), content: this.data.content })
        for (let i = 0; i < this.data.reviewImages.length; i++) await api.uploadReviewImage(reviewId, this.data.reviewImages[i], `${this.reviewRequestKey}-${i}`)
        await api.publishReview(reviewId)
      } else await api.submitAftersale({ orderId: this.orderId, type: this.data.type, amountCents, reason: this.data.content, requestKey: `mini-${Date.now()}-${Math.random().toString(36).slice(2, 10)}` })
      wx.showToast({ title: this.data.mode === 'review' ? '评价已提交' : '售后已申请', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 700)
    } catch (_) {
      wx.showToast({ title: '提交失败，请检查后重试', icon: 'none' })
    } finally { this.setData({ submitting: false }) }
  }
})
