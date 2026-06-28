import request from '@/utils/request'

export function listPeriod(query) {
  return request({ url: '/finance/config/period/list', method: 'get', params: query })
}

export function listSubject(query) {
  return request({ url: '/finance/config/subject/list', method: 'get', params: query })
}

export function listFeeType(query) {
  return request({ url: '/finance/config/fee-type/list', method: 'get', params: query })
}

export function listPlatformAccount(query) {
  return request({ url: '/finance/config/platform-account/list', method: 'get', params: query })
}

export function listBankAccount(query) {
  return request({ url: '/finance/config/bank-account/list', method: 'get', params: query })
}
