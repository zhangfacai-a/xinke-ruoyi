package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.RpaRoomBindingRequest;
import com.xinke.erp.domain.RpaTaskClaimRequest;
import com.xinke.erp.domain.RpaTaskResultRequest;
import com.xinke.erp.domain.RpaTrackingConfigRequest;
import com.xinke.erp.domain.RpaViewerTrackingRequest;
import com.xinke.erp.mapper.RpaOutreachMapper;

@ExtendWith(MockitoExtension.class)
class RpaOutreachServiceImplTest
{
    @Mock
    private RpaOutreachMapper rpaOutreachMapper;

    @InjectMocks
    private RpaOutreachServiceImpl service;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(service, "leaseMinutes", 30);
    }

    @Test
    void claimReturnsOneShopBatchWithRequestedTasks()
    {
        when(rpaOutreachMapper.selectClaimableShop("")).thenReturn(Map.of(
                "shopConfigId", 3L,
                "dailyRemaining", 20));
        when(rpaOutreachMapper.insertBatch(any())).thenReturn(1);
        when(rpaOutreachMapper.selectPendingTaskIds(3L, 10)).thenReturn(List.of(11L, 12L));
        when(rpaOutreachMapper.leaseTasks(any())).thenReturn(2);
        when(rpaOutreachMapper.selectBatch(anyString())).thenReturn(Map.of("shopConfigId", 3L));
        when(rpaOutreachMapper.selectBatchTasks(anyString())).thenReturn(List.of(
                Map.of("taskNo", "TASK-11"),
                Map.of("taskNo", "TASK-12")));

        RpaTaskClaimRequest request = new RpaTaskClaimRequest();
        request.setWorkerId("worker-1");
        request.setLimit(10);

        Map<String, Object> result = service.claim(request);

        assertTrue((Boolean) result.get("available"));
        assertEquals(2, ((List<?>) result.get("tasks")).size());
        assertEquals(1800, result.get("leaseSeconds"));
        assertEquals(900, result.get("heartbeatAfterSeconds"));

        ArgumentCaptor<Map<String, Object>> lease = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).leaseTasks(lease.capture());
        assertEquals("worker-1", lease.getValue().get("workerId"));
        assertEquals(List.of(11L, 12L), lease.getValue().get("taskIds"));
    }

    @Test
    void claimReturnsUnavailableWhenNoShopHasWork()
    {
        RpaTaskClaimRequest request = new RpaTaskClaimRequest();
        request.setWorkerId("worker-1");

        Map<String, Object> result = service.claim(request);

        assertFalse((Boolean) result.get("available"));
        assertEquals(60, result.get("retryAfterSeconds"));
        assertTrue(((List<?>) result.get("tasks")).isEmpty());
        verify(rpaOutreachMapper, never()).insertBatch(any());
    }

    @Test
    void orderedResultUpdatesTaskAndLead()
    {
        when(rpaOutreachMapper.selectTaskForUpdate("TASK-1")).thenReturn(leasedTask());
        when(rpaOutreachMapper.updateTaskResult(any())).thenReturn(1);
        when(rpaOutreachMapper.selectBatch("BATCH-1")).thenReturn(Map.of("status", "leased"));

        RpaTaskResultRequest request = resultRequest("ORDERED");
        request.setDouyinNo("douyin-100");
        request.setOrderNo("ORDER-100");

        Map<String, Object> result = service.submitResult(request);

        assertTrue((Boolean) result.get("accepted"));
        assertFalse((Boolean) result.get("idempotent"));
        assertEquals("ordered", result.get("status"));

        ArgumentCaptor<Map<String, Object>> taskResult = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).updateTaskResult(taskResult.capture());
        assertEquals("ordered", taskResult.getValue().get("status"));
        assertEquals("douyin-100", taskResult.getValue().get("douyinNo"));
        assertEquals("ORDER-100", taskResult.getValue().get("orderNo"));
        verify(rpaOutreachMapper).markViewerOrdered(any());
        verify(rpaOutreachMapper).insertOutreachFollowRecord(any());
    }

    @Test
    void contactedResultRecordsFollowAndMessage()
    {
        when(rpaOutreachMapper.selectTaskForUpdate("TASK-1")).thenReturn(leasedTask());
        when(rpaOutreachMapper.updateTaskResult(any())).thenReturn(1);

        RpaTaskResultRequest request = resultRequest("CONTACTED");
        request.setDouyinNo("douyin-100");
        request.setFollowed(true);
        request.setMessaged(true);
        request.setMessageContent("您好，看到您来过直播间");

        Map<String, Object> result = service.submitResult(request);

        assertEquals("contacted", result.get("status"));
        verify(rpaOutreachMapper).markViewerContacted(any());

        ArgumentCaptor<Map<String, Object>> record = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).insertOutreachFollowRecord(record.capture());
        assertEquals("messaged", record.getValue().get("followResult"));
        assertEquals("您好，看到您来过直播间", record.getValue().get("followContent"));
    }

    @Test
    void retryableErrorReturnsTaskToPendingQueue()
    {
        when(rpaOutreachMapper.selectTaskForUpdate("TASK-1")).thenReturn(leasedTask());
        when(rpaOutreachMapper.updateTaskResult(any())).thenReturn(1);

        RpaTaskResultRequest request = resultRequest("RETRYABLE_ERROR");
        request.setResultCode("PAGE_TIMEOUT");
        request.setErrorMessage("页面加载超时");

        Map<String, Object> result = service.submitResult(request);

        assertEquals("pending", result.get("status"));
        ArgumentCaptor<Map<String, Object>> taskResult = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).updateTaskResult(taskResult.capture());
        assertEquals("pending", taskResult.getValue().get("status"));
        verify(rpaOutreachMapper, never()).markViewerOrdered(any());
        verify(rpaOutreachMapper, never()).markViewerContacted(any());
    }

    @Test
    void duplicateRequestReturnsOriginalResultWithoutWritingAgain()
    {
        when(rpaOutreachMapper.selectTaskByRequestId("REQUEST-1")).thenReturn(Map.of(
                "taskNo", "TASK-1",
                "status", "ordered",
                "outcome", "ORDERED",
                "orderNo", "ORDER-100"));

        Map<String, Object> result = service.submitResult(resultRequest("ORDERED"));

        assertTrue((Boolean) result.get("accepted"));
        assertTrue((Boolean) result.get("idempotent"));
        assertEquals("ORDER-100", result.get("orderNo"));
        verify(rpaOutreachMapper, never()).selectTaskForUpdate(anyString());
        verify(rpaOutreachMapper, never()).updateTaskResult(any());
    }

    @Test
    void failedResultRequiresDiagnosticInformation()
    {
        when(rpaOutreachMapper.selectTaskForUpdate("TASK-1")).thenReturn(leasedTask());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitResult(resultRequest("FAILED")));

        assertEquals("FAILED结果必须返回resultCode或errorMessage", error.getMessage());
        verify(rpaOutreachMapper, never()).updateTaskResult(any());
    }

    @Test
    void emptyRoomListClearsExistingBindings()
    {
        when(rpaOutreachMapper.selectShopConfigById(3L)).thenReturn(Map.of("shopConfigId", 3L));
        RpaRoomBindingRequest request = new RpaRoomBindingRequest();
        request.setRoomKeys(List.of());

        int count = service.bindRooms(3L, request);

        assertEquals(0, count);
        verify(rpaOutreachMapper).deleteRoomBindings(3L);
        verify(rpaOutreachMapper, never()).upsertRoomBindings(anyLong(), any());
    }

    @Test
    void trackingConfigIsNormalizedAndPrunesPendingTasks()
    {
        when(rpaOutreachMapper.updateTrackingConfig(any())).thenReturn(1);
        when(rpaOutreachMapper.selectTrackingConfig()).thenReturn(Map.of(
                "enabled", 0,
                "lookbackDays", 7));
        RpaTrackingConfigRequest request = new RpaTrackingConfigRequest();
        request.setEnabled(false);
        request.setLookbackDays(7);

        Map<String, Object> result = service.updateTrackingConfig(request);

        assertEquals(false, result.get("enabled"));
        assertEquals(7, result.get("lookbackDays"));
        verify(rpaOutreachMapper).removeIneligiblePendingTasks();
    }

    @Test
    void viewerExclusionReplacesOverrideAndDropsPendingTasks()
    {
        RpaViewerTrackingRequest request = new RpaViewerTrackingRequest();
        request.setViewerIds(List.of(2L, 3L, 3L));
        request.setMode("EXCLUDE");

        int count = service.updateViewerTracking(request);

        assertEquals(2, count);
        verify(rpaOutreachMapper).deleteViewerTrackingRules(List.of(2L, 3L));
        verify(rpaOutreachMapper).insertViewerTrackingRules(any());
        verify(rpaOutreachMapper).deletePendingTasksByViewerIds(List.of(2L, 3L));
    }

    private Map<String, Object> leasedTask()
    {
        Map<String, Object> task = new HashMap<>();
        task.put("taskId", 1L);
        task.put("taskNo", "TASK-1");
        task.put("viewerId", 2L);
        task.put("leadId", 3L);
        task.put("status", "leased");
        task.put("attemptCount", 1);
        task.put("maxAttempts", 5);
        task.put("batchNo", "BATCH-1");
        task.put("leaseToken", "LEASE-1");
        task.put("workerId", "worker-1");
        task.put("leaseExpiresAt", new Date(System.currentTimeMillis() + 60_000));
        return task;
    }

    private RpaTaskResultRequest resultRequest(String outcome)
    {
        RpaTaskResultRequest request = new RpaTaskResultRequest();
        request.setBatchNo("BATCH-1");
        request.setLeaseToken("LEASE-1");
        request.setWorkerId("worker-1");
        request.setRequestId("REQUEST-1");
        request.setTaskNo("TASK-1");
        request.setOutcome(outcome);
        return request;
    }
}
