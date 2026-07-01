package com.ruoyi.erp.domain;

import java.util.List;

public class DyCaptureReport
{
    private String batchNo;
    private String roomKey;
    private String userId;
    private String source;
    private String payloadType;
    private String liveRoomName;
    private String pageUrl;
    private String requestUrl;
    private Integer queueSize;
    private Long clientTime;
    private List<DyViewerCommentPayload> comments;
    private List<DyViewerPayload> audiences;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getRoomKey() { return roomKey; }
    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }
    public String getLiveRoomName() { return liveRoomName; }
    public void setLiveRoomName(String liveRoomName) { this.liveRoomName = liveRoomName; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public Integer getQueueSize() { return queueSize; }
    public void setQueueSize(Integer queueSize) { this.queueSize = queueSize; }
    public Long getClientTime() { return clientTime; }
    public void setClientTime(Long clientTime) { this.clientTime = clientTime; }
    public List<DyViewerCommentPayload> getComments() { return comments; }
    public void setComments(List<DyViewerCommentPayload> comments) { this.comments = comments; }
    public List<DyViewerPayload> getAudiences() { return audiences; }
    public void setAudiences(List<DyViewerPayload> audiences) { this.audiences = audiences; }
}
