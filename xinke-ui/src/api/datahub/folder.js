import request from '@/utils/request'

export function listFolderTree() {
  return request({
    url: '/datahub/folder/tree',
    method: 'get'
  })
}

export function createFolder(data) {
  return request({
    url: '/datahub/folder',
    method: 'post',
    data
  })
}

export function updateFolder(folderId, data) {
  return request({
    url: `/datahub/folder/${folderId}`,
    method: 'put',
    data
  })
}

export function deleteFolder(folderId, lockVersion) {
  return request({
    url: `/datahub/folder/${folderId}`,
    method: 'delete',
    params: { lockVersion }
  })
}

export function moveDatasetToFolder(datasetId, data) {
  return request({
    url: `/datahub/folder/item/${datasetId}`,
    method: 'put',
    data
  })
}
