package com.xinke.erp.livecrm.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.livecrm.service.LiveCrmService;

/** Daily safety net for customers that need a phone-service handoff. */
@Component
public class LiveCrmHandoffTask {
    private final LiveCrmService service;

    public LiveCrmHandoffTask(LiveCrmService service) {
        this.service = service;
    }

    @Scheduled(cron = "${live.audience-v2.handoff-cron:0 15 0 * * ?}")
    public void handoffEligibleOpportunities() {
        service.processPhoneHandoffs();
    }
}
