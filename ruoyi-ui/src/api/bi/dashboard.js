import request from '@/utils/request'

export function getProfitSummary(query) {
  return request({
    url: '/bi/profit/summary',
    method: 'get',
    params: query
  })
}

export function getGmvTrend(query) {
  return request({
    url: '/bi/shop/sale',
    method: 'get',
    params: query
  })
}

export function getProductRank(query) {
  return request({
    url: '/bi/product/rank',
    method: 'get',
    params: query
  })
}

export function getProfitTrend(query) {
  return request({
    url: '/bi/profit/trend',
    method: 'get',
    params: query
  })
}
