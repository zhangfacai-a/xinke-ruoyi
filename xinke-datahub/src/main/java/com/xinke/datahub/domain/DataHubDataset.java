package com.xinke.datahub.domain;

import com.xinke.common.core.domain.BaseEntity;

public class DataHubDataset extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long datasetId;
    private String displayName;
    private String normalizedName;
    private String datasetCode;
    private Long ownerUserId;
    private String ownerUserName;
    private Long currentSchemaId;
    private Long currentVersionId;
    private Integer currentSchemaVersion;
    private Integer currentVersionNo;
    private Long rowCount;
    private Integer columnCount;
    private String status;
    private String sourceFileName;
    private String sourceSheetName;
    private Long activeJobId;
    private Integer lockVersion;
    private String delFlag;
    private Integer accessMask;
    private Long folderId;
    private Integer folderItemVersion;
    private String folderScope;

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getDatasetCode() { return datasetCode; }
    public void setDatasetCode(String datasetCode) { this.datasetCode = datasetCode; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getOwnerUserName() { return ownerUserName; }
    public void setOwnerUserName(String ownerUserName) { this.ownerUserName = ownerUserName; }
    public Long getCurrentSchemaId() { return currentSchemaId; }
    public void setCurrentSchemaId(Long currentSchemaId) { this.currentSchemaId = currentSchemaId; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public Integer getCurrentSchemaVersion() { return currentSchemaVersion; }
    public void setCurrentSchemaVersion(Integer currentSchemaVersion) { this.currentSchemaVersion = currentSchemaVersion; }
    public Integer getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(Integer currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public Long getRowCount() { return rowCount; }
    public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
    public Integer getColumnCount() { return columnCount; }
    public void setColumnCount(Integer columnCount) { this.columnCount = columnCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String sourceSheetName) { this.sourceSheetName = sourceSheetName; }
    public Long getActiveJobId() { return activeJobId; }
    public void setActiveJobId(Long activeJobId) { this.activeJobId = activeJobId; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Integer getAccessMask() { return accessMask; }
    public void setAccessMask(Integer accessMask) { this.accessMask = accessMask; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public Integer getFolderItemVersion() { return folderItemVersion; }
    public void setFolderItemVersion(Integer folderItemVersion) { this.folderItemVersion = folderItemVersion; }
    public String getFolderScope() { return folderScope; }
    public void setFolderScope(String folderScope) { this.folderScope = folderScope; }
}
