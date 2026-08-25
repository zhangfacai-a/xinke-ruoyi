package com.xinke.erp.domain.audience;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** One independent purchase opportunity in a customer's lifecycle. */
public class AudienceOpportunity
{
    private Long opportunityId;
    private Long followupId;
    private Integer sequenceNo;
    private Boolean current;
    private String status;
    private String followResultCode;
    private String intentLevel;
    private String consultModel;
    private Long sourceRoomId;
    private String sourceRoomName;
    private Long ownerUserId;
    private String ownerNameSnapshot;
    private String closeReasonCode;
    private String closeReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date openedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long value) { opportunityId = value; }
    public Long getFollowupId() { return followupId; }
    public void setFollowupId(Long value) { followupId = value; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer value) { sequenceNo = value; }
    public Boolean getCurrent() { return current; }
    public void setCurrent(Boolean value) { current = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getFollowResultCode() { return followResultCode; }
    public void setFollowResultCode(String value) { followResultCode = value; }
    public String getIntentLevel() { return intentLevel; }
    public void setIntentLevel(String value) { intentLevel = value; }
    public String getConsultModel() { return consultModel; }
    public void setConsultModel(String value) { consultModel = value; }
    public Long getSourceRoomId() { return sourceRoomId; }
    public void setSourceRoomId(Long value) { sourceRoomId = value; }
    public String getSourceRoomName() { return sourceRoomName; }
    public void setSourceRoomName(String value) { sourceRoomName = value; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { ownerUserId = value; }
    public String getOwnerNameSnapshot() { return ownerNameSnapshot; }
    public void setOwnerNameSnapshot(String value) { ownerNameSnapshot = value; }
    public String getCloseReasonCode() { return closeReasonCode; }
    public void setCloseReasonCode(String value) { closeReasonCode = value; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String value) { closeReason = value; }
    public Date getOpenedAt() { return openedAt; }
    public void setOpenedAt(Date value) { openedAt = value; }
    public Date getClosedAt() { return closedAt; }
    public void setClosedAt(Date value) { closedAt = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
}
