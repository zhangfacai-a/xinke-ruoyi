package com.xinke.erp.domain;

import java.util.Date;
import com.xinke.common.annotation.Excel;
import com.xinke.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ErpWarehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 64, message = "仓库编码长度不能超过64")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 128, message = "仓库名称长度不能超过128")
    private String warehouseName;

    @Excel(name = "仓库形态", readConverterExp = "physical=实体仓,cloud=云仓")
    private String warehouseType;

    @Excel(name = "仓库用途", readConverterExp = "normal=正品仓,return=退货仓,defective=残次仓,transit=中转仓")
    private String warehouseUsage;

    @Excel(name = "经营方式", readConverterExp = "self_operated=自营,outsourced=外包")
    private String ownershipType;

    @Excel(name = "服务商")
    private String providerName;

    @Excel(name = "外部仓编码")
    private String externalWarehouseCode;

    @Excel(name = "货主编码")
    private String ownerCode;

    @Excel(name = "同步方式", readConverterExp = "manual=手工,api=接口,file=文件")
    private String syncMode;

    @Excel(name = "同步状态", readConverterExp = "never=未同步,success=正常,warning=有差异,failed=失败,not_applicable=不适用")
    private String syncStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后同步时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    @Excel(name = "启用库位", readConverterExp = "0=否,1=是")
    private String enableLocation;

    @Excel(name = "允许负库存", readConverterExp = "0=否,1=是")
    private String allowNegativeStock;

    @Excel(name = "出库优先级")
    private Integer priority;

    private Integer skuCount;

    private Integer locationCount;

    @Excel(name = "联系人")
    private String contactName;

    @Excel(name = "联系电话")
    private String contactPhone;

    @Excel(name = "地址")
    private String address;

    @Excel(name = "状态")
    private String status;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseType() { return warehouseType; }
    public void setWarehouseType(String warehouseType) { this.warehouseType = warehouseType; }
    public String getWarehouseUsage() { return warehouseUsage; }
    public void setWarehouseUsage(String warehouseUsage) { this.warehouseUsage = warehouseUsage; }
    public String getOwnershipType() { return ownershipType; }
    public void setOwnershipType(String ownershipType) { this.ownershipType = ownershipType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getExternalWarehouseCode() { return externalWarehouseCode; }
    public void setExternalWarehouseCode(String externalWarehouseCode) { this.externalWarehouseCode = externalWarehouseCode; }
    public String getOwnerCode() { return ownerCode; }
    public void setOwnerCode(String ownerCode) { this.ownerCode = ownerCode; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public Date getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Date lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public String getEnableLocation() { return enableLocation; }
    public void setEnableLocation(String enableLocation) { this.enableLocation = enableLocation; }
    public String getAllowNegativeStock() { return allowNegativeStock; }
    public void setAllowNegativeStock(String allowNegativeStock) { this.allowNegativeStock = allowNegativeStock; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSkuCount() { return skuCount; }
    public void setSkuCount(Integer skuCount) { this.skuCount = skuCount; }
    public Integer getLocationCount() { return locationCount; }
    public void setLocationCount(Integer locationCount) { this.locationCount = locationCount; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
