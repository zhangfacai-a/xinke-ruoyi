package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubFolderItem
{
    private Long itemId;
    private Long ownerUserId;
    private Long folderId;
    private Long datasetId;
    private Integer lockVersion;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
