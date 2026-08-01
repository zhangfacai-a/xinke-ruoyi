package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubSchema
{
    private Long schemaId;
    private Long datasetId;
    private Integer versionNo;
    private Long sourceJobId;
    private String schemaHash;
    private String status;
    private String createBy;
    private Date createTime;

    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public Long getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(Long sourceJobId) { this.sourceJobId = sourceJobId; }
    public String getSchemaHash() { return schemaHash; }
    public void setSchemaHash(String schemaHash) { this.schemaHash = schemaHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
