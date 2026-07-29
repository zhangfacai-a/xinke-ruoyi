package com.xinke.datahub.domain.dto;

public class DataHubFolderRequest
{
    private Long parentFolderId;
    private String folderName;
    private Integer lockVersion;

    public Long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
}
