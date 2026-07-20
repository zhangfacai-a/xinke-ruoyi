package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.domain.ErpPurchaseOrderItem;

@Mapper
public interface ErpPurchaseOrderMapper
{
    List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder);
    ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId);
    List<ErpPurchaseOrderItem> selectPurchaseOrderItemsByPurchaseId(Long purchaseId);
    List<Map<String, Object>> selectSkuPurchaseHistory(@Param("skuId") Long skuId,
            @Param("limit") Integer limit);
    Map<String, Object> selectPurchaseOrderSummary();
    int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int batchInsertPurchaseOrderItems(List<ErpPurchaseOrderItem> items);
    int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrderStatus(@Param("purchaseId") Long purchaseId,
            @Param("currentStatus") String currentStatus,
            @Param("targetStatus") String targetStatus,
            @Param("operator") String operator);
    int incrementReceivedQty(@Param("purchaseId") Long purchaseId,
            @Param("itemId") Long itemId,
            @Param("quantity") Integer quantity);
    int batchInsertPurchaseReceiptItems(@Param("receiptNo") String receiptNo,
            @Param("purchaseNo") String purchaseNo,
            @Param("items") List<Map<String, Object>> items);
    int deletePurchaseOrderItemsByPurchaseId(Long purchaseId);
    int deletePurchaseOrderItemsByPurchaseIds(Long[] purchaseIds);
    int deletePurchaseOrderByIds(Long[] purchaseIds);
}
