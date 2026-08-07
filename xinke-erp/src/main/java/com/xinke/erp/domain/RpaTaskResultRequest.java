package com.xinke.erp.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RpaTaskResultRequest extends RpaBatchRequest
{
    @NotBlank(message = "requestId不能为空")
    @Size(max = 64, message = "requestId长度不能超过64")
    private String requestId;

    @NotBlank(message = "taskNo不能为空")
    @Size(max = 64, message = "taskNo长度不能超过64")
    private String taskNo;

    @NotBlank(message = "outcome不能为空")
    @Size(max = 32, message = "outcome长度不能超过32")
    private String outcome;

    @Size(max = 128, message = "douyinNo长度不能超过128")
    private String douyinNo;

    @Size(max = 128, message = "orderNo长度不能超过128")
    private String orderNo;

    private Boolean followed;
    private Boolean messaged;

    @Size(max = 1000, message = "messageContent长度不能超过1000")
    private String messageContent;

    @Size(max = 64, message = "resultCode长度不能超过64")
    private String resultCode;

    @Size(max = 1000, message = "errorMessage长度不能超过1000")
    private String errorMessage;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getDouyinNo() { return douyinNo; }
    public void setDouyinNo(String douyinNo) { this.douyinNo = douyinNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }
    public Boolean getMessaged() { return messaged; }
    public void setMessaged(Boolean messaged) { this.messaged = messaged; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
