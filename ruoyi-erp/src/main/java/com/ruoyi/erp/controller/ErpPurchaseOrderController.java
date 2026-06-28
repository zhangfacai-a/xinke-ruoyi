package com.ruoyi.erp.controller;

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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.domain.ErpPurchaseOrder;
import com.ruoyi.erp.service.IErpPurchaseOrderService;

@RestController
@RequestMapping("/erp/purchase")
public class ErpPurchaseOrderController extends BaseController
{
    @Autowired
    private IErpPurchaseOrderService purchaseOrderService;

    @PreAuthorize("@ss.hasPermi('erp:purchase:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpPurchaseOrder purchaseOrder)
    {
        startPage();
        List<ErpPurchaseOrder> list = purchaseOrderService.selectPurchaseOrderList(purchaseOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:query')")
    @GetMapping("/{purchaseId}")
    public AjaxResult getInfo(@PathVariable Long purchaseId)
    {
        return success(purchaseOrderService.selectPurchaseOrderById(purchaseId));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:add')")
    @Log(title = "ERP 采购订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpPurchaseOrder purchaseOrder)
    {
        purchaseOrder.setCreateBy(getUsername());
        return toAjax(purchaseOrderService.insertPurchaseOrder(purchaseOrder));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:edit')")
    @Log(title = "ERP 采购订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpPurchaseOrder purchaseOrder)
    {
        purchaseOrder.setUpdateBy(getUsername());
        return toAjax(purchaseOrderService.updatePurchaseOrder(purchaseOrder));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:remove')")
    @Log(title = "ERP 采购订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{purchaseIds}")
    public AjaxResult remove(@PathVariable Long[] purchaseIds)
    {
        return toAjax(purchaseOrderService.deletePurchaseOrderByIds(purchaseIds));
    }
}
