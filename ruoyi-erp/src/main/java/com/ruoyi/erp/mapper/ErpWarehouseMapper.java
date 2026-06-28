package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpWarehouse;

@Mapper
public interface ErpWarehouseMapper
{
    List<ErpWarehouse> selectWarehouseList(ErpWarehouse warehouse);
    ErpWarehouse selectWarehouseById(Long warehouseId);
    int insertWarehouse(ErpWarehouse warehouse);
    int updateWarehouse(ErpWarehouse warehouse);
    int deleteWarehouseByIds(Long[] warehouseIds);
}
