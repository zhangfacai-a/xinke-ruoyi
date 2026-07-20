package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ErpCloudInventorySyncRequest
{
    @NotNull(message = "云仓不能为空")
    private Long warehouseId;

    @Size(max = 64, message = "同步来源长度不能超过64")
    private String source;

    @Valid
    @NotEmpty(message = "同步明细不能为空")
    @Size(max = 5000, message = "单次最多同步5000个SKU")
    private List<ErpCloudInventorySyncItem> items;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<ErpCloudInventorySyncItem> getItems() { return items; }
    public void setItems(List<ErpCloudInventorySyncItem> items) { this.items = items; }
}
