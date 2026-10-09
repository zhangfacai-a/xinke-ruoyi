package com.xinke.erp.livecrm.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LiveCrmOrderUpdateRequest {
    @NotBlank private String orderStatus;
    @Size(max=64) private String orderNo;
    @Size(max=128) private String productModel;
    private BigDecimal payAmount;
    public String getOrderStatus(){return orderStatus;} public void setOrderStatus(String value){orderStatus=value;}
    public String getOrderNo(){return orderNo;} public void setOrderNo(String value){orderNo=value;}
    public String getProductModel(){return productModel;} public void setProductModel(String value){productModel=value;}
    public BigDecimal getPayAmount(){return payAmount;} public void setPayAmount(BigDecimal value){payAmount=value;}
}
