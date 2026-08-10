package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class RpaViewerIdsRequest
{
    @NotEmpty(message = "viewerIds不能为空")
    @Size(max = 500, message = "单次最多操作500个用户")
    private List<Long> viewerIds;

    public List<Long> getViewerIds() { return viewerIds; }
    public void setViewerIds(List<Long> viewerIds) { this.viewerIds = viewerIds; }
}
