package com.xinke.erp.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xinke.erp.config.LiveScheduleGate;
import com.xinke.erp.service.LiveAutoAccessService;

@Component
public class LiveAutoAccessTask {
    private static final Logger log=LoggerFactory.getLogger(LiveAutoAccessTask.class);
    private final LiveAutoAccessService service;
    private final LiveScheduleGate gate;
    public LiveAutoAccessTask(LiveAutoAccessService service,LiveScheduleGate gate) { this.service=service; this.gate=gate; }
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { reconcile(); }
    @Scheduled(cron="0 10 * * * ?",zone="Asia/Shanghai")
    public void reconcile() {
        if(!gate.isActive()) return;
        try { service.reconcileAll(); }
        catch(Exception ex) { log.error("自动页面权限同步失败，请检查数据库迁移",ex); }
    }
}
