package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RpaShopConfigRequest
{
    @Size(max = 64) private String shopCode;
    @NotBlank @Size(max = 100) private String shopName;
    @NotBlank @Size(max = 128) private String douyinAccountCode;
    @Size(max = 128) private String douyinShopName;
    @Size(max = 1000) private String messageTemplate;
    @Min(1) @Max(10000) private Integer dailyLimit;
    @Size(max = 1) @Pattern(regexp = "[01]") private String status;
    @Size(max = 500) private String remark;
    @Min(1) @Max(1000) private Integer hourlyLimit;
    @Min(1) @Max(100) private Integer burstSize;
    @Min(1) @Max(120) private Integer restMinutes;
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") private String allowedStartTime;
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") private String allowedEndTime;
    @Min(1) @Max(3650) private Integer refundCooldownDays;
    @Min(1) @Max(365) private Integer cancelledCooldownDays;
    private Boolean pauseOnCaptcha;
    @Min(1) @Max(100) private Integer maxConsecutiveFailures;
    @Valid private List<RpaMessageTemplateRequest> messageTemplates;

    public String getShopCode() { return shopCode; }
    public void setShopCode(String value) { shopCode = value; }
    public String getShopName() { return shopName; }
    public void setShopName(String value) { shopName = value; }
    public String getDouyinAccountCode() { return douyinAccountCode; }
    public void setDouyinAccountCode(String value) { douyinAccountCode = value; }
    public String getDouyinShopName() { return douyinShopName; }
    public void setDouyinShopName(String value) { douyinShopName = value; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String value) { messageTemplate = value; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer value) { dailyLimit = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public Integer getHourlyLimit() { return hourlyLimit; }
    public void setHourlyLimit(Integer value) { hourlyLimit = value; }
    public Integer getBurstSize() { return burstSize; }
    public void setBurstSize(Integer value) { burstSize = value; }
    public Integer getRestMinutes() { return restMinutes; }
    public void setRestMinutes(Integer value) { restMinutes = value; }
    public String getAllowedStartTime() { return allowedStartTime; }
    public void setAllowedStartTime(String value) { allowedStartTime = value; }
    public String getAllowedEndTime() { return allowedEndTime; }
    public void setAllowedEndTime(String value) { allowedEndTime = value; }
    public Integer getRefundCooldownDays() { return refundCooldownDays; }
    public void setRefundCooldownDays(Integer value) { refundCooldownDays = value; }
    public Integer getCancelledCooldownDays() { return cancelledCooldownDays; }
    public void setCancelledCooldownDays(Integer value) { cancelledCooldownDays = value; }
    public Boolean getPauseOnCaptcha() { return pauseOnCaptcha; }
    public void setPauseOnCaptcha(Boolean value) { pauseOnCaptcha = value; }
    public Integer getMaxConsecutiveFailures() { return maxConsecutiveFailures; }
    public void setMaxConsecutiveFailures(Integer value) { maxConsecutiveFailures = value; }
    public List<RpaMessageTemplateRequest> getMessageTemplates() { return messageTemplates; }
    public void setMessageTemplates(List<RpaMessageTemplateRequest> value) { messageTemplates = value; }
}
