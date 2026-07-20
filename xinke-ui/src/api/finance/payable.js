import request from '@/utils/request'

export function listPayable(query) {
  return request({ url: '/finance/payable/list', method: 'get', params: query })
}

export function getPayableSummary(query) {
  return request({ url: '/finance/payable/summary', method: 'get', params: query })
}

export function getPayableAging(query) {
  return request({ url: '/finance/payable/aging', method: 'get', params: query })
}

export function listSupplierInvoice(query) {
  return request({ url: '/finance/payable/invoice/list', method: 'get', params: query })
}

export function listSupplierInvoiceItems(invoiceNo) {
  return request({ url: `/finance/payable/invoice/${invoiceNo}/items`, method: 'get' })
}

export function listInvoicePurchaseOptions(query) {
  return request({ url: '/finance/payable/invoice/purchase/options', method: 'get', params: query })
}

export function getInvoicePurchaseContext(purchaseNo) {
  return request({ url: `/finance/payable/invoice/purchase/${purchaseNo}`, method: 'get' })
}

export function addSupplierInvoice(data) {
  return request({ url: '/finance/payable/invoice', method: 'post', data })
}

export function approveSupplierInvoice(invoiceNo, data) {
  return request({ url: `/finance/payable/invoice/${invoiceNo}/approve`, method: 'post', data })
}

export function listPaymentRequest(query) {
  return request({ url: '/finance/payable/payment/list', method: 'get', params: query })
}

export function listPaymentBankOptions() {
  return request({ url: '/finance/payable/payment/bank/options', method: 'get' })
}

export function addPaymentRequest(payableNo, data) {
  return request({ url: `/finance/payable/${payableNo}/payment-request`, method: 'post', data })
}

export function approvePaymentRequest(paymentNo) {
  return request({ url: `/finance/payable/payment/${paymentNo}/approve`, method: 'post' })
}

export function rejectPaymentRequest(paymentNo, data) {
  return request({ url: `/finance/payable/payment/${paymentNo}/reject`, method: 'post', data })
}

export function executePaymentRequest(paymentNo, data) {
  return request({ url: `/finance/payable/payment/${paymentNo}/execute`, method: 'post', data })
}

export function writeoffPayable(payableNo, data) {
  return request({ url: `/finance/payable/${payableNo}/writeoff`, method: 'post', data })
}
