package com.xinke.erp.domain.audience;

import java.util.Date;

public class AudienceRankProfile
{
    private Long profileId;
    private String roomScopeKey;
    private String roomName;
    private Long roomId;
    private String secUid;
    private String nickname;
    private Boolean isFollower;
    private Boolean isFollowing;
    private Integer payLevel;
    private String payIconUrl;
    private Long firstBatchId;
    private Long lastBatchId;
    private Date firstSeenAt;
    private Date lastSeenAt;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
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
    public Long getFirstBatchId() { return firstBatchId; }
    public void setFirstBatchId(Long firstBatchId) { this.firstBatchId = firstBatchId; }
    public Long getLastBatchId() { return lastBatchId; }
    public void setLastBatchId(Long lastBatchId) { this.lastBatchId = lastBatchId; }
    public Date getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Date firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Date getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Date lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
