package com.ruoyi.erp.task;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.erp.service.IErpEtlService;

@Component
public class ErpEtlTask
{
    @Autowired
    private IErpEtlService etlService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void buildYesterdayDwdOrder()
    {
        etlService.buildOrderWideTable(new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000));
    }
}
