package com.xinke.datahub.domain;

import com.xinke.common.core.domain.BaseEntity;

public class DataHubFolder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long folderId;
    private Long parentFolderId;
    private String folderName;
    private String normalizedName;
    private Long ownerUserId;
    private String ownerUserName;
    private Integer sortOrder;
    private Integer lockVersion;
    private String delFlag;
    private Long itemCount;

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getOwnerUserName() { return ownerUserName; }
    public void setOwnerUserName(String ownerUserName) { this.ownerUserName = ownerUserName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Long getItemCount() { return itemCount; }
    public void setItemCount(Long itemCount) { this.itemCount = itemCount; }
}
