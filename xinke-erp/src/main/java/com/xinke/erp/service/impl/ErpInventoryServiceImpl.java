package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpInventoryBalance;
import com.xinke.erp.mapper.ErpInventoryMapper;
import com.xinke.erp.service.IErpInventoryService;

@Service
public class ErpInventoryServiceImpl implements IErpInventoryService
{
    @Autowired
    private ErpInventoryMapper inventoryMapper;

    @Override
    public List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory)
    {
        return inventoryMapper.selectInventoryList(inventory);
    }
}
