package com.xinke.erp.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ErpAfterSaleCreateRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单商品不能为空")
    private Long salesItemId;

    @NotBlank(message = "售后类型不能为空")
    private String afterSaleType;

    @NotNull(message = "售后数量不能为空")
    @Positive(message = "售后数量必须大于0")
    private Integer quantity;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.00", message = "退款金额不能小于0")
    private BigDecimal refundAmount;

    private Long returnWarehouseId;

    @NotBlank(message = "售后原因不能为空")
    @Size(max = 255, message = "售后原因不能超过255个字符")
    private String reason;

    @Size(max = 64, message = "幂等标识不能超过64个字符")
    private String idempotencyKey;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getSalesItemId() { return salesItemId; }
    public void setSalesItemId(Long salesItemId) { this.salesItemId = salesItemId; }
    public String getAfterSaleType() { return afterSaleType; }
    public void setAfterSaleType(String afterSaleType) { this.afterSaleType = afterSaleType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public Long getReturnWarehouseId() { return returnWarehouseId; }
    public void setReturnWarehouseId(Long returnWarehouseId) { this.returnWarehouseId = returnWarehouseId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
