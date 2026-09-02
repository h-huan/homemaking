const { request } = require('./request')

const api = {
  login: async (code) => { const result = await request({ url: '/homemaking/wechat/mini-login', method: 'POST', data: { code, appId: wx.getAccountInfoSync().miniProgram.appId } }); return { ...result, token: result.accessToken } },
  bindMobile: (data) => request({ url: '/homemaking/wechat/phone', method: 'POST', data: { appId: wx.getAccountInfoSync().miniProgram.appId, code: data.phoneCode } }),
  getProfile: async () => { const result = await request({ url: '/homemaking/me' }); return { ...result, customerId: result.id, isBindMobile: !!result.mobile } },
  getHome: () => request({ url: '/mini/home/index' }),
  getNotificationPreferences: () => request({ url: '/homemaking/notification-preferences' }),
  saveNotificationPreference: (data) => request({ url: '/homemaking/notification-preference', method: 'PUT', data }),
  getSubscriptionTemplates: () => request({ url: '/homemaking/wechat/subscription-templates', params: { appId: wx.getAccountInfoSync().miniProgram.appId } }),
  saveSubscription: (templateId, accepted) => request({ url: '/homemaking/wechat/subscription', method: 'POST', data: { appId: wx.getAccountInfoSync().miniProgram.appId, templateId, accepted } }),
  getContent: (contentType) => request({ url: `/mini/home/content/${contentType}` }),
  getServiceDetail: (serviceItemId) => request({ url: `/mini/service/${serviceItemId}` }),
  listServices: (params) => request({ url: '/mini/service/list', params }),
  listAddresses: () => request({ url: '/mini/address/list' }),
  getAddress: (addressId) => request({ url: `/mini/address/${addressId}` }),
  saveAddress: (data) => request({ url: '/mini/address', method: data.addressId ? 'PUT' : 'POST', data }),
  deleteAddress: (addressId) => request({ url: `/mini/address/${addressId}`, method: 'DELETE' }),
  calcOrder: (data) => request({ url: '/mini/order/calc', method: 'POST', data }),
  submitOrder: (data) => request({ url: '/mini/order/submit', method: 'POST', data }),
  listOrders: (params) => request({ url: '/mini/order/list', data: params }),
  getOrder: (orderId) => request({ url: `/mini/order/${orderId}` }),
  preparePayment: (orderId) => request({ url: `/homemaking/orders/${orderId}/mini-pay`, method: 'POST', data: { appId: wx.getAccountInfoSync().miniProgram.appId } }),
  syncPayment: (orderId) => request({ url: `/homemaking/orders/${orderId}/sync-pay`, method: 'POST' }),
  cancelOrder: (orderId, cancelReason) => request({ url: `/mini/order/cancel/${orderId}`, method: 'POST', data: { cancelReason } })
}

module.exports = api
