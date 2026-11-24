package com.xinke.erp.task;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.mapper.BiLiveRetentionMapper;
import com.xinke.erp.config.LiveScheduleGate;

@Component
public class BiLiveRetentionSyncTask {
    private static final Logger log=LoggerFactory.getLogger(BiLiveRetentionSyncTask.class);
    private final BiLiveRetentionMapper mapper;
    private final LiveScheduleGate scheduleGate;

    public BiLiveRetentionSyncTask(BiLiveRetentionMapper mapper,LiveScheduleGate scheduleGate){this.mapper=mapper;this.scheduleGate=scheduleGate;}

    @Scheduled(cron="${live.retention.bi-sync-cron:0 0 0,12 * * ?}",zone="Asia/Shanghai")
    public void sync() {
        if(!scheduleGate.isActive())return;
        try {
            int changed=mapper.upsertChanged()+mapper.markMissingDeleted();
            if(changed>0)log.info("挽单转推 BI 明细同步完成，影响行数={}",changed);
            reconcile();
        } catch(Exception exception){log.error("挽单转推 BI 明细同步失败",exception);}
    }

    private void reconcile() {
        try {
            Map<String,Object> result=mapper.reconcile();
            if(!matched(result.get("matched")))log.error("挽单转推 BI 明细对账不一致：{}",result);
            else log.info("挽单转推 BI 明细对账通过：{}",result);
        } catch(Exception exception){log.error("挽单转推 BI 明细对账失败",exception);}
    }

    private static boolean matched(Object value) {
        if(value instanceof Boolean bool)return bool;
        if(value instanceof Number number)return number.intValue()==1;
        return "1".equals(String.valueOf(value))||"true".equalsIgnoreCase(String.valueOf(value));
    }
}
