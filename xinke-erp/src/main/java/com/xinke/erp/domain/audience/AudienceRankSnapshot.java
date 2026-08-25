package com.xinke.erp.domain.audience;

import java.time.LocalDate;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceRankSnapshot
{
    private Long snapshotId;
    private Long followupId;
    private Long batchId;
    private String roomScopeKey;
    private String roomName;
    private Long roomId;
    private String secUid;
    private String nickname;
    private Boolean isFollower;
    private Boolean isFollowing;
    private Integer payLevel;
    private String payIconUrl;
    private Long commentCount;
    private Integer commentRank;
    private Long watchSeconds;
    private Integer watchRank;
    private LocalDate commentDataDate;
    private LocalDate watchDataDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date capturedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getFollowupId() { return followupId; }
    public void setFollowupId(Long followupId) { this.followupId = followupId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getRoomScopeKey() { return roomScopeKey; }
    public void setRoomScopeKey(String roomScopeKey) { this.roomScopeKey = roomScopeKey; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getSecUid() { return secUid; }
    public void setSecUid(String secUid) { this.secUid = secUid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Boolean getIsFollower() { return isFollower; }
    public void setIsFollower(Boolean follower) { isFollower = follower; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean following) { isFollowing = following; }
    public Integer getPayLevel() { return payLevel; }
    public void setPayLevel(Integer payLevel) { this.payLevel = payLevel; }
    public String getPayIconUrl() { return payIconUrl; }
    public void setPayIconUrl(String payIconUrl) { this.payIconUrl = payIconUrl; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    public Integer getCommentRank() { return commentRank; }
    public void setCommentRank(Integer commentRank) { this.commentRank = commentRank; }
    public Long getWatchSeconds() { return watchSeconds; }
    public void setWatchSeconds(Long watchSeconds) { this.watchSeconds = watchSeconds; }
    public Integer getWatchRank() { return watchRank; }
    public void setWatchRank(Integer watchRank) { this.watchRank = watchRank; }
    public LocalDate getCommentDataDate() { return commentDataDate; }
    public void setCommentDataDate(LocalDate commentDataDate) { this.commentDataDate = commentDataDate; }
    public LocalDate getWatchDataDate() { return watchDataDate; }
    public void setWatchDataDate(LocalDate watchDataDate) { this.watchDataDate = watchDataDate; }
    public Date getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Date capturedAt) { this.capturedAt = capturedAt; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
