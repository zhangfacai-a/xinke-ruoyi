package com.xinke.erp.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xinke.erp.service.ILiveGiftService;

@Component
public class DingTalkUserSyncTask
{
    private static final Logger log = LoggerFactory.getLogger(DingTalkUserSyncTask.class);

    @Autowired
    private ILiveGiftService liveGiftService;

    @Scheduled(cron = "${live.dingtalk.sync-cron:0 30 2 * * ?}")
    public void syncUsers()
    {
        try
        {
            liveGiftService.syncDingTalk("dingtalk-scheduler");
        }
        catch (Exception ex)
        {
            log.error("钉钉系统用户定时同步失败", ex);
        }
    }
}
