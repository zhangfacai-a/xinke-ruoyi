package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryMovement;

public interface IErpInventoryMovementService
{
    List<ErpInventoryMovement> selectMovementList(ErpInventoryMovement movement);
}
