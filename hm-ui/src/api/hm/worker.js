import request from '@/utils/request'

export function listWorker(query) {
  return request({ url: '/admin/hm/worker/list', method: 'get', params: query })
}

export function getWorker(workerId) {
  return request({ url: `/admin/hm/worker/${workerId}`, method: 'get' })
}

export function addWorker(data) {
  return request({ url: '/admin/hm/worker', method: 'post', data })
}

export function updateWorker(data) {
  return request({ url: '/admin/hm/worker', method: 'put', data })
}