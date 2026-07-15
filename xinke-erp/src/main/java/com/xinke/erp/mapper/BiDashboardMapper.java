package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.BiMetric;
import com.xinke.erp.domain.FinanceDayProfit;

@Mapper
public interface BiDashboardMapper
{
    List<BiMetric> selectProfitSummary(FinanceDayProfit query);
    List<BiMetric> selectGmvTrend(FinanceDayProfit query);
    List<BiMetric> selectProfitTrend(FinanceDayProfit query);
    List<BiMetric> selectTopProducts(FinanceDayProfit query);
    List<FinanceDayProfit> selectDailyProfitList(FinanceDayProfit query);
    List<FinanceDayProfit> selectMonthlyProfitList(FinanceDayProfit query);
    List<FinanceDayProfit> selectOperatorDailyProfitList(FinanceDayProfit query);
    List<FinanceDayProfit> selectOperatorMonthlyProfitList(FinanceDayProfit query);
}
