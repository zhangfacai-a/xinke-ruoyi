package com.ruoyi.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class FinanceDayProfit
{
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dt;
    private Long shopId;
    private String shopName;
    private BigDecimal gmv;
    private BigDecimal netAmount;
    private BigDecimal productCost;
    private BigDecimal platformFee;
    private BigDecimal adCost;
    private BigDecimal freightFee;
    private BigDecimal afterSaleCost;
    private BigDecimal profitAmount;
    private BigDecimal roi;

    public Date getDt() { return dt; }
    public void setDt(Date dt) { this.dt = dt; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public BigDecimal getGmv() { return gmv; }
    public void setGmv(BigDecimal gmv) { this.gmv = gmv; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getProductCost() { return productCost; }
    public void setProductCost(BigDecimal productCost) { this.productCost = productCost; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getAdCost() { return adCost; }
    public void setAdCost(BigDecimal adCost) { this.adCost = adCost; }
    public BigDecimal getFreightFee() { return freightFee; }
    public void setFreightFee(BigDecimal freightFee) { this.freightFee = freightFee; }
    public BigDecimal getAfterSaleCost() { return afterSaleCost; }
    public void setAfterSaleCost(BigDecimal afterSaleCost) { this.afterSaleCost = afterSaleCost; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public BigDecimal getRoi() { return roi; }
    public void setRoi(BigDecimal roi) { this.roi = roi; }
}
