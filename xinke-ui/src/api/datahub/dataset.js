import request from '@/utils/request'

export function previewImport(data, onUploadProgress) {
  return request({
    url: '/datahub/import/preview',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data',
      repeatSubmit: false
    },
    timeout: 120000,
    onUploadProgress
  })
}

export function updatePreviewSheet(previewId, data) {
  return request({
    url: `/datahub/import/${previewId}/sheet`,
    method: 'put',
    data,
    timeout: 60000
  })
}

export function confirmImport(previewId, data) {
  return request({
    url: `/datahub/import/${previewId}/confirm`,
    method: 'post',
    data,
    timeout: 30000
  })
}

export function getImportJob(previewId) {
  return request({
    url: `/datahub/import/${previewId}`,
    method: 'get'
  })
}

export function getImportErrors(previewId) {
  return request({
    url: `/datahub/import/${previewId}/errors`,
    method: 'get'
  })
}

export function listDataset(query) {
  return request({
    url: '/datahub/dataset/list',
    method: 'get',
    params: query
  })
}

export function getDataset(datasetId) {
  return request({
    url: `/datahub/dataset/${datasetId}`,
    method: 'get'
  })
}

export function queryDatasetData(datasetId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/data/query`,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 30000
  })
}

export function listDatasetJobs(datasetId) {
  return request({
    url: `/datahub/dataset/${datasetId}/jobs`,
    method: 'get'
  })
}

export function getDatasetAcl(datasetId) {
  return request({
    url: `/datahub/dataset/${datasetId}/acl`,
    method: 'get'
  })
}

export function updateDatasetAcl(datasetId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/acl`,
    method: 'put',
    data
  })
}

export function listAccessUsers(query) {
  return request({
    url: '/datahub/access/users',
    method: 'get',
    params: query
  })
}

export function listAccessRoles(query) {
  return request({
    url: '/datahub/access/roles',
    method: 'get',
    params: query
  })
}
