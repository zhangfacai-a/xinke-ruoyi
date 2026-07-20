package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ErpPurchaseOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long purchaseId;

    @NotBlank(message = "采购单号不能为空")
    @Size(max = 64, message = "采购单号长度不能超过64")
    @Excel(name = "采购单号")
    private String purchaseNo;

    @NotNull(message = "请选择供应商")
    private Long supplierId;

    @Excel(name = "供应商")
    private String supplierName;

    @NotNull(message = "请选择采购入库仓")
    private Long warehouseId;

    @Excel(name = "入库仓")
    private String warehouseName;

    private String warehouseType;

    @Excel(name = "未税金额")
    private BigDecimal totalAmount;

    @Excel(name = "税额")
    private BigDecimal taxAmount;

    @Excel(name = "价税合计")
    private BigDecimal totalWithTax;

    @Excel(name = "采购状态")
    private String purchaseStatus;

    @NotNull(message = "请选择采购日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "采购日期", width = 20, dateFormat = "yyyy-MM-dd")
    private Date purchaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预计到货时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expectedTime;

    private String submitBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    private String approveBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
    private String closeBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    private Integer itemCount;
    private Integer totalQuantity;
    private Integer receivedQuantity;
    private BigDecimal receiveProgress;
    private Boolean overdue;
    private List<ErpPurchaseOrderItem> items;

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
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseType() { return warehouseType; }
    public void setWarehouseType(String warehouseType) { this.warehouseType = warehouseType; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalWithTax() { return totalWithTax; }
    public void setTotalWithTax(BigDecimal totalWithTax) { this.totalWithTax = totalWithTax; }
    public String getPurchaseStatus() { return purchaseStatus; }
    public void setPurchaseStatus(String purchaseStatus) { this.purchaseStatus = purchaseStatus; }
    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }
    public Date getExpectedTime() { return expectedTime; }
    public void setExpectedTime(Date expectedTime) { this.expectedTime = expectedTime; }
    public String getSubmitBy() { return submitBy; }
    public void setSubmitBy(String submitBy) { this.submitBy = submitBy; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }
    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }
    public String getCloseBy() { return closeBy; }
    public void setCloseBy(String closeBy) { this.closeBy = closeBy; }
    public Date getCloseTime() { return closeTime; }
    public void setCloseTime(Date closeTime) { this.closeTime = closeTime; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public BigDecimal getReceiveProgress() { return receiveProgress; }
    public void setReceiveProgress(BigDecimal receiveProgress) { this.receiveProgress = receiveProgress; }
    public Boolean getOverdue() { return overdue; }
    public void setOverdue(Boolean overdue) { this.overdue = overdue; }
    public List<ErpPurchaseOrderItem> getItems() { return items; }
    public void setItems(List<ErpPurchaseOrderItem> items) { this.items = items; }
}
