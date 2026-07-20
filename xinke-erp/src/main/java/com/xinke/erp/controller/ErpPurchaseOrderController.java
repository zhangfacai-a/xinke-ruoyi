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
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.domain.ErpPurchaseReceiveRequest;
import com.xinke.erp.service.IErpPurchaseOrderService;

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

    @PreAuthorize("@ss.hasPermi('erp:purchase:list')")
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(purchaseOrderService.selectPurchaseOrderSummary());
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:query')")
    @GetMapping("/sku/{skuId}/history")
    public AjaxResult skuHistory(@PathVariable Long skuId, Integer limit)
    {
        return success(purchaseOrderService.selectSkuPurchaseHistory(skuId, limit));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:query')")
    @GetMapping("/{purchaseId}")
    public AjaxResult getInfo(@PathVariable Long purchaseId)
    {
        return success(purchaseOrderService.selectPurchaseOrderById(purchaseId));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:add')")
    @Log(title = "ERP采购订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpPurchaseOrder purchaseOrder)
    {
        purchaseOrder.setCreateBy(getUsername());
        return toAjax(purchaseOrderService.insertPurchaseOrder(purchaseOrder));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:edit')")
    @Log(title = "ERP采购订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpPurchaseOrder purchaseOrder)
    {
        purchaseOrder.setUpdateBy(getUsername());
        return toAjax(purchaseOrderService.updatePurchaseOrder(purchaseOrder));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:edit')")
    @Log(title = "ERP采购订单状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{purchaseId}/status/{targetStatus}")
    public AjaxResult updateStatus(@PathVariable Long purchaseId, @PathVariable String targetStatus)
    {
        return toAjax(purchaseOrderService.updatePurchaseOrderStatus(purchaseId, targetStatus, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:edit')")
    @Log(title = "ERP采购到货", businessType = BusinessType.INSERT)
    @PostMapping("/{purchaseId}/receive")
    public AjaxResult receive(@PathVariable Long purchaseId, @RequestBody ErpPurchaseReceiveRequest request)
    {
        return toAjax(purchaseOrderService.receivePurchaseOrder(purchaseId, request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:purchase:remove')")
    @Log(title = "ERP采购订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{purchaseIds}")
    public AjaxResult remove(@PathVariable Long[] purchaseIds)
    {
        return toAjax(purchaseOrderService.deletePurchaseOrderByIds(purchaseIds));
    }
}
