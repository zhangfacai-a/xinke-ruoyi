import request from '@/utils/request'
export const personnelDetail = id => request({ url: `/live/personnel/${id}` })
export const personnelSummaries = ids => request({ url: '/live/personnel', params: { ids: ids.join(',') } })
export const reconcilePersonnel = () => request({ url:'/live/personnel/reconcile',method:'post',timeout:120000 })
