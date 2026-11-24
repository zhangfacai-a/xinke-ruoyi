import request from '@/utils/request'

const root = '/live/freight'
export const freightShops = () => request({ url: `${root}/shops` })
export const freightList = params => request({ url: root, params })
export const freightDashboard = params => request({ url: `${root}/dashboard`, params })
export const freightCreate = data => request({ url: root, method: 'post', data, headers: { skipError: true } })
export const freightUpdate = (id, data) => request({ url: `${root}/${id}`, method: 'put', data, headers: { skipError: true } })
export const freightImport = file => {
  const data = new FormData()
  data.append('file', file)
  return request({ url: `${root}/import`, method: 'post', data, timeout: 120000, headers: { 'Content-Type': 'multipart/form-data', skipError: true, repeatSubmit: false } })
}
export const freightExport = params => request({ url: `${root}/export`, params, responseType: 'blob', timeout: 120000 })
