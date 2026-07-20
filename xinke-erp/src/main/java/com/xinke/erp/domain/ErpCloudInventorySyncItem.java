package com.xinke.erp.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ErpCloudInventorySyncItem
{
    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 64, message = "SKU编码长度不能超过64")
    private String skuCode;

    @NotNull(message = "云仓可用库存不能为空")
    @PositiveOrZero(message = "云仓可用库存不能小于0")
    private Integer availableQty;

    @PositiveOrZero(message = "云仓锁定库存不能小于0")
    private Integer lockedQty;

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public Integer getAvailableQty() { return availableQty; }
    public void setAvailableQty(Integer availableQty) { this.availableQty = availableQty; }
    public Integer getLockedQty() { return lockedQty; }
    public void setLockedQty(Integer lockedQty) { this.lockedQty = lockedQty; }
}
