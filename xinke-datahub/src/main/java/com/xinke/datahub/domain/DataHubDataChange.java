package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubDataChange
{
    private Long changeId;
    private Long datasetId;
    private Long jobId;
    private Long targetVersionId;
    private String clientMutationId;
    private Long rowId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String createBy;
    private Date createTime;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getTargetVersionId() { return targetVersionId; }
    public void setTargetVersionId(Long targetVersionId) { this.targetVersionId = targetVersionId; }
    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
