export const paymentChannels: Record<string, string> = {
  CASH: '现金',
  WECHAT_TRANSFER: '微信转账',
  ALIPAY_TRANSFER: '支付宝转账',
  BANK_TRANSFER: '银行转账',
  OTHER: '其他',
  WX_MINI: '微信在线支付'
}
export const paymentKinds: Record<string, string> = {
  RECEIPT: '收款',
  REFUND: '退款',
  REVERSAL: '收款冲正'
}
export const paymentMethods: Record<string, string> = {
  OFFLINE: '线下支付',
  ONLINE: '线上支付',
  LEGACY: '历史付款（待对账）'
}
