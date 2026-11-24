package com.xinke.erp.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.erp.config.LiveScheduleGate;

@Component
public class FreightAccessTask {
    private static final Logger log = LoggerFactory.getLogger(FreightAccessTask.class);
    private final JdbcTemplate jdbc;
    private final LiveScheduleGate gate;

    public FreightAccessTask(JdbcTemplate jdbc, LiveScheduleGate gate) {
        this.jdbc = jdbc;
        this.gate = gate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { synchronize(); }

    @Scheduled(cron = "0 10 * * * ?", zone = "Asia/Shanghai")
    public void synchronize() {
        if (!gate.isActive()) return;
        try { reconcile(); }
        catch (Exception ex) { log.error("运费页面权限同步失败", ex); }
    }

    @Transactional
    public void reconcile() {
        jdbc.update("insert ignore into sys_user_role(user_id,role_id) "
            + "select distinct u.user_id,r.role_id from live_subject_user_map m "
            + "join sys_user u on u.user_id=m.user_id and u.status='0' and u.del_flag='0' "
            + "join erp_shop s on s.shop_id=m.subject_id and s.status='0' "
            + "join sys_role r on r.role_key='auto_live_freight' and r.status='0' and r.del_flag='0' "
            + "where m.subject_type='SHOP' and m.role_code in ('customer_service','customer_admin')");
        jdbc.update("delete ur from sys_user_role ur join sys_role r on r.role_id=ur.role_id "
            + "where r.role_key='auto_live_freight' and not exists ("
            + "select 1 from live_subject_user_map m "
            + "join sys_user u on u.user_id=m.user_id and u.status='0' and u.del_flag='0' "
            + "join erp_shop s on s.shop_id=m.subject_id and s.status='0' "
            + "where m.user_id=ur.user_id and m.subject_type='SHOP' "
            + "and m.role_code in ('customer_service','customer_admin'))");
    }
}
