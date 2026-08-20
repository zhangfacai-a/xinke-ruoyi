package com.xinke.erp.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.service.IRpaOutreachService;

@Component
public class RpaOutreachLeaseTask
{
    @Autowired
    private IRpaOutreachService rpaOutreachService;

    @Scheduled(initialDelay = 30000, fixedDelay = 60000)
    public void reclaimExpiredLeases()
    {
        rpaOutreachService.reclaimExpiredLeases();
    }
}
