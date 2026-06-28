package com.ruoyi.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.erp.domain.FinanceDayProfit;
import com.ruoyi.erp.service.IBiDashboardService;

@RestController
@RequestMapping("/finance/profit")
public class FinanceReportController extends BaseController
{
    @Autowired
    private IBiDashboardService dashboardService;

    @PreAuthorize("@ss.hasPermi('finance:profit:daily')")
    @GetMapping("/daily")
    public TableDataInfo daily(FinanceDayProfit query)
    {
        startPage();
        List<FinanceDayProfit> list = dashboardService.selectDailyProfitList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:profit:monthly')")
    @GetMapping("/monthly")
    public TableDataInfo monthly(FinanceDayProfit query)
    {
        startPage();
        List<FinanceDayProfit> list = dashboardService.selectMonthlyProfitList(query);
        return getDataTable(list);
    }
}
