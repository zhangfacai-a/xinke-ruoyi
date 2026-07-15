package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.ErpInventoryMovement;

@Mapper
public interface ErpInventoryMovementMapper
{
    List<ErpInventoryMovement> selectMovementList(ErpInventoryMovement movement);
}
