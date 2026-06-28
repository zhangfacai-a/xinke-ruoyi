package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.BiMetric;
import com.ruoyi.erp.domain.FinanceDayProfit;

@Mapper
public interface BiDashboardMapper
{
    List<BiMetric> selectProfitSummary(FinanceDayProfit query);
    List<BiMetric> selectGmvTrend(FinanceDayProfit query);
    List<BiMetric> selectProfitTrend(FinanceDayProfit query);
    List<BiMetric> selectTopProducts(FinanceDayProfit query);
    List<FinanceDayProfit> selectDailyProfitList(FinanceDayProfit query);
    List<FinanceDayProfit> selectMonthlyProfitList(FinanceDayProfit query);
}
