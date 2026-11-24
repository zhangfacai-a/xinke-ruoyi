package com.xinke.erp.livecrm.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.livecrm.service.LiveCrmService;
import com.xinke.erp.config.LiveScheduleGate;

/** Daily safety net for customers that need a phone-service handoff. */
@Component
public class LiveCrmHandoffTask {
    private final LiveCrmService service;
    private final LiveScheduleGate scheduleGate;

    public LiveCrmHandoffTask(LiveCrmService service,LiveScheduleGate scheduleGate) {
        this.service = service;
        this.scheduleGate = scheduleGate;
    }

    @Scheduled(cron = "${live.audience-v2.handoff-cron:0 15 0 * * ?}")
    public void handoffEligibleOpportunities() {
        if(!scheduleGate.isActive())return;
        service.processPhoneHandoffs();
    }
}
