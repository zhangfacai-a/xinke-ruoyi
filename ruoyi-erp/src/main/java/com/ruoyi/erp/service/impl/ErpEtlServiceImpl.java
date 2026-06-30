package com.ruoyi.erp.service.impl;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.erp.mapper.ErpEtlMapper;
import com.ruoyi.erp.service.IErpEtlService;

@Service
public class ErpEtlServiceImpl implements IErpEtlService
{
    @Autowired
    private ErpEtlMapper etlMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int buildOrderWideTable(Date dt)
    {
        etlMapper.deleteDwdOrderItems(dt);
        int rows = etlMapper.buildDwdOrderItems(dt);
        etlMapper.rebuildDwsFinanceDayProfit(dt);
        etlMapper.deleteOrderProfitAttribution(dt);
        etlMapper.buildOrderProfitAttribution(dt);
        etlMapper.rebuildOperatorProfitDay(dt);
        return rows;
    }
}
