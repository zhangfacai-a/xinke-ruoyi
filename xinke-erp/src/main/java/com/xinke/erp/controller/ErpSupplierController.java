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
import com.xinke.erp.domain.ErpSupplier;
import com.xinke.erp.service.IErpSupplierService;

@RestController
@RequestMapping("/erp/supplier")
public class ErpSupplierController extends BaseController
{
    @Autowired
    private IErpSupplierService supplierService;

    @PreAuthorize("@ss.hasPermi('erp:supplier:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpSupplier supplier)
    {
        startPage();
        List<ErpSupplier> list = supplierService.selectSupplierList(supplier);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:query')")
    @GetMapping("/{supplierId}")
    public AjaxResult getInfo(@PathVariable Long supplierId)
    {
        return success(supplierService.selectSupplierById(supplierId));
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:add')")
    @Log(title = "ERP 供应商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpSupplier supplier)
    {
        supplier.setCreateBy(getUsername());
        return toAjax(supplierService.insertSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:edit')")
    @Log(title = "ERP 供应商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpSupplier supplier)
    {
        supplier.setUpdateBy(getUsername());
        return toAjax(supplierService.updateSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:remove')")
    @Log(title = "ERP 供应商", businessType = BusinessType.DELETE)
    @DeleteMapping("/{supplierIds}")
    public AjaxResult remove(@PathVariable Long[] supplierIds)
    {
        return toAjax(supplierService.deleteSupplierByIds(supplierIds));
    }
}
