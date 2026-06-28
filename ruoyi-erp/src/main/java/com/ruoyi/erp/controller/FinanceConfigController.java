package com.ruoyi.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.erp.service.IFinanceCoreService;

@RestController
@RequestMapping("/finance/config")
public class FinanceConfigController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @PreAuthorize("@ss.hasPermi('finance:config:list')")
    @GetMapping("/period/list")
    public TableDataInfo periodList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectPeriodList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:config:list')")
    @GetMapping("/subject/list")
    public TableDataInfo subjectList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectSubjectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:config:list')")
    @GetMapping("/fee-type/list")
    public TableDataInfo feeTypeList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectFeeTypeList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:config:list')")
    @GetMapping("/platform-account/list")
    public TableDataInfo platformAccountList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectPlatformAccountList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:config:list')")
    @GetMapping("/bank-account/list")
    public TableDataInfo bankAccountList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectBankAccountList(query);
        return getDataTable(list);
    }
}
