package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpPurchaseOrder;

public interface IErpPurchaseOrderService
{
    List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder);
    ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId);
    int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int deletePurchaseOrderByIds(Long[] purchaseIds);
}
