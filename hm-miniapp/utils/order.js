const STATUS_MAP = {
  '10': { label: '待支付', className: 'gray', desc: '支付功能后续开放' },
  '20': { label: '待确认', className: 'blue', desc: '平台正在确认预约信息' },
  '30': { label: '待派单', className: 'blue', desc: '预约已提交，等待安排服务人员' },
  '40': { label: '待上门', className: 'blue', desc: '服务人员已安排，请留意上门时间' },
  '50': { label: '服务中', className: 'blue', desc: '服务进行中' },
  '60': { label: '待评价', className: 'green', desc: '服务完成，等待评价' },
  '70': { label: '已完成', className: 'green', desc: '订单已完成' },
  '80': { label: '已取消', className: 'gray', desc: '订单已取消' },
  '90': { label: '已退款', className: 'gray', desc: '订单已退款' }
}

function getOrderStatusMeta(status) {
  return STATUS_MAP[status] || { label: '处理中', className: 'gray', desc: '订单处理中' }
}

module.exports = {
  STATUS_MAP,
  getOrderStatusMeta
}
