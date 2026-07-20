package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.domain.ErpWarehouseLocation;

public interface IErpWarehouseService
{
    List<ErpWarehouse> selectWarehouseList(ErpWarehouse warehouse);
    ErpWarehouse selectWarehouseById(Long warehouseId);
    int insertWarehouse(ErpWarehouse warehouse);
    int updateWarehouse(ErpWarehouse warehouse);
    int deleteWarehouseByIds(Long[] warehouseIds);
    Map<String, Object> selectWarehouseSummary();
    List<ErpWarehouseLocation> selectLocationList(Long warehouseId);
    ErpWarehouseLocation selectLocationById(Long locationId);
    int insertLocation(ErpWarehouseLocation location);
    int updateLocation(ErpWarehouseLocation location);
    int deleteLocationByIds(Long[] locationIds);
}
