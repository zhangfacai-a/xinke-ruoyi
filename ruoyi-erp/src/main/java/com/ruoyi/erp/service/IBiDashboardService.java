package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.erp.domain.BiMetric;
import com.ruoyi.erp.domain.FinanceDayProfit;

public interface IBiDashboardService
{
    Map<String, Object> selectProfitSummary(FinanceDayProfit query);
    List<BiMetric> selectGmvTrend(FinanceDayProfit query);
    List<BiMetric> selectProfitTrend(FinanceDayProfit query);
    List<BiMetric> selectTopProducts(FinanceDayProfit query);
    List<FinanceDayProfit> selectDailyProfitList(FinanceDayProfit query);
    List<FinanceDayProfit> selectMonthlyProfitList(FinanceDayProfit query);
}
