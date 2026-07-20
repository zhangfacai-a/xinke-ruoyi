package com.xinke.erp.domain;

import java.io.Serializable;
import java.math.BigDecimal;

public class ErpPurchaseOrderItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Long purchaseId;
    private String purchaseNo;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private Integer receivedQty;
    private BigDecimal purchasePrice;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal taxInclusiveAmount;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getReceivedQty() { return receivedQty; }
    public void setReceivedQty(Integer receivedQty) { this.receivedQty = receivedQty; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTaxInclusiveAmount() { return taxInclusiveAmount; }
    public void setTaxInclusiveAmount(BigDecimal taxInclusiveAmount) { this.taxInclusiveAmount = taxInclusiveAmount; }
}
