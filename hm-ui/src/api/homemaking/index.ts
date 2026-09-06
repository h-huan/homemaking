import request from '@/config/axios'

export type BusinessRow = Record<string, any>
export const getPaymentSettings = () => request.get({ url: '/homemaking/payment-settings' })
export const savePaymentSettings = (data: BusinessRow) =>
  request.put({ url: '/homemaking/payment-settings', data })
export const getPaymentEntries = (id: number) =>
  request.get({ url: `/homemaking/orders/${id}/payments` })
export const recordOfflineReceipt = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/orders/${id}/offline-receipt`, data })
export const recordOfflineRefund = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/aftersales/${id}/offline-refund`, data })
export const reverseReceipt = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/orders/${id}/reverse-receipt`, data })
export const paymentLedger = (params: BusinessRow) =>
  request.get({ url: '/homemaking/payment-ledger', params })
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
export const retryRefund = (id: number, remark: string) =>
  request.post({ url: `/homemaking/aftersales/${id}/retry`, data: { remark } })
export const rejectRefund = (id: number, remark: string) =>
  request.post({ url: `/homemaking/aftersales/${id}/reject`, data: { remark } })
export const getAftersale = (id: number) => request.get({ url: `/homemaking/aftersales/${id}` })
export const scheduleAftersale = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/aftersales/${id}/schedule`, data })
export const resolveAftersale = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/aftersales/${id}/resolve`, data })
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
export const getPortal = () => request.get({ url: '/homemaking/portal' })
export const savePortal = (data: BusinessRow) => request.put({ url: '/homemaking/portal', data })
export const publishPortal = (version: number) =>
  request.post({ url: '/homemaking/portal/publish', data: { version } })
export const getWorkerCalendar = (id: number, from: string, to: string) =>
  request.get({ url: `/homemaking/workers/${id}/calendar`, params: { from, to } })
export const saveWorkerSkills = (id: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/workers/${id}/skills`, data })
export const bindWorker = (id: number, userId: number) =>
  request.post({ url: `/homemaking/workers/${id}/account`, data: { userId } })
export const addWorkerInterval = (id: number, kind: 'schedules' | 'leaves', data: BusinessRow) =>
  request.post({ url: `/homemaking/workers/${id}/${kind}`, data })
export const workerTasks = (from: string, to: string) =>
  request.get({ url: '/homemaking/worker/tasks', params: { from, to } })
export const workerAction = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/worker/orders/${id}/action`, data })
export const addWorkerEvidence = (id: number, data: FormData) =>
  request.post({
    url: `/homemaking/worker/orders/${id}/evidence`,
    data,
    headersType: 'multipart/form-data'
  })
export const getWorkerEvidence = (id: number) =>
  request.get({ url: `/homemaking/worker/orders/${id}/evidence` })
export const evidenceImage = (orderId: number, id: number, worker = false) =>
  request.download<Blob>({
    url: `/homemaking/${worker ? 'worker/' : ''}orders/${orderId}/evidence/${id}/content`
  })
export const getQuota = (tenantId?: number) =>
  request.get({ url: '/homemaking/quota', params: tenantId ? { tenantId } : {} })
export const saveQuota = (tenantId: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/quota/${tenantId}`, data })
export const getCommissionRule = (tenantId: number) =>
  request.get({ url: `/homemaking/commission-rule/${tenantId}` })
export const saveCommissionRule = (tenantId: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/commission-rule/${tenantId}`, data })
export const listStatements = (tenantId: number) =>
  request.get({ url: '/homemaking/settlement-statements', params: { tenantId } })
export const statementAction = (id: number, action: string, data = {}) =>
  request.post({ url: `/homemaking/settlement-statements/${id}/${action}`, data })
export const createStatement = (data: BusinessRow) =>
  request.post({ url: '/homemaking/settlement-statements', data })
export const listPlans = () => request.get({ url: '/homemaking/plans' })
export const savePlan = (data: BusinessRow) => request.post({ url: '/homemaking/plans', data })
export const removeWorkerInterval = (workerId: number, kind: 'schedules' | 'leaves', id: number) =>
  request.delete({ url: `/homemaking/workers/${workerId}/${kind}/${id}` })
export const listShiftTemplates = () => request.get({ url: '/homemaking/shift-templates' })
export const saveShiftTemplate = (data: BusinessRow) =>
  request.post({ url: '/homemaking/shift-templates', data })
export const applyShiftTemplate = (workerId: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/workers/${workerId}/apply-shift`, data })
export const previewOrderChange = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/orders/${id}/changes/preview`, data })
export const createOrderChange = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/orders/${id}/changes`, data })
export const cancelOrderChange = (id: number, changeId: number, reason: string) =>
  request.post({ url: `/homemaking/orders/${id}/changes/${changeId}/cancel`, data: { reason } })
export const getOrderDetail = (id: number) => request.get({ url: `/homemaking/orders/${id}` })
export const getOrderEvidence = (id: number) =>
  request.get({ url: `/homemaking/orders/${id}/evidence` })
export const listReviews = () => request.get({ url: '/homemaking/reviews' })
export const setReviewVisible = (id: number, visible: boolean) =>
  request.put({ url: `/homemaking/reviews/${id}/visibility`, data: { visible } })
export const getReview = (id: number) => request.get({ url: `/homemaking/reviews/${id}` })
export const moderateReview = (id: number, data: { visible: boolean; recommended: boolean }) =>
  request.put({ url: `/homemaking/reviews/${id}/moderation`, data })
export const replyReview = (id: number, content: string) =>
  request.post({ url: `/homemaking/reviews/${id}/reply`, data: { content } })
export const reviewImage = (reviewId: number, imageId: number) =>
  request.download<Blob>({ url: `/homemaking/reviews/${reviewId}/images/${imageId}/content` })

export const getServiceSettings = (id: number) =>
  request.get({ url: `/homemaking/services/${id}/settings` })
export const saveServiceSettings = (id: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/services/${id}/settings`, data })

export const workerCalendar = (from: string, to: string) =>
  request.get({ url: '/homemaking/worker/calendar', params: { from, to } })
export const workerIncome = (params: BusinessRow) =>
  request.get({ url: '/homemaking/worker/income', params })
export const workerRequestLeave = (data: BusinessRow) =>
  request.post({ url: '/homemaking/worker/time-off', data })
export const workerCancelLeave = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/worker/time-off/${id}/cancel`, data })
export const reviewWorkerLeave = (worker: number, id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/workers/${worker}/time-off/${id}/review`, data })
export const cancelWorkerLeave = (worker: number, id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/workers/${worker}/time-off/${id}/cancel`, data })

export const listFranchiseTenants = () => request.get({ url: '/homemaking/franchises/tenants' })
export const listFranchises = () => request.get({ url: '/homemaking/franchises' })
export const getFranchise = (id: number) => request.get({ url: `/homemaking/franchises/${id}` })
export const createFranchise = (data: BusinessRow) =>
  request.post({ url: '/homemaking/franchises', data })
export const updateFranchise = (id: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/franchises/${id}`, data })
export const revealFranchiseSensitive = (id: number, reason: string) =>
  request.post({ url: `/homemaking/franchises/${id}/sensitive`, data: { reason } })
export const createFranchiseContract = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/franchises/${id}/contracts`, data })
export const updateFranchiseContract = (id: number, contractId: number, data: BusinessRow) =>
  request.put({ url: `/homemaking/franchises/${id}/contracts/${contractId}`, data })
export const activateFranchiseContract = (id: number, contractId: number, version: number) =>
  request.post({ url: `/homemaking/franchises/${id}/contracts/${contractId}/activate`, data: { version } })
export const renewFranchiseContract = (id: number, contractId: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/franchises/${id}/contracts/${contractId}/renew`, data })
export const terminateFranchiseContract = (id: number, contractId: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/franchises/${id}/contracts/${contractId}/terminate`, data })
export const recordFranchiseDeposit = (id: number, contractId: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/franchises/${id}/contracts/${contractId}/deposits`, data })
export const terminateFranchise = (id: number, data: BusinessRow) =>
  request.post({ url: `/homemaking/franchises/${id}/terminate`, data })
