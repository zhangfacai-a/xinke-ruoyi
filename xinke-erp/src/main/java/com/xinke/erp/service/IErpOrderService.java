package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpOrderItem;

public interface IErpOrderService
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);
}
