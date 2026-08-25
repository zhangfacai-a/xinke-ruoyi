package com.xinke.erp.domain.audience;

public class AudienceRankRoomMatch
{
    private Long roomId;
    private String roomCode;
    private String roomName;
    private String liveAccount;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getLiveAccount() { return liveAccount; }
    public void setLiveAccount(String liveAccount) { this.liveAccount = liveAccount; }
}
