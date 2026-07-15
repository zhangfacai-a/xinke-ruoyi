import request from '@/utils/request'

export function listPeriodClose(query) {
  return request({ url: '/finance/close/list', method: 'get', params: query })
}

export function checkPeriodClose(data) {
  return request({ url: '/finance/close/check', method: 'post', data })
}

export function closePeriod(data) {
  return request({ url: '/finance/close/close', method: 'post', data })
}

export function reopenPeriod(data) {
  return request({ url: '/finance/close/reopen', method: 'post', data })
}
