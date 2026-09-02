import request from '@/utils/request'

export function listCategory(query) {
  return request({ url: '/admin/hm/category/list', method: 'get', params: query })
}

export function getCategory(categoryId) {
  return request({ url: `/admin/hm/category/${categoryId}`, method: 'get' })
}

export function addCategory(data) {
  return request({ url: '/admin/hm/category', method: 'post', data })
}

export function updateCategory(data) {
  return request({ url: '/admin/hm/category', method: 'put', data })
}

export function delCategory(categoryId) {
  return request({ url: `/admin/hm/category/${categoryId}`, method: 'delete' })
}