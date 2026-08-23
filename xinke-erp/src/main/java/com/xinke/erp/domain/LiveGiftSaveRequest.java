package com.xinke.erp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LiveGiftSaveRequest
{
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    @NotBlank(message = "请选择礼品处理状态")
    private String processStatus;
    private Long dailyId;
    private Long roomId;
    private String roomCodeSnapshot;
    private String roomNameSnapshot;
    private String operatorNote;
    private Long anchorUserId;
    private String anchorNameSnapshot;
    private Long controllerUserId;
    private String controllerNameSnapshot;
    private BigDecimal refundAmount;
    private String refundReason;
    private String otherRemark;
    private String afterSaleCompensation;
    private String serviceMark;
    private Boolean extendedWarranty;
    private Boolean priceProtection;
    private Boolean delayed;
    private Boolean followUp;
    private Boolean urgent;
    private Long templateId;
    private String templateNameSnapshot;
    private String parsedText;
    @NotNull(message = "礼品明细不能为空")
    private List<Map<String, Object>> gifts = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public Long getDailyId() { return dailyId; }
    public void setDailyId(Long dailyId) { this.dailyId = dailyId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomCodeSnapshot() { return roomCodeSnapshot; }
    public void setRoomCodeSnapshot(String roomCodeSnapshot) { this.roomCodeSnapshot = roomCodeSnapshot; }
    public String getRoomNameSnapshot() { return roomNameSnapshot; }
    public void setRoomNameSnapshot(String roomNameSnapshot) { this.roomNameSnapshot = roomNameSnapshot; }
    public String getOperatorNote() { return operatorNote; }
    public void setOperatorNote(String operatorNote) { this.operatorNote = operatorNote; }
    public List<Map<String, Object>> getGifts() { return gifts; }
    public void setGifts(List<Map<String, Object>> gifts) { this.gifts = gifts; }
    public Long getAnchorUserId() { return anchorUserId; }
    public void setAnchorUserId(Long anchorUserId) { this.anchorUserId = anchorUserId; }
    public String getAnchorNameSnapshot() { return anchorNameSnapshot; }
    public void setAnchorNameSnapshot(String anchorNameSnapshot) { this.anchorNameSnapshot = anchorNameSnapshot; }
    public Long getControllerUserId() { return controllerUserId; }
    public void setControllerUserId(Long controllerUserId) { this.controllerUserId = controllerUserId; }
    public String getControllerNameSnapshot() { return controllerNameSnapshot; }
    public void setControllerNameSnapshot(String controllerNameSnapshot) { this.controllerNameSnapshot = controllerNameSnapshot; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getOtherRemark() { return otherRemark; }
    public void setOtherRemark(String otherRemark) { this.otherRemark = otherRemark; }
    public String getAfterSaleCompensation() { return afterSaleCompensation; }
    public void setAfterSaleCompensation(String afterSaleCompensation) { this.afterSaleCompensation = afterSaleCompensation; }
    public String getServiceMark() { return serviceMark; }
    public void setServiceMark(String serviceMark) { this.serviceMark = serviceMark; }
    public Boolean getExtendedWarranty() { return extendedWarranty; }
    public void setExtendedWarranty(Boolean extendedWarranty) { this.extendedWarranty = extendedWarranty; }
    public Boolean getPriceProtection() { return priceProtection; }
    public void setPriceProtection(Boolean priceProtection) { this.priceProtection = priceProtection; }
    public Boolean getDelayed() { return delayed; }
    public void setDelayed(Boolean delayed) { this.delayed = delayed; }
    public Boolean getFollowUp() { return followUp; }
    public void setFollowUp(Boolean followUp) { this.followUp = followUp; }
    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateNameSnapshot() { return templateNameSnapshot; }
    public void setTemplateNameSnapshot(String templateNameSnapshot) { this.templateNameSnapshot = templateNameSnapshot; }
    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }
}
