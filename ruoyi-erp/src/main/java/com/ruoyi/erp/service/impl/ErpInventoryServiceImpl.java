package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpInventoryBalance;
import com.ruoyi.erp.mapper.ErpInventoryMapper;
import com.ruoyi.erp.service.IErpInventoryService;

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
