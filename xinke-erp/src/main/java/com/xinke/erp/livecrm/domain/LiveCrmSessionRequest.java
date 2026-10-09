package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LiveCrmSessionRequest {
    @NotBlank @Size(max = 128) private String roomName;
    @Size(max = 64) private String platformRoomId;
    @Size(max = 64) private String anchorId;
    private String liveDate;
    public String getRoomName(){return roomName;} public void setRoomName(String v){roomName=v;}
    public String getPlatformRoomId(){return platformRoomId;} public void setPlatformRoomId(String v){platformRoomId=v;}
    public String getAnchorId(){return anchorId;} public void setAnchorId(String v){anchorId=v;}
    public String getLiveDate(){return liveDate;} public void setLiveDate(String v){liveDate=v;}
}
