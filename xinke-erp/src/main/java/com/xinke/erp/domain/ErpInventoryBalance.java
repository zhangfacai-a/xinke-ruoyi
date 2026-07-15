package com.xinke.erp.domain;

import java.math.BigDecimal;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;

public class ErpInventoryBalance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "库存ID")
    private Long balanceId;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "SKU ID")
    private Long skuId;

    @Excel(name = "SKU编码")
    private String skuCode;

    @Excel(name = "SKU名称")
    private String skuName;

    @Excel(name = "可用库存")
    private Integer availableQty;

    @Excel(name = "锁定库存")
    private Integer lockedQty;

    @Excel(name = "在途库存")
    private Integer inboundQty;

    @Excel(name = "残次库存")
    private Integer defectiveQty;

    @Excel(name = "安全库存")
    private Integer safetyQty;

    @Excel(name = "成本价")
    private BigDecimal costPrice;

    public Long getBalanceId() { return balanceId; }
    public void setBalanceId(Long balanceId) { this.balanceId = balanceId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Integer getAvailableQty() { return availableQty; }
    public void setAvailableQty(Integer availableQty) { this.availableQty = availableQty; }
    public Integer getLockedQty() { return lockedQty; }
    public void setLockedQty(Integer lockedQty) { this.lockedQty = lockedQty; }
    public Integer getInboundQty() { return inboundQty; }
    public void setInboundQty(Integer inboundQty) { this.inboundQty = inboundQty; }
    public Integer getDefectiveQty() { return defectiveQty; }
    public void setDefectiveQty(Integer defectiveQty) { this.defectiveQty = defectiveQty; }
    public Integer getSafetyQty() { return safetyQty; }
    public void setSafetyQty(Integer safetyQty) { this.safetyQty = safetyQty; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
}
