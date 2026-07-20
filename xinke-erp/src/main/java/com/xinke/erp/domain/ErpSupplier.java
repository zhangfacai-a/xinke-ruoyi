package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ErpSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long supplierId;
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 64, message = "供应商编码长度不能超过64")
    @Excel(name = "供应商编码")
    private String supplierCode;
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 128, message = "供应商名称长度不能超过128")
    @Excel(name = "供应商名称")
    private String supplierName;
    private String contactName;
    private String contactPhone;
    private String settlementType;
    private Integer paymentDays;
    private String supplierLevel;
    private Integer leadTimeDays;
    private BigDecimal minOrderAmount;
    private BigDecimal qualityScore;
    private BigDecimal deliveryScore;
    private BigDecimal serviceScore;
    private String preferredFlag;
    private String status;
    private Integer purchaseOrderCount;
    private BigDecimal openPurchaseAmount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastPurchaseTime;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
    public Integer getPaymentDays() { return paymentDays; }
    public void setPaymentDays(Integer paymentDays) { this.paymentDays = paymentDays; }
    public String getSupplierLevel() { return supplierLevel; }
    public void setSupplierLevel(String supplierLevel) { this.supplierLevel = supplierLevel; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public BigDecimal getDeliveryScore() { return deliveryScore; }
    public void setDeliveryScore(BigDecimal deliveryScore) { this.deliveryScore = deliveryScore; }
    public BigDecimal getServiceScore() { return serviceScore; }
    public void setServiceScore(BigDecimal serviceScore) { this.serviceScore = serviceScore; }
    public String getPreferredFlag() { return preferredFlag; }
    public void setPreferredFlag(String preferredFlag) { this.preferredFlag = preferredFlag; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPurchaseOrderCount() { return purchaseOrderCount; }
    public void setPurchaseOrderCount(Integer purchaseOrderCount) { this.purchaseOrderCount = purchaseOrderCount; }
    public BigDecimal getOpenPurchaseAmount() { return openPurchaseAmount; }
    public void setOpenPurchaseAmount(BigDecimal openPurchaseAmount) { this.openPurchaseAmount = openPurchaseAmount; }
    public Date getLastPurchaseTime() { return lastPurchaseTime; }
    public void setLastPurchaseTime(Date lastPurchaseTime) { this.lastPurchaseTime = lastPurchaseTime; }
}
