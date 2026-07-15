package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;

public class ErpInventoryMovement extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "流水ID")
    private Long movementId;

    @Excel(name = "流水号")
    private String movementNo;

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

    @Excel(name = "流水类型")
    private String movementType;

    @Excel(name = "来源类型")
    private String sourceType;

    @Excel(name = "来源单号")
    private String sourceNo;

    @Excel(name = "数量")
    private Integer quantity;

    @Excel(name = "变动前库存")
    private Integer beforeQty;

    @Excel(name = "变动后库存")
    private Integer afterQty;

    @Excel(name = "成本价")
    private BigDecimal costPrice;

    @Excel(name = "操作人")
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "流水时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date movementTime;

    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }
    public String getMovementNo() { return movementNo; }
    public void setMovementNo(String movementNo) { this.movementNo = movementNo; }
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
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getBeforeQty() { return beforeQty; }
    public void setBeforeQty(Integer beforeQty) { this.beforeQty = beforeQty; }
    public Integer getAfterQty() { return afterQty; }
    public void setAfterQty(Integer afterQty) { this.afterQty = afterQty; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getMovementTime() { return movementTime; }
    public void setMovementTime(Date movementTime) { this.movementTime = movementTime; }
}
