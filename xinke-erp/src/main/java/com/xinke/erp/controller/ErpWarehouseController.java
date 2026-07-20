package com.xinke.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.domain.ErpWarehouseLocation;
import com.xinke.erp.service.IErpWarehouseService;

@RestController
@RequestMapping("/erp/warehouse")
public class ErpWarehouseController extends BaseController
{
    @Autowired
    private IErpWarehouseService warehouseService;

    @PreAuthorize("@ss.hasPermi('erp:warehouse:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpWarehouse warehouse)
    {
        startPage();
        List<ErpWarehouse> list = warehouseService.selectWarehouseList(warehouse);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:list')")
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(warehouseService.selectWarehouseSummary());
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:query')")
    @GetMapping("/{warehouseId}")
    public AjaxResult getInfo(@PathVariable Long warehouseId)
    {
        return success(warehouseService.selectWarehouseById(warehouseId));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:add')")
    @Log(title = "ERP 仓库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpWarehouse warehouse)
    {
        warehouse.setCreateBy(getUsername());
        return toAjax(warehouseService.insertWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:edit')")
    @Log(title = "ERP 仓库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpWarehouse warehouse)
    {
        warehouse.setUpdateBy(getUsername());
        return toAjax(warehouseService.updateWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:remove')")
    @Log(title = "ERP 仓库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{warehouseIds}")
    public AjaxResult remove(@PathVariable Long[] warehouseIds)
    {
        return toAjax(warehouseService.deleteWarehouseByIds(warehouseIds));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:query')")
    @GetMapping("/{warehouseId}/location/list")
    public AjaxResult locationList(@PathVariable Long warehouseId)
    {
        return success(warehouseService.selectLocationList(warehouseId));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:add')")
    @Log(title = "ERP 仓库库位", businessType = BusinessType.INSERT)
    @PostMapping("/location")
    public AjaxResult addLocation(@Validated @RequestBody ErpWarehouseLocation location)
    {
        location.setCreateBy(getUsername());
        return toAjax(warehouseService.insertLocation(location));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:edit')")
    @Log(title = "ERP 仓库库位", businessType = BusinessType.UPDATE)
    @PutMapping("/location")
    public AjaxResult editLocation(@Validated @RequestBody ErpWarehouseLocation location)
    {
        location.setUpdateBy(getUsername());
        return toAjax(warehouseService.updateLocation(location));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:remove')")
    @Log(title = "ERP 仓库库位", businessType = BusinessType.DELETE)
    @DeleteMapping("/location/{locationIds}")
    public AjaxResult removeLocation(@PathVariable Long[] locationIds)
    {
        return toAjax(warehouseService.deleteLocationByIds(locationIds));
    }
}
