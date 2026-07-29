package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubAcl
{
    private Long aclId;
    private Long datasetId;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private Integer permissionMask;
    private String createBy;
    private Date createTime;

    public Long getAclId() { return aclId; }
    public void setAclId(Long aclId) { this.aclId = aclId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Integer getPermissionMask() { return permissionMask; }
    public void setPermissionMask(Integer permissionMask) { this.permissionMask = permissionMask; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
