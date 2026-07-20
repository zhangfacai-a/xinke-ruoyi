import request from '@/utils/request'

export function listCashFlow(query) {
  return request({ url: '/finance/cashflow/list', method: 'get', params: query })
}

export function getCashFlowSummary(query) {
  return request({ url: '/finance/cashflow/summary', method: 'get', params: query })
}

export function getCashFlowAccountOptions() {
  return request({ url: '/finance/cashflow/account-options', method: 'get' })
}

export function addCashFlow(data) {
  return request({ url: '/finance/cashflow', method: 'post', data })
}

export function postCashFlow(flowNo, data = {}) {
  return request({ url: `/finance/cashflow/${flowNo}/post`, method: 'post', data })
}

export function voidCashFlow(flowNo) {
  return request({ url: `/finance/cashflow/${flowNo}/void`, method: 'post' })
}

export function reverseCashFlow(flowNo, data) {
  return request({ url: `/finance/cashflow/${flowNo}/reverse`, method: 'post', data })
}
