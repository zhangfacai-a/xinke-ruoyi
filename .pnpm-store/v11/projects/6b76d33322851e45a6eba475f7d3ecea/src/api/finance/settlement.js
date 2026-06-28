import request from '@/utils/request'

export function listSettlement(query) {
  return request({ url: '/finance/settlement/list', method: 'get', params: query })
}

export function addSettlement(data) {
  return request({ url: '/finance/settlement', method: 'post', data })
}

export function approveSettlement(settlementNo) {
  return request({ url: `/finance/settlement/${settlementNo}/approve`, method: 'post' })
}

export function createSettlementVoucher(settlementNo) {
  return request({ url: `/finance/settlement/${settlementNo}/voucher`, method: 'post' })
}
