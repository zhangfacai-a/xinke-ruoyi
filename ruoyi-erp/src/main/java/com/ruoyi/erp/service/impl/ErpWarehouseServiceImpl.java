package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpWarehouse;
import com.ruoyi.erp.mapper.ErpWarehouseMapper;
import com.ruoyi.erp.service.IErpWarehouseService;

@Service
public class ErpWarehouseServiceImpl implements IErpWarehouseService
{
    @Autowired
    private ErpWarehouseMapper warehouseMapper;

    @Override
    public List<ErpWarehouse> selectWarehouseList(ErpWarehouse warehouse) { return warehouseMapper.selectWarehouseList(warehouse); }
    @Override
    public ErpWarehouse selectWarehouseById(Long warehouseId) { return warehouseMapper.selectWarehouseById(warehouseId); }
    @Override
    public int insertWarehouse(ErpWarehouse warehouse) { return warehouseMapper.insertWarehouse(warehouse); }
    @Override
    public int updateWarehouse(ErpWarehouse warehouse) { return warehouseMapper.updateWarehouse(warehouse); }
    @Override
    public int deleteWarehouseByIds(Long[] warehouseIds) { return warehouseMapper.deleteWarehouseByIds(warehouseIds); }
}
