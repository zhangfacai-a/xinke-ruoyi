package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RpaViewerTrackingRequest
{
    @NotEmpty(message = "viewerIds不能为空")
    @Size(max = 500, message = "单次最多设置500个用户")
    private List<Long> viewerIds;

    @NotBlank(message = "mode不能为空")
    @Pattern(regexp = "AUTO|INCLUDE|EXCLUDE", message = "mode只能是AUTO、INCLUDE或EXCLUDE")
    private String mode;

    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;

    public List<Long> getViewerIds() { return viewerIds; }
    public void setViewerIds(List<Long> viewerIds) { this.viewerIds = viewerIds; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
