package com.xinke.erp.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RpaTrackingConfigRequest
{
    @NotNull(message = "enabled不能为空")
    private Boolean enabled;

    @NotNull(message = "lookbackDays不能为空")
    @Min(value = 1, message = "lookbackDays不能小于1")
    @Max(value = 365, message = "lookbackDays不能大于365")
    private Integer lookbackDays;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(Integer lookbackDays) { this.lookbackDays = lookbackDays; }
}
