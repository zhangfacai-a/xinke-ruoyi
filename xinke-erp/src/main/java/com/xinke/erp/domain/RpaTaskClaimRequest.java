package com.xinke.erp.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RpaTaskClaimRequest
{
    @NotBlank(message = "workerId不能为空")
    @Size(max = 128, message = "workerId长度不能超过128")
    private String workerId;

    @Min(value = 1, message = "limit不能小于1")
    @Max(value = 10, message = "limit不能大于10")
    private Integer limit;

    @Size(max = 64, message = "preferredShopCode长度不能超过64")
    private String preferredShopCode;

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public String getPreferredShopCode() { return preferredShopCode; }
    public void setPreferredShopCode(String preferredShopCode) { this.preferredShopCode = preferredShopCode; }
}
