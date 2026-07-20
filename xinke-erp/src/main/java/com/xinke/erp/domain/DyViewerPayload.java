package com.xinke.erp.domain;

import jakarta.validation.constraints.Size;

public class DyViewerPayload
{
    @Size(max = 256, message = "secUid长度不能超过256")
    private String secUid;
    @Size(max = 128, message = "昵称长度不能超过128")
    private String nickname;
    @Size(max = 500, message = "头像地址长度不能超过500")
    private String avatar;
    @Size(max = 128, message = "anchorId长度不能超过128")
    private String anchorId;
    @Size(max = 128, message = "roomId长度不能超过128")
    private String roomId;
    private Integer gender;
    private Integer gradeLevel;
    private Integer fansClubLevel;
    private Long capturedAt;
    @Size(max = 64, message = "观众来源长度不能超过64")
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
