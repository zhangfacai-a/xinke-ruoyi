package com.xinke.erp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;

public class LiveGiftBatchSaveRequest
{
    @NotEmpty(message = "请至少输入一个订单号")
    private List<String> orderNos = new ArrayList<>();
    private Long dailyId;
    @NotEmpty(message = "请至少选择一个礼品")
    private List<Map<String, Object>> gifts = new ArrayList<>();
    private String operatorNote;

    public List<String> getOrderNos() { return orderNos; }
    public void setOrderNos(List<String> orderNos) { this.orderNos = orderNos; }
    public Long getDailyId() { return dailyId; }
    public void setDailyId(Long dailyId) { this.dailyId = dailyId; }
    public List<Map<String, Object>> getGifts() { return gifts; }
    public void setGifts(List<Map<String, Object>> gifts) { this.gifts = gifts; }
    public String getOperatorNote() { return operatorNote; }
    public void setOperatorNote(String operatorNote) { this.operatorNote = operatorNote; }
}
