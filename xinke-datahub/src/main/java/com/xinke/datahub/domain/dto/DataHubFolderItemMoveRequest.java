package com.xinke.datahub.domain.dto;

public class DataHubFolderItemMoveRequest
{
    private Long folderId;
    private Integer itemVersion;

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Integer getItemVersion() { return itemVersion; }
    public void setItemVersion(Integer itemVersion) { this.itemVersion = itemVersion; }
}
