package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpWarehouse;

public interface IErpWarehouseService
{
    List<ErpWarehouse> selectWarehouseList(ErpWarehouse warehouse);
    ErpWarehouse selectWarehouseById(Long warehouseId);
    int insertWarehouse(ErpWarehouse warehouse);
    int updateWarehouse(ErpWarehouse warehouse);
    int deleteWarehouseByIds(Long[] warehouseIds);
}
