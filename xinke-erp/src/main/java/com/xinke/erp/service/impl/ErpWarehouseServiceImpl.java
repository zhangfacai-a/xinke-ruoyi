package com.xinke.erp.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.StringUtils;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.domain.ErpWarehouseLocation;
import com.xinke.erp.mapper.ErpWarehouseMapper;
import com.xinke.erp.service.IErpWarehouseService;

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
    public int insertWarehouse(ErpWarehouse warehouse)
    {
        normalizeWarehouse(warehouse);
        ensureWarehouseCodeUnique(warehouse);
        return warehouseMapper.insertWarehouse(warehouse);
    }
    @Override
    public int updateWarehouse(ErpWarehouse warehouse)
    {
        ErpWarehouse old = warehouseMapper.selectWarehouseById(warehouse.getWarehouseId());
        if (old == null)
        {
            throw new ServiceException("仓库不存在");
        }
        normalizeWarehouse(warehouse);
        ensureWarehouseCodeUnique(warehouse);
        if (!Objects.equals(normalizeWarehouseType(old.getWarehouseType()), warehouse.getWarehouseType())
                && warehouseMapper.countInventoryByWarehouse(warehouse.getWarehouseId()) > 0)
        {
            throw new ServiceException("仓库已有库存，不能变更实体仓/云仓形态");
        }
        if ("cloud".equals(warehouse.getWarehouseType()))
        {
            warehouse.setSyncStatus("not_applicable".equals(old.getSyncStatus())
                    ? "never" : StringUtils.defaultIfBlank(old.getSyncStatus(), "never"));
        }
        return warehouseMapper.updateWarehouse(warehouse);
    }
    @Override
    @Transactional
    public int deleteWarehouseByIds(Long[] warehouseIds)
    {
        for (Long warehouseId : warehouseIds)
        {
            if (warehouseMapper.countInventoryByWarehouse(warehouseId) > 0)
            {
                throw new ServiceException("仓库已有库存记录，不能删除；请停用仓库以保留业务历史");
            }
        }
        return warehouseMapper.deleteWarehouseByIds(warehouseIds);
    }

    @Override
    public Map<String, Object> selectWarehouseSummary()
    {
        return warehouseMapper.selectWarehouseSummary();
    }

    @Override
    public List<ErpWarehouseLocation> selectLocationList(Long warehouseId)
    {
        requirePhysicalWarehouse(warehouseId);
        return warehouseMapper.selectLocationList(warehouseId);
    }

    @Override
    public ErpWarehouseLocation selectLocationById(Long locationId)
    {
        return warehouseMapper.selectLocationById(locationId);
    }

    @Override
    public int insertLocation(ErpWarehouseLocation location)
    {
        requirePhysicalWarehouse(location.getWarehouseId());
        normalizeLocation(location);
        ensureLocationCodeUnique(location);
        return warehouseMapper.insertLocation(location);
    }

    @Override
    public int updateLocation(ErpWarehouseLocation location)
    {
        ErpWarehouseLocation old = warehouseMapper.selectLocationById(location.getLocationId());
        if (old == null)
        {
            throw new ServiceException("库位不存在");
        }
        location.setWarehouseId(old.getWarehouseId());
        requirePhysicalWarehouse(location.getWarehouseId());
        normalizeLocation(location);
        ensureLocationCodeUnique(location);
        return warehouseMapper.updateLocation(location);
    }

    @Override
    public int deleteLocationByIds(Long[] locationIds)
    {
        return warehouseMapper.deleteLocationByIds(locationIds);
    }

    private void normalizeWarehouse(ErpWarehouse warehouse)
    {
        warehouse.setWarehouseCode(warehouse.getWarehouseCode().trim().toUpperCase(Locale.ROOT));
        warehouse.setWarehouseName(warehouse.getWarehouseName().trim());
        warehouse.setWarehouseType(normalizeWarehouseType(warehouse.getWarehouseType()));
        warehouse.setWarehouseUsage(StringUtils.defaultIfBlank(warehouse.getWarehouseUsage(), "normal"));
        warehouse.setStatus(StringUtils.defaultIfBlank(warehouse.getStatus(), "0"));
        warehouse.setAllowNegativeStock(StringUtils.defaultIfBlank(warehouse.getAllowNegativeStock(), "0"));
        warehouse.setPriority(warehouse.getPriority() == null ? 100 : warehouse.getPriority());

        if (!Set.of("normal", "return", "defective", "transit").contains(warehouse.getWarehouseUsage()))
        {
            throw new ServiceException("不支持的仓库用途");
        }
        if (!Set.of("0", "1").contains(warehouse.getAllowNegativeStock()))
        {
            throw new ServiceException("负库存配置不正确");
        }

        if ("cloud".equals(warehouse.getWarehouseType()))
        {
            if (StringUtils.isBlank(warehouse.getProviderName()) || StringUtils.isBlank(warehouse.getExternalWarehouseCode()))
            {
                throw new ServiceException("云仓必须填写服务商和外部仓编码");
            }
            warehouse.setOwnershipType(StringUtils.defaultIfBlank(warehouse.getOwnershipType(), "outsourced"));
            warehouse.setSyncMode(StringUtils.defaultIfBlank(warehouse.getSyncMode(), "manual"));
            warehouse.setSyncStatus(StringUtils.defaultIfBlank(warehouse.getSyncStatus(), "never"));
            warehouse.setEnableLocation("0");
        }
        else
        {
            warehouse.setOwnershipType(StringUtils.defaultIfBlank(warehouse.getOwnershipType(), "self_operated"));
            warehouse.setSyncMode("manual");
            warehouse.setSyncStatus("not_applicable");
            warehouse.setEnableLocation(StringUtils.defaultIfBlank(warehouse.getEnableLocation(), "1"));
            warehouse.setProviderName(null);
            warehouse.setExternalWarehouseCode(null);
            warehouse.setOwnerCode(null);
        }
    }

    private String normalizeWarehouseType(String warehouseType)
    {
        if ("third_party".equals(warehouseType)) return "cloud";
        if ("self".equals(warehouseType) || "return".equals(warehouseType) || StringUtils.isBlank(warehouseType)) return "physical";
        if (!Set.of("physical", "cloud").contains(warehouseType))
        {
            throw new ServiceException("仓库形态只能是实体仓或云仓");
        }
        return warehouseType;
    }

    private void ensureWarehouseCodeUnique(ErpWarehouse warehouse)
    {
        ErpWarehouse sameCode = warehouseMapper.selectWarehouseByCode(warehouse.getWarehouseCode());
        if (sameCode != null && !Objects.equals(sameCode.getWarehouseId(), warehouse.getWarehouseId()))
        {
            throw new ServiceException("仓库编码已存在");
        }
    }

    private ErpWarehouse requirePhysicalWarehouse(Long warehouseId)
    {
        ErpWarehouse warehouse = warehouseMapper.selectWarehouseById(warehouseId);
        if (warehouse == null)
        {
            throw new ServiceException("仓库不存在");
        }
        if (!"physical".equals(normalizeWarehouseType(warehouse.getWarehouseType())))
        {
            throw new ServiceException("云仓由服务商维护库位，不能在 ERP 中建立库位");
        }
        return warehouse;
    }

    private void normalizeLocation(ErpWarehouseLocation location)
    {
        location.setLocationCode(location.getLocationCode().trim().toUpperCase(Locale.ROOT));
        location.setLocationName(location.getLocationName().trim());
        location.setUsageType(StringUtils.defaultIfBlank(location.getUsageType(), "normal"));
        location.setSortOrder(location.getSortOrder() == null ? 100 : location.getSortOrder());
        location.setStatus(StringUtils.defaultIfBlank(location.getStatus(), "0"));
        if (!Set.of("normal", "return", "defective", "staging").contains(location.getUsageType()))
        {
            throw new ServiceException("不支持的库位用途");
        }
    }

    private void ensureLocationCodeUnique(ErpWarehouseLocation location)
    {
        ErpWarehouseLocation sameCode = warehouseMapper.selectLocationByCode(location.getWarehouseId(), location.getLocationCode());
        if (sameCode != null && !Objects.equals(sameCode.getLocationId(), location.getLocationId()))
        {
            throw new ServiceException("当前仓库已存在相同库位编码");
        }
    }
}
