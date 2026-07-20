package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.mapper.ErpWarehouseMapper;

@ExtendWith(MockitoExtension.class)
class ErpWarehouseServiceImplTest
{
    @Mock
    private ErpWarehouseMapper warehouseMapper;

    @InjectMocks
    private ErpWarehouseServiceImpl service;

    @Test
    void physicalWarehouseReceivesSafeDefaults()
    {
        ErpWarehouse warehouse = warehouse(" wh-hz-01 ", "杭州仓", "physical");
        when(warehouseMapper.insertWarehouse(any())).thenReturn(1);

        service.insertWarehouse(warehouse);

        ArgumentCaptor<ErpWarehouse> captor = ArgumentCaptor.forClass(ErpWarehouse.class);
        verify(warehouseMapper).insertWarehouse(captor.capture());
        assertEquals("WH-HZ-01", captor.getValue().getWarehouseCode());
        assertEquals("normal", captor.getValue().getWarehouseUsage());
        assertEquals("1", captor.getValue().getEnableLocation());
        assertEquals("0", captor.getValue().getAllowNegativeStock());
        assertEquals("not_applicable", captor.getValue().getSyncStatus());
    }

    @Test
    void cloudWarehouseRequiresProviderAndExternalCode()
    {
        ErpWarehouse warehouse = warehouse("CLOUD-01", "华东云仓", "cloud");
        assertThrows(ServiceException.class, () -> service.insertWarehouse(warehouse));
    }

    @Test
    void warehouseWithInventoryCannotChangeBusinessType()
    {
        ErpWarehouse old = warehouse("WH-01", "一号仓", "physical");
        old.setWarehouseId(1L);
        ErpWarehouse changed = warehouse("WH-01", "一号仓", "cloud");
        changed.setWarehouseId(1L);
        changed.setProviderName("云仓服务商");
        changed.setExternalWarehouseCode("EXT-01");
        when(warehouseMapper.selectWarehouseById(1L)).thenReturn(old);
        when(warehouseMapper.selectWarehouseByCode("WH-01")).thenReturn(old);
        when(warehouseMapper.countInventoryByWarehouse(1L)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.updateWarehouse(changed));
    }

    private ErpWarehouse warehouse(String code, String name, String type)
    {
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setWarehouseCode(code);
        warehouse.setWarehouseName(name);
        warehouse.setWarehouseType(type);
        warehouse.setStatus("0");
        return warehouse;
    }
}
