import request from '@/utils/request'

export function listVoucher(query) {
  return request({ url: '/finance/voucher/list', method: 'get', params: query })
}

export function listVoucherEntry(voucherNo) {
  return request({ url: `/finance/voucher/${voucherNo}/entries`, method: 'get' })
}

export function auditVoucher(voucherNo) {
  return request({ url: `/finance/voucher/${voucherNo}/audit`, method: 'post' })
}
