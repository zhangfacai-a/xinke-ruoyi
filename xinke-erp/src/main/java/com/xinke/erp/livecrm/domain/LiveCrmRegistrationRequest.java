package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LiveCrmRegistrationRequest {
    private Long customerId;
    @NotBlank private String nickname;
    private Long sessionId;
    /** Optional room context supplied by the center when today's session has
     * not been created yet.  The service creates/reuses the session only when
     * the registration is saved; merely selecting a room never writes data. */
    @Size(max=128) private String roomName;
    @Size(max=64) private String platformRoomId;
    @Size(max=32) private String liveDate;
    private String secUid;
    @Size(max=128) private String douyinAccount;
    @Size(max=32) private String phone;
    @Size(max=128) private String wechat;
    private String interactionType;
    @NotBlank private String stage;
    private String addFanStatus;
    private Long ownerUserId;
    private String ownerName;
    private Long anchorUserId;
    private String anchorName;
    private Long controllerUserId;
    private String controllerName;
    @Size(max=128) private String consultModel;
    @Size(max=500) private String concern;
    @Size(max=500) private String specialMark;
    @Size(max=1000) private String note;
    @Size(max=64) private String orderNo;
    @Size(max=32) private String nextTaskDate;
    public String getNickname(){return nickname;} public void setNickname(String v){nickname=v;}
    public Long getCustomerId(){return customerId;} public void setCustomerId(Long v){customerId=v;}
    public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public String getRoomName(){return roomName;} public void setRoomName(String v){roomName=v;}
    public String getPlatformRoomId(){return platformRoomId;} public void setPlatformRoomId(String v){platformRoomId=v;}
    public String getLiveDate(){return liveDate;} public void setLiveDate(String v){liveDate=v;}
    public String getSecUid(){return secUid;} public void setSecUid(String v){secUid=v;}
    public String getDouyinAccount(){return douyinAccount;} public void setDouyinAccount(String v){douyinAccount=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getWechat(){return wechat;} public void setWechat(String v){wechat=v;}
    public String getInteractionType(){return interactionType;} public void setInteractionType(String v){interactionType=v;}
    public String getStage(){return stage;} public void setStage(String v){stage=v;}
    public String getAddFanStatus(){return addFanStatus;} public void setAddFanStatus(String v){addFanStatus=v;}
    public Long getOwnerUserId(){return ownerUserId;} public void setOwnerUserId(Long v){ownerUserId=v;}
    public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;}
    public Long getAnchorUserId(){return anchorUserId;} public void setAnchorUserId(Long v){anchorUserId=v;}
    public String getAnchorName(){return anchorName;} public void setAnchorName(String v){anchorName=v;}
    public Long getControllerUserId(){return controllerUserId;} public void setControllerUserId(Long v){controllerUserId=v;}
    public String getControllerName(){return controllerName;} public void setControllerName(String v){controllerName=v;}
    public String getConsultModel(){return consultModel;} public void setConsultModel(String v){consultModel=v;}
    public String getConcern(){return concern;} public void setConcern(String v){concern=v;}
    public String getSpecialMark(){return specialMark;} public void setSpecialMark(String v){specialMark=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
    public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public String getNextTaskDate(){return nextTaskDate;} public void setNextTaskDate(String v){nextTaskDate=v;}
}
