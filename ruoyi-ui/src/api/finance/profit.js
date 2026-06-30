import request from '@/utils/request'

export function listDailyProfit(query) {
  return request({
    url: '/finance/profit/daily',
    method: 'get',
    params: query
  })
}

export function listMonthlyProfit(query) {
  return request({
    url: '/finance/profit/monthly',
    method: 'get',
    params: query
  })
}

export function listOperatorDailyProfit(query) {
  return request({
    url: '/finance/profit/operator/daily',
    method: 'get',
    params: query
  })
}

export function listOperatorMonthlyProfit(query) {
  return request({
    url: '/finance/profit/operator/monthly',
    method: 'get',
    params: query
  })
}
