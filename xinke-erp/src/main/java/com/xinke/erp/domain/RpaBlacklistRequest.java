package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RpaBlacklistRequest
{
    @NotEmpty(message = "viewerIds不能为空")
    @Size(max = 500, message = "单次最多拉黑500个用户")
    private List<Long> viewerIds;

    @NotBlank(message = "reason不能为空")
    @Size(max = 64, message = "reason长度不能超过64")
    private String reason;

    @Pattern(regexp = "GLOBAL|SHOP", message = "scope只能是GLOBAL或SHOP")
    private String scope = "GLOBAL";

    private Long shopConfigId;

    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;

    public List<Long> getViewerIds() { return viewerIds; }
    public void setViewerIds(List<Long> viewerIds) { this.viewerIds = viewerIds; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getShopConfigId() { return shopConfigId; }
    public void setShopConfigId(Long shopConfigId) { this.shopConfigId = shopConfigId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
