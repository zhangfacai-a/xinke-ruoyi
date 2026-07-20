package com.xinke.erp.domain;

import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ErpWarehouseLocation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long locationId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Excel(name = "库位编码")
    @NotBlank(message = "库位编码不能为空")
    @Size(max = 64, message = "库位编码长度不能超过64")
    private String locationCode;

    @Excel(name = "库位名称")
    @NotBlank(message = "库位名称不能为空")
    @Size(max = 128, message = "库位名称长度不能超过128")
    private String locationName;

    @Excel(name = "库区")
    private String zoneCode;

    @Excel(name = "用途", readConverterExp = "normal=正品,return=退货,defective=残次,staging=暂存")
    private String usageType;

    @Excel(name = "条码")
    private String barcode;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
