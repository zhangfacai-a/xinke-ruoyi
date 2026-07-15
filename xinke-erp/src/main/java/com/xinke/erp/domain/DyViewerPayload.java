package com.xinke.erp.domain;

public class DyViewerPayload
{
    private String secUid;
    private String nickname;
    private String avatar;
    private String anchorId;
    private String roomId;
    private Integer gender;
    private Integer gradeLevel;
    private Integer fansClubLevel;
    private Long capturedAt;
    private String source;

    public String getSecUid() { return secUid; }
    public void setSecUid(String secUid) { this.secUid = secUid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getAnchorId() { return anchorId; }
    public void setAnchorId(String anchorId) { this.anchorId = anchorId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public Integer getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
    public Integer getFansClubLevel() { return fansClubLevel; }
    public void setFansClubLevel(Integer fansClubLevel) { this.fansClubLevel = fansClubLevel; }
    public Long getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Long capturedAt) { this.capturedAt = capturedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
