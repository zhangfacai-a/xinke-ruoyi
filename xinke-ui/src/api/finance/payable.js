import request from '@/utils/request'

export function listPayable(query) {
  return request({ url: '/finance/payable/list', method: 'get', params: query })
}

export function writeoffPayable(payableNo, data) {
  return request({ url: `/finance/payable/${payableNo}/writeoff`, method: 'post', data })
}
