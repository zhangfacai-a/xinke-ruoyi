package com.ruoyi.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.service.IErpFlowService;

@RestController
@RequestMapping("/erp/flow")
public class ErpFlowController extends BaseController
{
    @Autowired
    private IErpFlowService erpFlowService;

    @PreAuthorize("@ss.hasPermi('erp:flow:list')")
    @GetMapping("/{flowType}/list")
    public TableDataInfo list(@PathVariable String flowType, @RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = erpFlowService.list(flowType, query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:flow:query')")
    @GetMapping("/{flowType}/{id}")
    public AjaxResult getInfo(@PathVariable String flowType, @PathVariable Long id)
    {
        return success(erpFlowService.getInfo(flowType, id));
    }

    @PreAuthorize("@ss.hasPermi('erp:flow:add')")
    @Log(title = "ERP business flow", businessType = BusinessType.INSERT)
    @PostMapping("/{flowType}")
    public AjaxResult add(@PathVariable String flowType, @RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.add(flowType, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:flow:edit')")
    @Log(title = "ERP business flow", businessType = BusinessType.UPDATE)
    @PutMapping("/{flowType}/{id}")
    public AjaxResult edit(@PathVariable String flowType, @PathVariable Long id, @RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.edit(flowType, id, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:flow:remove')")
    @Log(title = "ERP business flow", businessType = BusinessType.DELETE)
    @DeleteMapping("/{flowType}/{ids}")
    public AjaxResult remove(@PathVariable String flowType, @PathVariable Long[] ids)
    {
        return toAjax(erpFlowService.remove(flowType, ids));
    }

    @PreAuthorize("@ss.hasPermi('erp:flow:audit')")
    @Log(title = "ERP business flow approve", businessType = BusinessType.UPDATE)
    @PostMapping("/{flowType}/{bizNo}/approve")
    public AjaxResult approve(@PathVariable String flowType, @PathVariable String bizNo)
    {
        return toAjax(erpFlowService.approve(flowType, bizNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:in')")
    @Log(title = "ERP inventory in", businessType = BusinessType.INSERT)
    @PostMapping("/inventory/in")
    public AjaxResult inventoryIn(@RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.createInventoryIn(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:out')")
    @Log(title = "ERP inventory out", businessType = BusinessType.INSERT)
    @PostMapping("/inventory/out")
    public AjaxResult inventoryOut(@RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.createInventoryOut(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:transfer')")
    @Log(title = "ERP inventory transfer", businessType = BusinessType.INSERT)
    @PostMapping("/inventory/transfer")
    public AjaxResult inventoryTransfer(@RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.createInventoryTransfer(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:loss')")
    @Log(title = "ERP inventory loss", businessType = BusinessType.INSERT)
    @PostMapping("/inventory/loss")
    public AjaxResult inventoryLoss(@RequestBody Map<String, Object> form)
    {
        return toAjax(erpFlowService.createInventoryLoss(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:add')")
    @Log(title = "ERP supplier reconcile payable", businessType = BusinessType.INSERT)
    @PostMapping("/supplier-reconcile/{reconcileNo}/payable")
    public AjaxResult generatePayable(@PathVariable String reconcileNo)
    {
        return toAjax(erpFlowService.generatePayableFromSupplierReconcile(reconcileNo, getUsername()));
    }
}
