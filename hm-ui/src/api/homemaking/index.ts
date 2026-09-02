import request from '@/config/axios'

export type BusinessRow = Record<string, any>
export const listCatalog = (kind: string, params = {}) =>
  request.get({ url: `/homemaking/catalog/${kind}`, params })
export const saveCatalog = (kind: string, data: BusinessRow) =>
  request.post({ url: `/homemaking/catalog/${kind}`, data })
export const listBusiness = (kind: string, params = {}) =>
  request.get({ url: `/homemaking/${kind}`, params })
export const orderAction = (id: number, action: string, data = {}) =>
  request.post({ url: `/homemaking/orders/${id}/${action}`, data })
export const refundAction = (id: number, action: string) =>
  request.post({ url: `/homemaking/aftersales/${id}/${action}` })
export const getBrand = () => request.get({ url: '/homemaking/brand' })
export const saveBrand = (data: BusinessRow) => request.put({ url: '/homemaking/brand', data })
export const requestDomain = (domain: string) =>
  request.post({ url: '/homemaking/domains', data: { domain } })
export const verifyDomain = (domain: string) =>
  request.post({ url: '/homemaking/domains/verify', data: { domain } })
export const getPolicy = (event: string) =>
  request.get({ url: '/homemaking/notification-policy', params: { event } })
export const savePolicy = (event: string, data: BusinessRow) =>
  request.put({ url: '/homemaking/notification-policy', params: { event }, data })
