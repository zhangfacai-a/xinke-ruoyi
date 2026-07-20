package com.xinke.erp.domain;

import java.io.Serializable;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ErpShipmentCreateRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    @NotNull(message = "销售订单不能为空")
    private Long salesId;

    @NotNull(message = "发货仓库不能为空")
    private Long warehouseId;

    @Size(max = 128, message = "物流公司不能超过128个字符")
    private String logisticsCompany;

    @Size(max = 128, message = "物流单号不能超过128个字符")
    private String logisticsNo;

    @Size(max = 64, message = "幂等标识不能超过64个字符")
    private String idempotencyKey;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "至少选择一个发货商品")
    private List<ErpFulfillmentQuantity> items;

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long salesId) { this.salesId = salesId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public String getLogisticsNo() { return logisticsNo; }
    public void setLogisticsNo(String logisticsNo) { this.logisticsNo = logisticsNo; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<ErpFulfillmentQuantity> getItems() { return items; }
    public void setItems(List<ErpFulfillmentQuantity> items) { this.items = items; }
}
