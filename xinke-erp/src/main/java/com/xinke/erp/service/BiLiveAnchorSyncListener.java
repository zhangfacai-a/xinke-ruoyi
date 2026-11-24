package com.xinke.erp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BiLiveAnchorSyncListener
{
    private static final Logger log=LoggerFactory.getLogger(BiLiveAnchorSyncListener.class);
    private final BiLiveAnchorSyncService service;

    public BiLiveAnchorSyncListener(BiLiveAnchorSyncService service){this.service=service;}

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void afterCommit(BiLiveAnchorChangedEvent event)
    {
        try
        {
            if(event.deleted())service.markDeleted(event.segmentId());
            else service.syncSegment(event.segmentId());
        }
        catch(Exception exception)
        {
            // BI is a derived read model. A sync failure must never turn a committed report into a user-visible failure.
            log.error("主播区间 BI 即时同步失败，segmentId={}",event.segmentId(),exception);
        }
    }
}
