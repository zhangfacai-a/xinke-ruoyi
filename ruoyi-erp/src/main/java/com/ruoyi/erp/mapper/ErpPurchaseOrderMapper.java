package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpPurchaseOrder;

@Mapper
public interface ErpPurchaseOrderMapper
{
    List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder);
    ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId);
    int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder);
    int deletePurchaseOrderByIds(Long[] purchaseIds);
}
