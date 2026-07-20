package com.xinke.erp.domain;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ErpFulfillmentQuantity implements Serializable
{
    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单商品不能为空")
    private Long salesItemId;

    @NotNull(message = "操作数量不能为空")
    @Positive(message = "操作数量必须大于0")
    private Integer quantity;

    public Long getSalesItemId() { return salesItemId; }
    public void setSalesItemId(Long salesItemId) { this.salesItemId = salesItemId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
