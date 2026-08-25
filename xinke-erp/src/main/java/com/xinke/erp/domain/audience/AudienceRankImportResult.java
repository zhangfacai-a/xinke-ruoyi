package com.xinke.erp.domain.audience;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceRankImportResult
{
    private Long batchId;
    private boolean duplicate;
    private String payloadHash;
    private String roomName;
    private Long roomId;
    private String matchedRoomName;
    private String roomMatchStatus;
    private Integer commentRowCount;
    private Integer watchRowCount;
    private Integer uniqueUserCount;
    private Integer newCustomerCount;
    private Integer updatedCustomerCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date capturedAt;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getMatchedRoomName() { return matchedRoomName; }
    public void setMatchedRoomName(String matchedRoomName) { this.matchedRoomName = matchedRoomName; }
    public String getRoomMatchStatus() { return roomMatchStatus; }
    public void setRoomMatchStatus(String roomMatchStatus) { this.roomMatchStatus = roomMatchStatus; }
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
    public Date getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Date capturedAt) { this.capturedAt = capturedAt; }
}
