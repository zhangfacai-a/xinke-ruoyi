package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseOrder;

public interface IErpPurchaseOrderService
{
    List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder);
    ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId);
    int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int deletePurchaseOrderByIds(Long[] purchaseIds);
}
