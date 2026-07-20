package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import com.xinke.erp.domain.ErpAfterSaleCreateRequest;
import com.xinke.erp.domain.ErpOrderItem;
import com.xinke.erp.domain.ErpShipmentCreateRequest;

public interface IErpOrderService
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);

    List<Map<String, Object>> selectFulfillmentOrderList(Map<String, Object> query);
    Map<String, Object> selectFulfillmentSummary(Map<String, Object> query);
    Map<String, Object> selectFulfillmentDetail(Long salesId);
    Map<String, Object> syncFulfillmentOrders(String username);

    String createShipment(ErpShipmentCreateRequest request, String username);
    int shipShipment(String shipmentNo, String username);
    int signShipment(String shipmentNo, String username);
    int cancelShipment(String shipmentNo, String reason, String username);

    String createAfterSale(ErpAfterSaleCreateRequest request, String username);
    int approveAfterSale(String afterSaleNo, String username);
    int rejectAfterSale(String afterSaleNo, String reason, String username);
    int completeAfterSale(String afterSaleNo, String username);
}
