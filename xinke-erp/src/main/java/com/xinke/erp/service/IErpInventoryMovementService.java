package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpInventoryMovement;

public interface IErpInventoryMovementService
{
    List<ErpInventoryMovement> selectMovementList(ErpInventoryMovement movement);
}
