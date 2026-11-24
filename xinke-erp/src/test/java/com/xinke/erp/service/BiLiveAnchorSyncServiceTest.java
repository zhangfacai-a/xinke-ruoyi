package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.erp.mapper.BiLiveAnchorMapper;

@ExtendWith(MockitoExtension.class)
class BiLiveAnchorSyncServiceTest
{
    @Mock BiLiveAnchorMapper mapper;

    @Test
    void fallbackSyncUpsertsChangesAndMarksMissingRowsDeleted()
    {
        when(mapper.upsertRecentSegments()).thenReturn(3);
        when(mapper.markMissingDeleted()).thenReturn(1);
        assertEquals(4,new BiLiveAnchorSyncService(mapper).syncRecent());
    }

    @Test
    void reconciliationResultIsReturnedUnchanged()
    {
        Map<String,Object> expected=Map.ofEntries(
            Map.entry("matched",true),Map.entry("sourceCount",37),Map.entry("biCount",37),
            Map.entry("sourceWatch",1200),Map.entry("biWatch",1200),
            Map.entry("sourceInteraction",300),Map.entry("biInteraction",300),
            Map.entry("sourceNewFan",80),Map.entry("biNewFan",80),
            Map.entry("sourcePresale",12),Map.entry("biPresale",12));
        when(mapper.reconcile()).thenReturn(expected);
        assertSame(expected,new BiLiveAnchorSyncService(mapper).reconcile());
    }

    @Test
    void voidEventSoftDeletesFactRowAfterCommit()
    {
        BiLiveAnchorSyncService service=org.mockito.Mockito.mock(BiLiveAnchorSyncService.class);
        BiLiveAnchorSyncListener listener=new BiLiveAnchorSyncListener(service);
        listener.afterCommit(new BiLiveAnchorChangedEvent(12L,true));
        verify(service).markDeleted(12L);
    }

    @Test
    void afterCommitListenerContainsBiFailure()
    {
        BiLiveAnchorSyncService service=org.mockito.Mockito.mock(BiLiveAnchorSyncService.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("BI unavailable")).when(service).syncSegment(9L);
        BiLiveAnchorSyncListener listener=new BiLiveAnchorSyncListener(service);
        assertDoesNotThrow(()->listener.afterCommit(new BiLiveAnchorChangedEvent(9L,false)));
        verify(service).syncSegment(9L);
    }
}
