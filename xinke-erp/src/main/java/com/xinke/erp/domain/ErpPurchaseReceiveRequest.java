package com.xinke.erp.domain;

import java.io.Serializable;
import java.util.List;

public class ErpPurchaseReceiveRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    private List<ErpPurchaseReceiveItem> items;
    private String remark;

    public List<ErpPurchaseReceiveItem> getItems() { return items; }
    public void setItems(List<ErpPurchaseReceiveItem> items) { this.items = items; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
