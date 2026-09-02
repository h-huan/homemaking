import request from '@/utils/request'

export function listService(query) {
  return request({ url: '/admin/hm/service/list', method: 'get', params: query })
}

export function getService(serviceItemId) {
  return request({ url: `/admin/hm/service/${serviceItemId}`, method: 'get' })
}

export function addService(data) {
  return request({ url: '/admin/hm/service', method: 'post', data })
}

export function updateService(data) {
  return request({ url: '/admin/hm/service', method: 'put', data })
}

export function delService(serviceItemId) {
  return request({ url: `/admin/hm/service/${serviceItemId}`, method: 'delete' })
}