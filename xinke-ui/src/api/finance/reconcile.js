import request from '@/utils/request'

export function listReconcileTask(query) {
  return request({ url: '/finance/reconcile/task/list', method: 'get', params: query })
}

export function listReconcileDiff(query) {
  return request({ url: '/finance/reconcile/diff/list', method: 'get', params: query })
}

export function runReconcile(data) {
  return request({ url: '/finance/reconcile/run', method: 'post', data })
}

export function resolveReconcileDiff(diffNo, data) {
  return request({ url: `/finance/reconcile/diff/${diffNo}/resolve`, method: 'post', data })
}
