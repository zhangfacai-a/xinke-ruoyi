import request from '@/utils/request'

export function listCashFlow(query) {
  return request({ url: '/finance/cashflow/list', method: 'get', params: query })
}

export function addCashFlow(data) {
  return request({ url: '/finance/cashflow', method: 'post', data })
}
