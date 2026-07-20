package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.domain.ErpWarehouseLocation;

@Mapper
public interface ErpWarehouseMapper
{
    List<ErpWarehouse> selectWarehouseList(ErpWarehouse warehouse);
    ErpWarehouse selectWarehouseById(Long warehouseId);
    ErpWarehouse selectWarehouseByCode(String warehouseCode);
    int insertWarehouse(ErpWarehouse warehouse);
    int updateWarehouse(ErpWarehouse warehouse);
    int deleteWarehouseByIds(Long[] warehouseIds);
    int countInventoryByWarehouse(Long warehouseId);
    Map<String, Object> selectWarehouseSummary();
    int updateWarehouseSyncStatus(@Param("warehouseId") Long warehouseId, @Param("syncStatus") String syncStatus);
    List<ErpWarehouseLocation> selectLocationList(Long warehouseId);
    ErpWarehouseLocation selectLocationById(Long locationId);
    ErpWarehouseLocation selectLocationByCode(@Param("warehouseId") Long warehouseId, @Param("locationCode") String locationCode);
    int insertLocation(ErpWarehouseLocation location);
    int updateLocation(ErpWarehouseLocation location);
    int deleteLocationByIds(Long[] locationIds);
}
