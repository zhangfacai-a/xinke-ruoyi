package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.erp.domain.ErpOrderItem;

@Mapper
public interface ErpOrderMapper
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);

    List<Map<String, Object>> selectFulfillmentOrderList(Map<String, Object> query);
    Map<String, Object> selectFulfillmentSummary(Map<String, Object> query);
    Map<String, Object> selectSalesOrderById(Long salesId);
    Map<String, Object> selectSalesOrderForUpdate(Long salesId);
    List<Map<String, Object>> selectSalesOrderItems(Long salesId);
    Map<String, Object> selectSalesOrderItem(Long salesItemId);
    Map<String, Object> selectSalesOrderItemForUpdate(Long salesItemId);
    List<Map<String, Object>> selectSalesShipments(Long salesId);
    List<Map<String, Object>> selectShipmentItems(Long shipmentId);
    List<Map<String, Object>> selectAfterSales(Long salesId);
    List<Map<String, Object>> selectAvailableWarehouses();
    Map<String, Object> selectWarehouseForFulfillment(Long warehouseId);
    List<Map<String, Object>> selectFulfillmentLogs(Long salesId);
    int selectReservedShipmentQuantity(Long salesItemId);
    Map<String, Object> selectReservedAfterSale(Long salesItemId);

    int syncSalesOrdersFromDwd();
    int syncSalesOrderItemsFromDwd();
    int refreshSalesOrderQuantity();

    Map<String, Object> selectShipmentByIdempotencyKey(String idempotencyKey);
    Map<String, Object> selectShipmentForUpdate(String shipmentNo);
    int insertShipment(Map<String, Object> shipment);
    int insertShipmentItem(Map<String, Object> item);
    int updateShipmentStatus(@Param("shipmentNo") String shipmentNo,
                             @Param("fromStatus") String fromStatus,
                             @Param("toStatus") String toStatus,
                             @Param("username") String username,
                             @Param("reason") String reason);
    int increaseItemShippedQuantity(@Param("salesItemId") Long salesItemId,
                                    @Param("quantity") Integer quantity);

    Map<String, Object> selectAfterSaleByIdempotencyKey(String idempotencyKey);
    Map<String, Object> selectAfterSaleForUpdate(String afterSaleNo);
    int insertAfterSale(Map<String, Object> afterSale);
    int updateAfterSaleStatus(@Param("afterSaleNo") String afterSaleNo,
                              @Param("fromStatus") String fromStatus,
                              @Param("toStatus") String toStatus,
                              @Param("username") String username);
    int increaseItemRefundedQuantity(@Param("salesItemId") Long salesItemId,
                                     @Param("quantity") Integer quantity);
    int updateDwdRefundCost(@Param("dwdOrderItemId") Long dwdOrderItemId,
                            @Param("refundAmount") java.math.BigDecimal refundAmount);

    int refreshSalesOrderItemStatus(Long salesId);
    int refreshSalesOrderStatus(Long salesId);
    int insertFulfillmentLog(Map<String, Object> log);
}
