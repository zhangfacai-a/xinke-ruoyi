package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;

public class ErpPurchaseOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "采购ID")
    private Long purchaseId;

    @Excel(name = "采购单号")
    private String purchaseNo;

    @Excel(name = "供应商ID")
    private Long supplierId;

    @Excel(name = "供应商名称")
    private String supplierName;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "采购金额")
    private BigDecimal totalAmount;

    @Excel(name = "采购状态")
    private String purchaseStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预计到货时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expectedTime;

    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPurchaseStatus() { return purchaseStatus; }
    public void setPurchaseStatus(String purchaseStatus) { this.purchaseStatus = purchaseStatus; }
    public Date getExpectedTime() { return expectedTime; }
    public void setExpectedTime(Date expectedTime) { this.expectedTime = expectedTime; }
}
