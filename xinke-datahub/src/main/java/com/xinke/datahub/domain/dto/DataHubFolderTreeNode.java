package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubFolderTreeNode
{
    private Long folderId;
    private Long parentFolderId;
    private String folderName;
    private Integer sortOrder;
    private Integer lockVersion;
    private Long itemCount;
    private List<DataHubFolderTreeNode> children = new ArrayList<>();

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Long parentFolderId) { this.parentFolderId = parentFolderId; }
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
    public Long getItemCount() { return itemCount; }
    public void setItemCount(Long itemCount) { this.itemCount = itemCount; }
    public List<DataHubFolderTreeNode> getChildren() { return children; }
    public void setChildren(List<DataHubFolderTreeNode> children) { this.children = children; }
}
