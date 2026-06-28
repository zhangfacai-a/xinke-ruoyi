package com.ruoyi.erp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.domain.FinanceDayProfit;
import com.ruoyi.erp.service.IBiDashboardService;

@RestController
@RequestMapping("/bi")
public class BiDashboardController extends BaseController
{
    @Autowired
    private IBiDashboardService dashboardService;

    @PreAuthorize("@ss.hasPermi('bi:profit:summary')")
    @GetMapping("/profit/summary")
    public AjaxResult profitSummary(FinanceDayProfit query)
    {
        return success(dashboardService.selectProfitSummary(query));
    }

    @PreAuthorize("@ss.hasPermi('bi:shop:sale')")
    @GetMapping("/shop/sale")
    public AjaxResult shopSale(FinanceDayProfit query)
    {
        return success(dashboardService.selectGmvTrend(query));
    }

    @PreAuthorize("@ss.hasPermi('bi:product:rank')")
    @GetMapping("/product/rank")
    public AjaxResult productRank(FinanceDayProfit query)
    {
        return success(dashboardService.selectTopProducts(query));
    }

    @PreAuthorize("@ss.hasPermi('bi:profit:summary')")
    @GetMapping("/profit/trend")
    public AjaxResult profitTrend(FinanceDayProfit query)
    {
        return success(dashboardService.selectProfitTrend(query));
    }
}
