import request from '@/utils/request'

export function listArea(query) {
  return request({ url: '/admin/hm/area/list', method: 'get', params: query })
}

export function getArea(areaId) {
  return request({ url: `/admin/hm/area/${areaId}`, method: 'get' })
}

export function addArea(data) {
  return request({ url: '/admin/hm/area', method: 'post', data })
}

export function updateArea(data) {
  return request({ url: '/admin/hm/area', method: 'put', data })
}