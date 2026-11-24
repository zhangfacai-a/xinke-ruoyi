import request from '@/utils/request'

const root = '/live/jd-customers'
export const jdRecords = params => request({ url: `${root}/records`, params })
export const jdRecordOrders = id => request({ url: `${root}/records/${id}/orders` })
export const jdCustomers = params => request({ url: `${root}/customers`, params })
export const jdCustomer = id => request({ url: `${root}/customers/${id}` })
export const jdBatches = () => request({ url: `${root}/batches` })
export const jdContact = (id, data) => request({ url: `${root}/customers/${id}/contact`, method: 'put', data, headers: { skipError: true } })
export const jdLinkOrder = data => request({ url: `${root}/orders/link`, method: 'post', data, headers: { skipError: true } })
export const jdMerge = data => request({ url: `${root}/customers/merge`, method: 'post', data, headers: { skipError: true } })
export const jdImport = file => {
  const data = new FormData()
  data.append('file', file)
  return request({ url: `${root}/import`, method: 'post', data, timeout: 120000, headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false, skipError: true } })
}
