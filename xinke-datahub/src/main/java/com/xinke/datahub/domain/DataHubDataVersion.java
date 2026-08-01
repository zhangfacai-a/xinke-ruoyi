package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubDataVersion
{
    private Long versionId;
    private Long datasetId;
    private Long parentVersionId;
    private Long schemaId;
    private Long jobId;
    private Integer versionNo;
    private String versionType;
    private String physicalTableName;
    private Long rowCount;
    private String status;
    private Date retentionUntil;
    private Date purgeClaimedAt;
    private Date createTime;

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(Long parentVersionId) { this.parentVersionId = parentVersionId; }
    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public String getPhysicalTableName() { return physicalTableName; }
    public void setPhysicalTableName(String physicalTableName) { this.physicalTableName = physicalTableName; }
    public Long getRowCount() { return rowCount; }
    public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(Date retentionUntil) { this.retentionUntil = retentionUntil; }
    public Date getPurgeClaimedAt() { return purgeClaimedAt; }
    public void setPurgeClaimedAt(Date purgeClaimedAt) { this.purgeClaimedAt = purgeClaimedAt; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
