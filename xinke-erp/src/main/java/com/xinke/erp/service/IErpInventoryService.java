package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpInventoryBalance;

public interface IErpInventoryService
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
}
