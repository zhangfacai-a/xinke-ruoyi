package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DyCaptureReport
{
    @Size(max = 128, message = "batchNo长度不能超过128")
    private String batchNo;
    @Size(max = 128, message = "roomKey长度不能超过128")
    private String roomKey;
    @Size(max = 128, message = "userId长度不能超过128")
    private String userId;
    @Size(max = 64, message = "source长度不能超过64")
    private String source;
    @NotBlank(message = "payloadType不能为空")
    @Size(max = 32, message = "payloadType长度不能超过32")
    private String payloadType;
    @Size(max = 128, message = "直播间名称长度不能超过128")
    private String liveRoomName;
    @Size(max = 500, message = "pageUrl长度不能超过500")
    private String pageUrl;
    @Size(max = 1000, message = "requestUrl长度不能超过1000")
    private String requestUrl;
    @NotBlank(message = "插件版本不能为空")
    @Size(max = 32, message = "插件版本长度不能超过32")
    private String pluginVersion;
    @NotBlank(message = "电脑编号不能为空")
    @Size(max = 128, message = "电脑编号长度不能超过128")
    private String clientId;
    @Size(max = 128, message = "电脑名称长度不能超过128")
    private String clientName;
    private Integer queueSize;
    private Long clientTime;
    @Valid
    @Size(max = 500, message = "单次评论上报不能超过500条")
    private List<DyViewerCommentPayload> comments;
    @Valid
    @Size(max = 500, message = "单次观众上报不能超过500条")
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
    public String getPluginVersion() { return pluginVersion; }
    public void setPluginVersion(String pluginVersion) { this.pluginVersion = pluginVersion; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Integer getQueueSize() { return queueSize; }
    public void setQueueSize(Integer queueSize) { this.queueSize = queueSize; }
    public Long getClientTime() { return clientTime; }
    public void setClientTime(Long clientTime) { this.clientTime = clientTime; }
    public List<DyViewerCommentPayload> getComments() { return comments; }
    public void setComments(List<DyViewerCommentPayload> comments) { this.comments = comments; }
    public List<DyViewerPayload> getAudiences() { return audiences; }
    public void setAudiences(List<DyViewerPayload> audiences) { this.audiences = audiences; }
}
