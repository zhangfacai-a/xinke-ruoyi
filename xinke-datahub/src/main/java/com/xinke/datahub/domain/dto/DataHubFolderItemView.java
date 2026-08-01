package com.xinke.datahub.domain.dto;

public class DataHubFolderItemView
{
    private Long datasetId;
    private Long folderId;
    private Integer itemVersion;

    public DataHubFolderItemView(Long datasetId, Long folderId, Integer itemVersion)
    {
        this.datasetId = datasetId;
        this.folderId = folderId;
        this.itemVersion = itemVersion;
    }

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Integer getItemVersion() { return itemVersion; }
    public void setItemVersion(Integer itemVersion) { this.itemVersion = itemVersion; }
}
