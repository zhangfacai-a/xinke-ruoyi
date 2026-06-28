package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpInventoryMovement;

@Mapper
public interface ErpInventoryMovementMapper
{
    List<ErpInventoryMovement> selectMovementList(ErpInventoryMovement movement);
}
