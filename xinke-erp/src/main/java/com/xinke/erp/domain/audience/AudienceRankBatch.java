package com.xinke.erp.domain.audience;

import java.time.LocalDate;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceRankBatch
{
    private Long batchId;
    private String payloadHash;
    private String roomScopeKey;
    private String roomName;
    private Long roomId;
    private String matchedRoomName;
    private String roomMatchStatus;
    private Boolean isCurrent;
    private LocalDate commentDataDate;
    private LocalDate watchDataDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date capturedAt;

    private Integer commentRowCount;
    private Integer watchRowCount;
    private Integer uniqueUserCount;
    private Integer newCustomerCount;
    private Integer updatedCustomerCount;
    private Integer anchorCount;
    private Integer controllerCount;
    private String uploadedIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getRoomScopeKey() { return roomScopeKey; }
    public void setRoomScopeKey(String roomScopeKey) { this.roomScopeKey = roomScopeKey; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getMatchedRoomName() { return matchedRoomName; }
    public void setMatchedRoomName(String matchedRoomName) { this.matchedRoomName = matchedRoomName; }
    public String getRoomMatchStatus() { return roomMatchStatus; }
    public void setRoomMatchStatus(String roomMatchStatus) { this.roomMatchStatus = roomMatchStatus; }
    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    public LocalDate getCommentDataDate() { return commentDataDate; }
    public void setCommentDataDate(LocalDate commentDataDate) { this.commentDataDate = commentDataDate; }
    public LocalDate getWatchDataDate() { return watchDataDate; }
    public void setWatchDataDate(LocalDate watchDataDate) { this.watchDataDate = watchDataDate; }
    public Date getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Date capturedAt) { this.capturedAt = capturedAt; }
    public Integer getCommentRowCount() { return commentRowCount; }
    public void setCommentRowCount(Integer commentRowCount) { this.commentRowCount = commentRowCount; }
    public Integer getWatchRowCount() { return watchRowCount; }
    public void setWatchRowCount(Integer watchRowCount) { this.watchRowCount = watchRowCount; }
    public Integer getUniqueUserCount() { return uniqueUserCount; }
    public void setUniqueUserCount(Integer uniqueUserCount) { this.uniqueUserCount = uniqueUserCount; }
    public Integer getNewCustomerCount() { return newCustomerCount; }
    public void setNewCustomerCount(Integer value) { newCustomerCount = value; }
    public Integer getUpdatedCustomerCount() { return updatedCustomerCount; }
    public void setUpdatedCustomerCount(Integer value) { updatedCustomerCount = value; }
    public Integer getAnchorCount() { return anchorCount; }
    public void setAnchorCount(Integer value) { anchorCount = value; }
    public Integer getControllerCount() { return controllerCount; }
    public void setControllerCount(Integer value) { controllerCount = value; }
    public String getUploadedIp() { return uploadedIp; }
    public void setUploadedIp(String uploadedIp) { this.uploadedIp = uploadedIp; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
