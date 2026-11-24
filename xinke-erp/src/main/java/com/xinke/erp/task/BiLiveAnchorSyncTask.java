package com.xinke.erp.task;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.service.BiLiveAnchorSyncService;
import com.xinke.erp.config.LiveScheduleGate;

@Component
public class BiLiveAnchorSyncTask
{
    private static final Logger log=LoggerFactory.getLogger(BiLiveAnchorSyncTask.class);
    private final BiLiveAnchorSyncService service;
    private final LiveScheduleGate scheduleGate;

    public BiLiveAnchorSyncTask(BiLiveAnchorSyncService service,LiveScheduleGate scheduleGate){this.service=service;this.scheduleGate=scheduleGate;}

    @Scheduled(cron="${live.metrics.bi-sync-cron:0 */5 * * * ?}")
    public void syncRecent()
    {
        if(!scheduleGate.isActive())return;
        try
        {
            int changed=service.syncRecent();
            if(changed>0)log.info("主播区间 BI 兜底同步完成，影响行数={}",changed);
        }
        catch(Exception exception){log.error("主播区间 BI 兜底同步失败",exception);}
    }

    @Scheduled(cron="${live.metrics.bi-reconcile-cron:0 20 3 * * ?}")
    public void reconcile()
    {
        if(!scheduleGate.isActive())return;
        try
        {
            Map<String,Object> result=service.reconcile();
            if(!matched(result.get("matched")))log.error("主播区间 BI 每日对账不一致：{}",result);
            else log.info("主播区间 BI 每日对账通过：{}",result);
        }
        catch(Exception exception){log.error("主播区间 BI 每日对账失败",exception);}
    }

    static boolean matched(Object value)
    {
        if(value instanceof Boolean bool)return bool;
        if(value instanceof Number number)return number.intValue()==1;
        return "1".equals(String.valueOf(value))||"true".equalsIgnoreCase(String.valueOf(value));
    }
}
