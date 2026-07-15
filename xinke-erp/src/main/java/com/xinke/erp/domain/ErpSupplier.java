package com.xinke.erp.domain;

import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;

public class ErpSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "供应商ID")
    private Long supplierId;

    @Excel(name = "供应商编码")
    private String supplierCode;

    @Excel(name = "供应商名称")
    private String supplierName;

    @Excel(name = "联系人")
    private String contactName;

    @Excel(name = "联系电话")
    private String contactPhone;

    @Excel(name = "结算方式")
    private String settlementType;

    @Excel(name = "账期天数")
    private Integer paymentDays;

    @Excel(name = "状态")
    private String status;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
