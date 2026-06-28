package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryBalance;

public interface IErpInventoryService
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
}
