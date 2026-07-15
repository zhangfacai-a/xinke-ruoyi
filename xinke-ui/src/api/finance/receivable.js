import request from '@/utils/request'

export function listReceivable(query) {
  return request({ url: '/finance/receivable/list', method: 'get', params: query })
}

export function writeoffReceivable(receivableNo, data) {
  return request({ url: `/finance/receivable/${receivableNo}/writeoff`, method: 'post', data })
}
