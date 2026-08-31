const api = require('../../utils/api')
const { hasLogin, requireLogin } = require('../../utils/auth')

const WEEK_LIST = ['日', '一', '二', '三', '四', '五', '六']
const STATUS_MAP = {
  '20': { text: '待确认', tone: 'orange' },
  '30': { text: '待上门', tone: 'orange' },
  '40': { text: '服务中', tone: 'blue' },
  '50': { text: '服务中', tone: 'blue' },
  '60': { text: '待评价', tone: 'green' },
  '70': { text: '已完成', tone: 'green' },
  '80': { text: '已取消', tone: 'gray' }
}

function pad(num) {
  return `${num}`.padStart(2, '0')
}

function formatMonthText(year, month) {
  return `${year}年${pad(month)}月`
}

function formatStatus(orderStatus) {
  return STATUS_MAP[orderStatus] || { text: '已预约', tone: 'blue' }
}

function mapOrderItem(item) {
  const statusInfo = formatStatus(item.orderStatus)
  return {
    ...item,
    statusText: statusInfo.text,
    statusTone: statusInfo.tone
  }
}

function buildCalendar(year, month, markedMap) {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const today = new Date()
  const todayText = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
  const days = []
  for (let i = 1; i <= lastDay.getDate(); i += 1) {
    const date = `${year}-${pad(month)}-${pad(i)}`
    const mark = markedMap[date] || null
    const statusInfo = mark ? formatStatus(mark.orderStatus) : null
    days.push({
      day: pad(i),
      date,
      isToday: date === todayText,
      orderId: mark ? mark.orderId : null,
      hasService: !!mark,
      serviceName: mark ? mark.serviceName : '',
      orderStatus: mark ? mark.orderStatus : '',
      statusText: statusInfo ? statusInfo.text : '',
      statusTone: statusInfo ? statusInfo.tone : ''
    })
  }
  return {
    firstWeekDay: firstDay.getDay(),
    days
  }
}

Page({
  data: {
    year: 0,
    month: 0,
    monthText: '',
    weekList: WEEK_LIST,
    calendar: { firstWeekDay: 0, days: [] },
    addressText: '全部服务地址',
    loggedIn: false,
    orders: [],
    recentOrders: [],
    highlightedOrder: null
  },
  onShow() {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() + 1
    const loggedIn = hasLogin()
    this.setData({
      year,
      month,
      loggedIn,
      monthText: formatMonthText(year, month)
    })
    if (!loggedIn) {
      this.setData({
        orders: [],
        recentOrders: [],
        highlightedOrder: null,
        calendar: buildCalendar(year, month, {})
      })
      return
    }
    this.loadOrders()
  },
  async loadOrders() {
    const res = await api.listOrders({ pageNum: 1, pageSize: 100 })
    const orders = (res.rows || []).map(mapOrderItem)
    this.setData({
      orders,
      recentOrders: orders.slice(0, 4),
      highlightedOrder: orders[0] || null,
      addressText: orders[0] && orders[0].serviceAddress ? orders[0].serviceAddress : '全部服务地址'
    })
    this.refreshCalendar()
  },
  refreshCalendar() {
    const markedMap = {}
    ;(this.data.orders || []).forEach(item => {
      if (!item.appointmentDate || markedMap[item.appointmentDate]) {
        return
      }
      markedMap[item.appointmentDate] = item
    })
    this.setData({
      calendar: buildCalendar(this.data.year, this.data.month, markedMap)
    })
  },
  prevMonth() {
    let { year, month } = this.data
    month -= 1
    if (month < 1) {
      month = 12
      year -= 1
    }
    this.setData({
      year,
      month,
      monthText: formatMonthText(year, month)
    })
    this.refreshCalendar()
  },
  nextMonth() {
    let { year, month } = this.data
    month += 1
    if (month > 12) {
      month = 1
      year += 1
    }
    this.setData({
      year,
      month,
      monthText: formatMonthText(year, month)
    })
    this.refreshCalendar()
  },
  goLogin() {
    requireLogin('/pages/schedule/index')
  },
  openOrderDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) {
      return
    }
    wx.navigateTo({ url: `/pages/order/detail/index?id=${id}` })
  }
})
