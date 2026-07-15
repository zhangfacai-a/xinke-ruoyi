package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpInventoryMovement;
import com.xinke.erp.mapper.ErpInventoryMovementMapper;
import com.xinke.erp.service.IErpInventoryMovementService;

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
