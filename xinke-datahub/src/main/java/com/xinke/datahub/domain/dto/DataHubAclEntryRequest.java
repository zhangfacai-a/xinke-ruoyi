package com.xinke.datahub.domain.dto;

public class DataHubAclEntryRequest
{
    private String subjectType;
    private Long subjectId;
    private Integer permissionMask;

    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Integer getPermissionMask() { return permissionMask; }
    public void setPermissionMask(Integer permissionMask) { this.permissionMask = permissionMask; }
}
