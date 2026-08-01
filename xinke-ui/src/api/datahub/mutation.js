import request from '@/utils/request'

export function previewDatasetImport(datasetId, data, onUploadProgress) {
  return request({
    url: `/datahub/dataset/${datasetId}/import/preview`,
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

export function updateDatasetImportSheet(datasetId, previewId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/import/${previewId}/sheet`,
    method: 'put',
    data,
    timeout: 120000
  })
}

export function confirmDatasetImport(datasetId, previewId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/import/${previewId}/confirm`,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 30000
  })
}

export function commitDatasetEdit(datasetId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/edit`,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 60000
  })
}

export function clearDataset(datasetId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/clear`,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 30000
  })
}

export function listDatasetVersions(datasetId) {
  return request({
    url: `/datahub/dataset/${datasetId}/versions`,
    method: 'get'
  })
}

export function rollbackDatasetVersion(datasetId, versionId, data) {
  return request({
    url: `/datahub/dataset/${datasetId}/versions/${versionId}/rollback`,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 30000
  })
}

export function getMutationJob(previewId) {
  return request({
    url: `/datahub/import/${previewId}`,
    method: 'get'
  })
}

export function getMutationErrors(previewId) {
  return request({
    url: `/datahub/import/${previewId}/errors`,
    method: 'get'
  })
}
