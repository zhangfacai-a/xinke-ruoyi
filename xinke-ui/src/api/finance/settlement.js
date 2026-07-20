import request from '@/utils/request'

export function listSettlement(query) {
  return request({ url: '/finance/settlement/list', method: 'get', params: query })
}

export function addSettlement(data) {
  return request({ url: '/finance/settlement', method: 'post', data })
}

export function updateSettlement(settlementNo, data) {
  return request({ url: `/finance/settlement/${settlementNo}`, method: 'put', data })
}

export function voidSettlement(settlementNo) {
  return request({ url: `/finance/settlement/${settlementNo}/void`, method: 'post' })
}

export function approveSettlement(settlementNo) {
  return request({ url: `/finance/settlement/${settlementNo}/approve`, method: 'post' })
}

export function createSettlementVoucher(settlementNo) {
  return request({ url: `/finance/settlement/${settlementNo}/voucher`, method: 'post' })
}
