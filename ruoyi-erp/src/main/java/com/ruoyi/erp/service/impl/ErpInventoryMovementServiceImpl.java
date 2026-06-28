package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpInventoryMovement;
import com.ruoyi.erp.mapper.ErpInventoryMovementMapper;
import com.ruoyi.erp.service.IErpInventoryMovementService;

@Service
public class ErpInventoryMovementServiceImpl implements IErpInventoryMovementService
{
    @Autowired
    private ErpInventoryMovementMapper movementMapper;

    @Override
    public List<ErpInventoryMovement> selectMovementList(ErpInventoryMovement movement)
    {
        return movementMapper.selectMovementList(movement);
    }
}
