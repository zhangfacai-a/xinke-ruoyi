package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.domain.ErpPurchaseReceiveRequest;

public interface IErpPurchaseOrderService
{
    List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder);
    ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId);
    List<Map<String, Object>> selectSkuPurchaseHistory(Long skuId, Integer limit);
    Map<String, Object> selectPurchaseOrderSummary();
    int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrderStatus(Long purchaseId, String targetStatus, String operator);
    int receivePurchaseOrder(Long purchaseId, ErpPurchaseReceiveRequest request, String operator);
    int deletePurchaseOrderByIds(Long[] purchaseIds);
}
