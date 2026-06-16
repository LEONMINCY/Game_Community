// 文件说明：utils/payment.js，支付跳转工具，生成支付宝回跳到前端支付结果页的地址。
export const buildPaymentReturnUrl = () => {
  return `${window.location.origin}/paymentResults`
}
