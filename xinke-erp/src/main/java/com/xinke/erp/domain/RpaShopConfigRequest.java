package com.xinke.erp.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RpaShopConfigRequest
{
    @NotBlank(message = "shopCode不能为空")
    @Size(max = 64, message = "shopCode长度不能超过64")
    private String shopCode;

    @NotBlank(message = "shopName不能为空")
    @Size(max = 100, message = "shopName长度不能超过100")
    private String shopName;

    @NotBlank(message = "douyinAccountCode不能为空")
    @Size(max = 128, message = "douyinAccountCode长度不能超过128")
    private String douyinAccountCode;

    @NotBlank(message = "douyinShopName不能为空")
    @Size(max = 128, message = "douyinShopName长度不能超过128")
    private String douyinShopName;

    @NotBlank(message = "messageTemplate不能为空")
    @Size(max = 1000, message = "messageTemplate长度不能超过1000")
    private String messageTemplate;

    @Min(value = 1, message = "dailyLimit不能小于1")
    @Max(value = 10000, message = "dailyLimit不能大于10000")
    private Integer dailyLimit;

    @Size(max = 1, message = "status长度不能超过1")
    @Pattern(regexp = "[01]", message = "status只能是0或1")
    private String status;

    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;

    public String getShopCode() { return shopCode; }
    public void setShopCode(String shopCode) { this.shopCode = shopCode; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getDouyinAccountCode() { return douyinAccountCode; }
    public void setDouyinAccountCode(String douyinAccountCode) { this.douyinAccountCode = douyinAccountCode; }
    public String getDouyinShopName() { return douyinShopName; }
    public void setDouyinShopName(String douyinShopName) { this.douyinShopName = douyinShopName; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
