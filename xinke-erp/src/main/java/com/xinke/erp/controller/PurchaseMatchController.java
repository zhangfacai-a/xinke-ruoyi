package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.annotation.Log;
import com.xinke.common.annotation.RepeatSubmit;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.service.PurchaseMatchService;

@RestController
@RequestMapping("/purchase")
public class PurchaseMatchController extends BaseController
{
    @Autowired
    private PurchaseMatchService service;

    @PreAuthorize("@ss.hasPermi('purchase:supplier:list')")
    @GetMapping("/supplier/list")
    public TableDataInfo supplierList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listSuppliers(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:list')")
    @GetMapping("/supplier/options")
    public AjaxResult supplierOptions(@RequestParam Map<String, Object> query)
    {
        return success(service.listSuppliers(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:list')")
    @GetMapping("/supplier/{id}")
    public AjaxResult supplier(@PathVariable Long id)
    {
        return success(service.getSupplier(id));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:add')")
    @Log(title = "采购供应商", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/supplier")
    public AjaxResult addSupplier(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.addSupplier(data, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:edit')")
    @Log(title = "采购供应商", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/supplier")
    public AjaxResult updateSupplier(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateSupplier(data, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:remove')")
    @Log(title = "采购供应商", businessType = BusinessType.DELETE)
    @DeleteMapping("/supplier/{ids}")
    public AjaxResult deleteSupplier(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteSupplier(ids, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:list')")
    @GetMapping("/supplier/alias/list")
    public TableDataInfo aliasList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listAliases(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:add')")
    @Log(title = "采购供应商别名", businessType = BusinessType.INSERT)
    @PostMapping("/supplier/alias")
    public AjaxResult addAlias(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.addAlias(data, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:edit')")
    @Log(title = "采购供应商别名", businessType = BusinessType.UPDATE)
    @PutMapping("/supplier/alias")
    public AjaxResult updateAlias(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateAlias(data, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:supplier:remove')")
    @Log(title = "采购供应商别名", businessType = BusinessType.DELETE)
    @DeleteMapping("/supplier/alias/{ids}")
    public AjaxResult deleteAlias(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteAlias(ids, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:list')")
    @GetMapping("/manual/list")
    public TableDataInfo manualList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listManual(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:list')")
    @GetMapping("/manual/{id}")
    public AjaxResult manual(@PathVariable Long id)
    {
        return success(service.getManual(id));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:add')")
    @Log(title = "采购手工订单", businessType = BusinessType.INSERT)
    @PostMapping("/manual")
    public AjaxResult addManual(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.addManual(data, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:edit')")
    @Log(title = "采购手工订单", businessType = BusinessType.UPDATE)
    @PutMapping("/manual")
    public AjaxResult updateManual(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateManual(data, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:remove')")
    @Log(title = "采购手工订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/manual/{ids}")
    public AjaxResult deleteManual(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteManual(ids, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:list')")
    @GetMapping("/summary/list")
    public TableDataInfo summaryList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listSummary(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:list')")
    @GetMapping("/summary/stats")
    public AjaxResult summaryStats(@RequestParam Map<String, Object> query)
    {
        return success(service.summaryStats(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:list')")
    @GetMapping("/summary/{id}")
    public AjaxResult summary(@PathVariable Long id)
    {
        return success(service.getSummary(id));
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:edit')")
    @Log(title = "采购汇总", businessType = BusinessType.UPDATE)
    @PutMapping("/summary")
    public AjaxResult updateSummary(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateSummary(data, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:remove')")
    @Log(title = "采购汇总", businessType = BusinessType.DELETE)
    @DeleteMapping("/summary/{ids}")
    public AjaxResult deleteSummary(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteSummary(ids, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:match:execute')")
    @Log(title = "采购自动匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/summary/{id}/match")
    public AjaxResult matchOne(@PathVariable Long id)
    {
        return toAjax(service.matchOne(id, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:match:execute')")
    @Log(title = "采购批量重新匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/summary/rematch-failed")
    public AjaxResult rematchFailed(@RequestBody Map<String, Object> query)
    {
        return success(service.rematchFailed(query, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:match:manual')")
    @Log(title = "采购手工匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/summary/{summaryId}/manual-bind/{manualOrderId}")
    public AjaxResult manualBind(@PathVariable Long summaryId, @PathVariable Long manualOrderId)
    {
        return toAjax(service.manualBind(summaryId, manualOrderId, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasAnyPermi('purchase:manual:import,purchase:summary:import')")
    @PostMapping("/import/preview")
    public AjaxResult preview(@RequestParam String importType, @RequestParam(required = false) Long supplierId, @RequestParam MultipartFile file) throws Exception
    {
        return success(service.preview(importType, supplierId, file));
    }

    @PreAuthorize("@ss.hasAnyPermi('purchase:manual:import,purchase:summary:import')")
    @Log(title = "采购Excel导入", businessType = BusinessType.IMPORT)
    @RepeatSubmit
    @PostMapping("/import/confirm")
    public AjaxResult confirmImport(@RequestParam String importType, @RequestParam(required = false) Long supplierId,
                                    @RequestParam(required = false) String sheetName, @RequestParam MultipartFile file) throws Exception
    {
        return success(service.importExcel(importType, supplierId, file, sheetName, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:import')")
    @GetMapping("/manual/template")
    public void manualTemplate(HttpServletResponse response) throws Exception
    {
        service.downloadTemplate("MANUAL_ORDER", response);
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:import')")
    @GetMapping("/summary/template")
    public void summaryTemplate(HttpServletResponse response) throws Exception
    {
        service.downloadTemplate("PURCHASE_SUMMARY", response);
    }

    @PreAuthorize("@ss.hasPermi('purchase:import:query')")
    @GetMapping("/import/batch/list")
    public TableDataInfo batchList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listImportBatches(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:import:query')")
    @GetMapping("/import/batch/{id}")
    public AjaxResult batch(@PathVariable Long id)
    {
        return success(service.getImportBatch(id));
    }

    @PreAuthorize("@ss.hasPermi('purchase:import:query')")
    @GetMapping("/import/detail/list")
    public TableDataInfo importDetails(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listImportDetails(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:conflict:list')")
    @GetMapping("/conflict/list")
    public TableDataInfo conflicts(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listConflicts(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:conflict:resolve')")
    @Log(title = "采购冲突处理", businessType = BusinessType.UPDATE)
    @PostMapping("/conflict/{id}/resolve/{action}")
    public AjaxResult resolve(@PathVariable Long id, @PathVariable String action)
    {
        return toAjax(service.resolveConflict(id, action, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('purchase:match:execute')")
    @GetMapping("/match/log/list")
    public TableDataInfo matchLogs(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(service.listMatchLogs(query));
    }

    @PreAuthorize("@ss.hasPermi('purchase:manual:export')")
    @Log(title = "采购手工订单导出", businessType = BusinessType.EXPORT)
    @PostMapping("/manual/export")
    public void exportManual(@RequestBody Map<String, Object> query, HttpServletResponse response) throws Exception
    {
        service.exportManual(query, response);
    }

    @PreAuthorize("@ss.hasPermi('purchase:summary:export')")
    @Log(title = "采购汇总导出", businessType = BusinessType.EXPORT)
    @PostMapping("/summary/export")
    public void exportSummary(@RequestBody Map<String, Object> query, HttpServletResponse response) throws Exception
    {
        service.exportSummary(query, response);
    }

}
