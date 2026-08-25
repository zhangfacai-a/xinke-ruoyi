package com.xinke.erp.domain.audience;

import java.util.Date;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

/** Editable follow-up record kept separately from ranking snapshots. */
public class AudienceFollowup
{
    private Long followupId;
    private Long profileId;
    private String roomScopeKey;
    private Long roomId;
    private String roomNameSnapshot;
    private String secUid;
    private String nicknameSnapshot;
    private String contactPhone;
    private String contactWechat;
    private Long ownerUserId;
    private String ownerNameSnapshot;
    private Long anchorUserId;
    private String anchorNameSnapshot;
    private Long controllerUserId;
    private String controllerNameSnapshot;
    private String status;
    private String followResultCode;
    private String intentLevel;
    private String consultModel;
    private String orderNo;
    private Integer orderCount;
    private Boolean priority;
    private Boolean reactivationPending;
    private String qualificationReason;
    private Long commentCount;
    private Integer commentRank;
    private Long watchSeconds;
    private Integer watchRank;
    private Boolean isFollower;
    private Boolean isFollowing;
    private Integer payLevel;
    private Integer appearanceDays;
    private Integer consecutiveDays;
    private Integer bestCommentRank;
    private Integer bestWatchRank;
    private LocalDate latestVisitDate;
    private String visitDatesCsv;
    private Integer version;
    private Long firstSourceBatchId;
    private Long lastSourceBatchId;
    private String lastFollowResult;
    private String remark;
    private String closeReason;
    private String closeReasonCode;
    private AudienceOpportunity currentOpportunity;
    private java.util.List<AudienceOpportunity> opportunities;
    private java.util.List<AudienceCustomerOrder> orders;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastContactAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextFollowAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statusChangedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date qualifiedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String updateBy;

    public Long getFollowupId() { return followupId; }
    public void setFollowupId(Long value) { followupId = value; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long value) { profileId = value; }
    public String getRoomScopeKey() { return roomScopeKey; }
    public void setRoomScopeKey(String value) { roomScopeKey = value; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long value) { roomId = value; }
    public String getRoomNameSnapshot() { return roomNameSnapshot; }
    public void setRoomNameSnapshot(String value) { roomNameSnapshot = value; }
    public String getSecUid() { return secUid; }
    public void setSecUid(String value) { secUid = value; }
    public String getNicknameSnapshot() { return nicknameSnapshot; }
    public void setNicknameSnapshot(String value) { nicknameSnapshot = value; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String value) { contactPhone = value; }
    public String getContactWechat() { return contactWechat; }
    public void setContactWechat(String value) { contactWechat = value; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { ownerUserId = value; }
    public String getOwnerNameSnapshot() { return ownerNameSnapshot; }
    public void setOwnerNameSnapshot(String value) { ownerNameSnapshot = value; }
    public Long getAnchorUserId() { return anchorUserId; }
    public void setAnchorUserId(Long value) { anchorUserId = value; }
    public String getAnchorNameSnapshot() { return anchorNameSnapshot; }
    public void setAnchorNameSnapshot(String value) { anchorNameSnapshot = value; }
    public Long getControllerUserId() { return controllerUserId; }
    public void setControllerUserId(Long value) { controllerUserId = value; }
    public String getControllerNameSnapshot() { return controllerNameSnapshot; }
    public void setControllerNameSnapshot(String value) { controllerNameSnapshot = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getFollowResultCode() { return followResultCode; }
    public void setFollowResultCode(String value) { followResultCode = value; }
    public String getIntentLevel() { return intentLevel; }
    public void setIntentLevel(String value) { intentLevel = value; }
    public String getConsultModel() { return consultModel; }
    public void setConsultModel(String value) { consultModel = value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { orderNo = value; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer value) { orderCount = value; }
    public Boolean getPriority() { return priority; }
    public void setPriority(Boolean value) { priority = value; }
    public Boolean getReactivationPending() { return reactivationPending; }
    public void setReactivationPending(Boolean value) { reactivationPending = value; }
    public String getQualificationReason() { return qualificationReason; }
    public void setQualificationReason(String value) { qualificationReason = value; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long value) { commentCount = value; }
    public Integer getCommentRank() { return commentRank; }
    public void setCommentRank(Integer value) { commentRank = value; }
    public Long getWatchSeconds() { return watchSeconds; }
    public void setWatchSeconds(Long value) { watchSeconds = value; }
    public Integer getWatchRank() { return watchRank; }
    public void setWatchRank(Integer value) { watchRank = value; }
    public Boolean getIsFollower() { return isFollower; }
    public void setIsFollower(Boolean value) { isFollower = value; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean value) { isFollowing = value; }
    public Integer getPayLevel() { return payLevel; }
    public void setPayLevel(Integer value) { payLevel = value; }
    public Integer getAppearanceDays() { return appearanceDays; }
    public void setAppearanceDays(Integer value) { appearanceDays = value; }
    public Integer getConsecutiveDays() { return consecutiveDays; }
    public void setConsecutiveDays(Integer value) { consecutiveDays = value; }
    public Integer getBestCommentRank() { return bestCommentRank; }
    public void setBestCommentRank(Integer value) { bestCommentRank = value; }
    public Integer getBestWatchRank() { return bestWatchRank; }
    public void setBestWatchRank(Integer value) { bestWatchRank = value; }
    public LocalDate getLatestVisitDate() { return latestVisitDate; }
    public void setLatestVisitDate(LocalDate value) { latestVisitDate = value; }
    public String getVisitDatesCsv() { return visitDatesCsv; }
    public void setVisitDatesCsv(String value) { visitDatesCsv = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public Long getFirstSourceBatchId() { return firstSourceBatchId; }
    public void setFirstSourceBatchId(Long value) { firstSourceBatchId = value; }
    public Long getLastSourceBatchId() { return lastSourceBatchId; }
    public void setLastSourceBatchId(Long value) { lastSourceBatchId = value; }
    public String getLastFollowResult() { return lastFollowResult; }
    public void setLastFollowResult(String value) { lastFollowResult = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String value) { closeReason = value; }
    public String getCloseReasonCode() { return closeReasonCode; }
    public void setCloseReasonCode(String value) { closeReasonCode = value; }
    public AudienceOpportunity getCurrentOpportunity() { return currentOpportunity; }
    public void setCurrentOpportunity(AudienceOpportunity value) { currentOpportunity = value; }
    public java.util.List<AudienceOpportunity> getOpportunities() { return opportunities; }
    public void setOpportunities(java.util.List<AudienceOpportunity> value) { opportunities = value; }
    public java.util.List<AudienceCustomerOrder> getOrders() { return orders; }
    public void setOrders(java.util.List<AudienceCustomerOrder> value) { orders = value; }
    public Date getLastContactAt() { return lastContactAt; }
    public void setLastContactAt(Date value) { lastContactAt = value; }
    public Date getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(Date value) { nextFollowAt = value; }
    public Date getStatusChangedAt() { return statusChangedAt; }
    public void setStatusChangedAt(Date value) { statusChangedAt = value; }
    public Date getQualifiedAt() { return qualifiedAt; }
    public void setQualifiedAt(Date value) { qualifiedAt = value; }
    public Date getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Date value) { firstSeenAt = value; }
    public Date getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Date value) { lastSeenAt = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String value) { updateBy = value; }
}
