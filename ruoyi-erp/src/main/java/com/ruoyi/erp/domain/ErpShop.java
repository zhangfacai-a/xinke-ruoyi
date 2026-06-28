package com.ruoyi.erp.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

public class ErpShop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "店铺ID")
    private Long shopId;

    @Excel(name = "店铺编码")
    private String shopCode;

    @Excel(name = "店铺名称")
    private String shopName;

    @Excel(name = "平台编码")
    private String platformCode;

    @Excel(name = "平台名称")
    private String platformName;

    @Excel(name = "负责人")
    private String ownerName;

    @Excel(name = "状态")
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopCode() { return shopCode; }
    public void setShopCode(String shopCode) { this.shopCode = shopCode; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
