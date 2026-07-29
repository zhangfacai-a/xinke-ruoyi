package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubImportError
{
    private Long errorId;
    private Long jobId;
    private Integer sourceRowNo;
    private String sourceColumnName;
    private String physicalColumnName;
    private String rawValue;
    private String errorCode;
    private String errorMessage;
    private Date createTime;

    public Long getErrorId() { return errorId; }
    public void setErrorId(Long errorId) { this.errorId = errorId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Integer getSourceRowNo() { return sourceRowNo; }
    public void setSourceRowNo(Integer sourceRowNo) { this.sourceRowNo = sourceRowNo; }
    public String getSourceColumnName() { return sourceColumnName; }
    public void setSourceColumnName(String sourceColumnName) { this.sourceColumnName = sourceColumnName; }
    public String getPhysicalColumnName() { return physicalColumnName; }
    public void setPhysicalColumnName(String physicalColumnName) { this.physicalColumnName = physicalColumnName; }
    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
