import request from '@/utils/request'

export function listHomeConfig() {
  return request({ url: '/admin/hm/mini-config/home/list', method: 'get' })
}

export function saveHomeConfig(data) {
  return request({ url: '/admin/hm/mini-config/home/save', method: 'post', data })
}

export function listNavConfig() {
  return request({ url: '/admin/hm/mini-config/nav/list', method: 'get' })
}

export function saveNavConfig(data) {
  return request({ url: '/admin/hm/mini-config/nav/save', method: 'post', data })
}

export function listPortalContent(params) {
  return request({ url: '/admin/hm/mini-config/content/list', method: 'get', params })
}

export function getPortalContent(contentId) {
  return request({ url: `/admin/hm/mini-config/content/${contentId}`, method: 'get' })
}

export function savePortalContent(data) {
  return request({ url: '/admin/hm/mini-config/content/save', method: 'post', data })
}

export function deletePortalContent(contentId) {
  return request({ url: `/admin/hm/mini-config/content/${contentId}`, method: 'delete' })
}
