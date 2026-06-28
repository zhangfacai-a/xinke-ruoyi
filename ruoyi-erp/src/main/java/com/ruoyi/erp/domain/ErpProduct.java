package com.ruoyi.erp.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

public class ErpProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "Product ID")
    private Long productId;

    @Excel(name = "Product Code")
    private String productCode;

    @Excel(name = "Product Name")
    private String productName;

    @Excel(name = "Category")
    private String categoryName;

    @Excel(name = "Brand")
    private String brandName;

    @Excel(name = "Cost Price")
    private BigDecimal costPrice;

    @Excel(name = "Sale Price")
    private BigDecimal salePrice;

    @Excel(name = "Status")
    private String status;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
