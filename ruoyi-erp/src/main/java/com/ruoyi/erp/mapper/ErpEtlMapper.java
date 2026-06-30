package com.ruoyi.erp.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ErpEtlMapper
{
    int deleteDwdOrderItems(@Param("dt") Date dt);
    int buildDwdOrderItems(@Param("dt") Date dt);
    int rebuildDwsFinanceDayProfit(@Param("dt") Date dt);
    int deleteOrderProfitAttribution(@Param("dt") Date dt);
    int buildOrderProfitAttribution(@Param("dt") Date dt);
    int rebuildOperatorProfitDay(@Param("dt") Date dt);
}
