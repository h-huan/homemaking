const { request } = require('./request')

const api = {
  login: (code) => request({ url: '/mini/auth/login', method: 'POST', data: { code } }),
  bindMobile: (data) => request({ url: '/mini/auth/bindMobile', method: 'POST', data }),
  getProfile: () => request({ url: '/mini/auth/profile' }),
  getHome: () => request({ url: '/mini/home/index' }),
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
  cancelOrder: (orderId, cancelReason) => request({ url: `/mini/order/cancel/${orderId}`, method: 'POST', data: { cancelReason } })
}

module.exports = api
