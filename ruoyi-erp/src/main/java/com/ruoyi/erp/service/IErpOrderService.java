package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpOrderItem;

public interface IErpOrderService
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);
}
