package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubImportJob
{
    private Long jobId;
    private String jobNo;
    private String previewId;
    private Long datasetId;
    private String operationType;
    private Long sourceVersionId;
    private Integer sourceLockVersion;
    private Long targetVersionId;
    private Long rollbackTargetVersionId;
    private Long retryOfJobId;
    private String status;
    private String phase;
    private String sourceFileName;
    private String storedFilePath;
    private String fileType;
    private String fileHash;
    private String dedupKey;
    private String sheetName;
    private String proposedDisplayName;
    private String proposedPhysicalName;
    private String schemaSnapshotJson;
    private String operationPayloadJson;
    private String stagingTableName;
    private String targetTableName;
    private Long totalRows;
    private Long processedRows;
    private Long successRows;
    private Long failedRows;
    private String errorMessage;
    private Long uploadUserId;
    private String uploadUserName;
    private Date expireTime;
    private Date startTime;
    private Date finishTime;
    private String cancelRequested;
    private Integer lockVersion;
    private Date createTime;
    private Date updateTime;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobNo() { return jobNo; }
    public void setJobNo(String jobNo) { this.jobNo = jobNo; }
    public String getPreviewId() { return previewId; }
    public void setPreviewId(String previewId) { this.previewId = previewId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public Long getSourceVersionId() { return sourceVersionId; }
    public void setSourceVersionId(Long sourceVersionId) { this.sourceVersionId = sourceVersionId; }
    public Integer getSourceLockVersion() { return sourceLockVersion; }
    public void setSourceLockVersion(Integer sourceLockVersion) { this.sourceLockVersion = sourceLockVersion; }
    public Long getTargetVersionId() { return targetVersionId; }
    public void setTargetVersionId(Long targetVersionId) { this.targetVersionId = targetVersionId; }
    public Long getRollbackTargetVersionId() { return rollbackTargetVersionId; }
    public void setRollbackTargetVersionId(Long rollbackTargetVersionId) { this.rollbackTargetVersionId = rollbackTargetVersionId; }
    public Long getRetryOfJobId() { return retryOfJobId; }
    public void setRetryOfJobId(Long retryOfJobId) { this.retryOfJobId = retryOfJobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public String getStoredFilePath() { return storedFilePath; }
    public void setStoredFilePath(String storedFilePath) { this.storedFilePath = storedFilePath; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getProposedDisplayName() { return proposedDisplayName; }
    public void setProposedDisplayName(String proposedDisplayName) { this.proposedDisplayName = proposedDisplayName; }
    public String getProposedPhysicalName() { return proposedPhysicalName; }
    public void setProposedPhysicalName(String proposedPhysicalName) { this.proposedPhysicalName = proposedPhysicalName; }
    public String getSchemaSnapshotJson() { return schemaSnapshotJson; }
    public void setSchemaSnapshotJson(String schemaSnapshotJson) { this.schemaSnapshotJson = schemaSnapshotJson; }
    public String getOperationPayloadJson() { return operationPayloadJson; }
    public void setOperationPayloadJson(String operationPayloadJson) { this.operationPayloadJson = operationPayloadJson; }
    public String getStagingTableName() { return stagingTableName; }
    public void setStagingTableName(String stagingTableName) { this.stagingTableName = stagingTableName; }
    public String getTargetTableName() { return targetTableName; }
    public void setTargetTableName(String targetTableName) { this.targetTableName = targetTableName; }
    public Long getTotalRows() { return totalRows; }
    public void setTotalRows(Long totalRows) { this.totalRows = totalRows; }
    public Long getProcessedRows() { return processedRows; }
    public void setProcessedRows(Long processedRows) { this.processedRows = processedRows; }
    public Long getSuccessRows() { return successRows; }
    public void setSuccessRows(Long successRows) { this.successRows = successRows; }
    public Long getFailedRows() { return failedRows; }
    public void setFailedRows(Long failedRows) { this.failedRows = failedRows; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getUploadUserId() { return uploadUserId; }
    public void setUploadUserId(Long uploadUserId) { this.uploadUserId = uploadUserId; }
    public String getUploadUserName() { return uploadUserName; }
    public void setUploadUserName(String uploadUserName) { this.uploadUserName = uploadUserName; }
    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
    public String getCancelRequested() { return cancelRequested; }
    public void setCancelRequested(String cancelRequested) { this.cancelRequested = cancelRequested; }
    public Integer getLockVersion() { return lockVersion; }
    public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
