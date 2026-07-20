package com.xinke.erp.domain;

import java.io.Serializable;

public class ErpPurchaseReceiveItem implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long itemId;
    private Integer quantity;
    private String batchNo;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
}
