const api = require('../../../utils/api')
const { requireLogin } = require('../../../utils/auth')

function parseSlotText(detail) {
  if (!detail.bookingRule || !detail.bookingRule.timeSlotsJson) {
    return '以预约页选择为准'
  }
  try {
    const slots = JSON.parse(detail.bookingRule.timeSlotsJson)
    return Array.isArray(slots) && slots.length ? slots.join(' / ') : '以预约页选择为准'
  } catch (error) {
    return '以预约页选择为准'
  }
}

require('../../../utils/page')({
  data: { detail: {}, selectedSkuId: null },
  onLoad(options) {
    this.serviceItemId = options.id
    this.loadData()
  },
  async loadData() {
    try {
      const detail = await api.getServiceDetail(this.serviceItemId)
      const skuList = detail.skuList || []
      detail.slotText = parseSlotText(detail)
      detail.shortName = detail.serviceInfo && detail.serviceInfo.serviceName
        ? detail.serviceInfo.serviceName.substring(0, 2)
        : '服'
      this.setData({
        detail,
        selectedSkuId: skuList[0] ? skuList[0].skuId : null
      })
    } catch (error) {
      wx.navigateTo({ url: `/pages/state/load-failed/index?title=服务详情加载失败&target=/pages/service/detail/index?id=${this.serviceItemId}` })
    }
  },
  selectSku(e) {
    this.setData({ selectedSkuId: Number(e.currentTarget.dataset.id) })
  },
  bookNow() {
    if (!requireLogin(`/pages/service/detail/index?id=${this.serviceItemId}`)) {
      return
    }
    const skuQuery = this.data.selectedSkuId ? `&skuId=${this.data.selectedSkuId}` : ''
    wx.navigateTo({ url: `/pages/order/booking/index?serviceItemId=${this.serviceItemId}${skuQuery}` })
  }
})
