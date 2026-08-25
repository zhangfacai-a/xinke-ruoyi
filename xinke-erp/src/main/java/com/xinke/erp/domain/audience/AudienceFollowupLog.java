package com.xinke.erp.domain.audience;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceFollowupLog
{
    private Long logId;
    private Long followupId;
    private String actionType;
    private String beforeJson;
    private String afterJson;
    private String contactMethod;
    private String content;
    private String result;
    private String statusBefore;
    private String statusAfter;
    private Date nextFollowAt;
    private Long operatorUserId;
    private String operatorNameSnapshot;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    public Long getLogId() { return logId; }
    public void setLogId(Long value) { logId = value; }
    public Long getFollowupId() { return followupId; }
    public void setFollowupId(Long value) { followupId = value; }
    public String getActionType() { return actionType; }
    public void setActionType(String value) { actionType = value; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String value) { beforeJson = value; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String value) { afterJson = value; }
    public String getContactMethod() { return contactMethod; }
    public void setContactMethod(String value) { contactMethod = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getResult() { return result; }
    public void setResult(String value) { result = value; }
    public String getStatusBefore() { return statusBefore; }
    public void setStatusBefore(String value) { statusBefore = value; }
    public String getStatusAfter() { return statusAfter; }
    public void setStatusAfter(String value) { statusAfter = value; }
    public Date getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(Date value) { nextFollowAt = value; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long value) { operatorUserId = value; }
    public String getOperatorNameSnapshot() { return operatorNameSnapshot; }
    public void setOperatorNameSnapshot(String value) { operatorNameSnapshot = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
}
