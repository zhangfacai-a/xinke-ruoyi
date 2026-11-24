package com.xinke.erp.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BiLiveAnchorSyncTaskTest
{
    @Test
    void acceptsCommonJdbcBooleanRepresentations()
    {
        assertTrue(BiLiveAnchorSyncTask.matched(true));
        assertTrue(BiLiveAnchorSyncTask.matched(1));
        assertTrue(BiLiveAnchorSyncTask.matched(1L));
        assertTrue(BiLiveAnchorSyncTask.matched("1"));
        assertFalse(BiLiveAnchorSyncTask.matched(0));
        assertFalse(BiLiveAnchorSyncTask.matched(null));
    }
}
