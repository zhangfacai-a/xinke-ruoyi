package com.ruoyi.erp.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.BiMetric;
import com.ruoyi.erp.domain.FinanceDayProfit;
import com.ruoyi.erp.mapper.BiDashboardMapper;
import com.ruoyi.erp.service.IBiDashboardService;

@Service
public class BiDashboardServiceImpl implements IBiDashboardService
{
    @Autowired
    private BiDashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> selectProfitSummary(FinanceDayProfit query)
    {
        Map<String, Object> result = new HashMap<>();
        for (BiMetric metric : dashboardMapper.selectProfitSummary(query))
        {
            result.put(metric.getName(), metric.getValue());
        }
        return result;
    }

    @Override
    public List<BiMetric> selectGmvTrend(FinanceDayProfit query) { return dashboardMapper.selectGmvTrend(query); }
    @Override
    public List<BiMetric> selectProfitTrend(FinanceDayProfit query) { return dashboardMapper.selectProfitTrend(query); }
    @Override
    public List<BiMetric> selectTopProducts(FinanceDayProfit query) { return dashboardMapper.selectTopProducts(query); }
    @Override
    public List<FinanceDayProfit> selectDailyProfitList(FinanceDayProfit query) { return dashboardMapper.selectDailyProfitList(query); }
    @Override
    public List<FinanceDayProfit> selectMonthlyProfitList(FinanceDayProfit query) { return dashboardMapper.selectMonthlyProfitList(query); }
    @Override
    public List<FinanceDayProfit> selectOperatorDailyProfitList(FinanceDayProfit query) { return dashboardMapper.selectOperatorDailyProfitList(query); }
    @Override
    public List<FinanceDayProfit> selectOperatorMonthlyProfitList(FinanceDayProfit query) { return dashboardMapper.selectOperatorMonthlyProfitList(query); }
}
