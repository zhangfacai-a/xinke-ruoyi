package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;

public class ErpOrderItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long orderItemId;
    @Excel(name = "Order ID")
    private Long orderId;
    private Long shopId;
    private String shopName;
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuName;
    private Integer quantity;
    private BigDecimal payAmount;
    private BigDecimal productCost;
    private BigDecimal platformFee;
    private BigDecimal adCost;
    private BigDecimal freightFee;
    private BigDecimal refundCost;
    private BigDecimal profitAmount;
    private Integer orderStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dt;

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public BigDecimal getProductCost() { return productCost; }
    public void setProductCost(BigDecimal productCost) { this.productCost = productCost; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getAdCost() { return adCost; }
    public void setAdCost(BigDecimal adCost) { this.adCost = adCost; }
    public BigDecimal getFreightFee() { return freightFee; }
    public void setFreightFee(BigDecimal freightFee) { this.freightFee = freightFee; }
    public BigDecimal getRefundCost() { return refundCost; }
    public void setRefundCost(BigDecimal refundCost) { this.refundCost = refundCost; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }
    public Date getDt() { return dt; }
    public void setDt(Date dt) { this.dt = dt; }
}
