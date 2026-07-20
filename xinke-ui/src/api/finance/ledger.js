import request from '@/utils/request'

export function listLedger(query) {
  return request({ url: '/finance/ledger/list', method: 'get', params: query })
}

export function listTrialBalance(query) {
  return request({ url: '/finance/ledger/trial-balance', method: 'get', params: query })
}

export function getTrialBalanceSummary(query) {
  return request({ url: '/finance/ledger/trial-balance/summary', method: 'get', params: query })
}
