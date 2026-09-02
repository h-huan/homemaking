import request from '@/utils/request'

export function listOrder(query) {
  return request({ url: '/admin/hm/order/list', method: 'get', params: query })
}

export function getOrder(orderId) {
  return request({ url: `/admin/hm/order/${orderId}`, method: 'get' })
}

export function assignOrder(data) {
  return request({ url: '/admin/hm/order/assign', method: 'post', data })
}

export function cancelOrder(data) {
  return request({ url: '/admin/hm/order/cancel', method: 'post', data })
}

export function refundOrder(data) {
  return request({ url: '/admin/hm/order/refund', method: 'post', data })
}