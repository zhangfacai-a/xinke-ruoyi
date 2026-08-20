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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.RpaBlacklistRequest;
import com.xinke.erp.domain.RpaMessageTemplateRequest;
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
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void claimReturnsOneShopBatchWithRequestedTasks()
    {
        when(rpaOutreachMapper.selectClaimableShop("")).thenReturn(Map.of(
                "shopConfigId", 3L,
                "dailyRemaining", 20));
        when(rpaOutreachMapper.insertBatch(any())).thenReturn(1);
        when(rpaOutreachMapper.selectPendingTaskIds(3L, 10)).thenReturn(List.of(11L, 12L));
        when(rpaOutreachMapper.leaseTasks(any())).thenReturn(2);
        when(rpaOutreachMapper.selectBatch(anyString())).thenReturn(Map.ofEntries(
                Map.entry("batchNo", "BATCH-1"),
                Map.entry("shopConfigId", 3L),
                Map.entry("workerId", "worker-1"),
                Map.entry("leaseToken", "LEASE-1"),
                Map.entry("shopName", "测试店铺"),
                Map.entry("douyinAccountCode", "测试账号"),
                Map.entry("taskCount", 2),
                Map.entry("burstSize", 10),
                Map.entry("restMinutes", 5),
                Map.entry("pauseOnCaptcha", 1),
                Map.entry("maxConsecutiveFailures", 5)));
        when(rpaOutreachMapper.selectBatchTasks(anyString())).thenReturn(List.of(
                Map.of("taskNo", "TASK-11", "nickname", "用户A", "profileUrl", "https://example.com/a",
                        "recentCommentContent", "什么时候发货", "commentCount", 3),
                Map.of("taskNo", "TASK-12")));

        RpaTaskClaimRequest request = new RpaTaskClaimRequest();
        request.setWorkerId("worker-1");
        request.setLimit(10);

        Map<String, Object> result = service.claim(request);

        assertTrue((Boolean) result.get("available"));
        assertFalse((Boolean) result.get("resumed"));
        assertEquals(2, ((List<?>) result.get("tasks")).size());
        Map<String, Object> firstTask = (Map<String, Object>) ((List<?>) result.get("tasks")).get(0);
        assertEquals(Set.of("taskNo", "nickname", "secUid", "profileUrl", "messageOptions"), firstTask.keySet());
        assertFalse(firstTask.containsKey("recentCommentContent"));
        assertFalse(firstTask.containsKey("commentCount"));
        Map<String, Object> messageOptions = (Map<String, Object>) firstTask.get("messageOptions");
        assertEquals(Set.of("NO_PURCHASE", "REFUND"), messageOptions.keySet());
        assertEquals(Set.of("templateKey", "templateName", "content"),
                ((Map<String, Object>) messageOptions.get("NO_PURCHASE")).keySet());
        assertTrue(String.valueOf(((Map<String, Object>) messageOptions.get("REFUND")).get("templateKey"))
                .startsWith("REFUND_"));
        Map<String, Object> batch = (Map<String, Object>) result.get("batch");
        assertEquals(Set.of("batchNo", "leaseToken", "workerId", "shopName", "douyinAccountCode", "taskCount",
                "burstSize", "restMinutes", "pauseOnCaptcha", "maxConsecutiveFailures"), batch.keySet());
        assertFalse(batch.containsKey("messageTemplates"));
        assertEquals(1800, result.get("leaseSeconds"));
        assertEquals(900, result.get("heartbeatAfterSeconds"));

        ArgumentCaptor<Map<String, Object>> lease = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).leaseTasks(lease.capture());
        assertEquals("worker-1", lease.getValue().get("workerId"));
        assertEquals(List.of(11L, 12L), lease.getValue().get("taskIds"));
    }

    @Test
    void claimReturnsAndRenewsWorkersActiveBatch()
    {
        when(rpaOutreachMapper.selectActiveBatchNo("worker-1", "")).thenReturn("BATCH-ACTIVE");
        when(rpaOutreachMapper.selectBatch("BATCH-ACTIVE")).thenReturn(Map.ofEntries(
                Map.entry("batchNo", "BATCH-ACTIVE"),
                Map.entry("leaseToken", "LEASE-ACTIVE"),
                Map.entry("workerId", "worker-1"),
                Map.entry("shopName", "测试店铺"),
                Map.entry("douyinAccountCode", "测试账号")));
        when(rpaOutreachMapper.heartbeatBatch(any())).thenReturn(1);
        when(rpaOutreachMapper.heartbeatTasks(any())).thenReturn(2);
        when(rpaOutreachMapper.selectBatchTasks("BATCH-ACTIVE")).thenReturn(List.of(
                Map.of("taskNo", "TASK-1"), Map.of("taskNo", "TASK-2")));

        RpaTaskClaimRequest request = new RpaTaskClaimRequest();
        request.setWorkerId("worker-1");
        request.setLimit(10);

        Map<String, Object> result = service.claim(request);

        assertTrue((Boolean) result.get("available"));
        assertTrue((Boolean) result.get("resumed"));
        assertEquals(2, ((List<?>) result.get("tasks")).size());
        verify(rpaOutreachMapper, never()).selectClaimableShop(anyString());
        verify(rpaOutreachMapper, never()).insertBatch(any());
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
    void reclaimExpiredLeasesRequeuesTasksBeforeExpiringBatches()
    {
        service.reclaimExpiredLeases();

        var ordered = org.mockito.Mockito.inOrder(rpaOutreachMapper);
        ordered.verify(rpaOutreachMapper).releaseExpiredTasks();
        ordered.verify(rpaOutreachMapper).expireBatches();
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

    @Test
    void enqueueViewersIncludesDistinctUsersAndRestoresCancelledTasks()
    {
        when(rpaOutreachMapper.resetCancelledTasksForViewerIds(List.of(2L, 3L))).thenReturn(1);
        when(rpaOutreachMapper.prepareTasksForViewerIds(List.of(2L, 3L))).thenReturn(2);

        int count = service.enqueueViewers(List.of(2L, 3L, 2L));

        assertEquals(3, count);
        verify(rpaOutreachMapper).deleteViewerTrackingRules(List.of(2L, 3L));
        ArgumentCaptor<Map<String, Object>> rule = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).insertViewerTrackingRules(rule.capture());
        assertEquals("INCLUDE", rule.getValue().get("mode"));
        assertEquals(List.of(2L, 3L), rule.getValue().get("viewerIds"));
    }

    @Test
    void globalBlacklistCancelsTasksAndClosesEmptyBatches()
    {
        RpaBlacklistRequest request = new RpaBlacklistRequest();
        request.setViewerIds(List.of(2L, 3L, 2L));
        request.setScope("GLOBAL");
        request.setReason("manual");

        int count = service.blacklistViewers(request, 9L, "admin");

        assertEquals(2, count);
        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).insertViewerBlacklist(data.capture());
        assertEquals("GLOBAL", data.getValue().get("scopeKey"));
        assertEquals(List.of(2L, 3L), data.getValue().get("viewerIds"));
        assertEquals(9L, data.getValue().get("operatorId"));
        verify(rpaOutreachMapper).cancelBlacklistedTasks(data.getValue());
        verify(rpaOutreachMapper).closeBatchesWithoutActiveTasks();
    }

    @Test
    void shopBlacklistRequiresShopAndUsesShopScopeKey()
    {
        RpaBlacklistRequest invalid = new RpaBlacklistRequest();
        invalid.setViewerIds(List.of(2L));
        invalid.setScope("SHOP");
        assertThrows(ServiceException.class, () -> service.blacklistViewers(invalid, 9L, "admin"));

        RpaBlacklistRequest request = new RpaBlacklistRequest();
        request.setViewerIds(List.of(2L));
        request.setScope("SHOP");
        request.setShopConfigId(7L);
        service.blacklistViewers(request, 9L, "admin");

        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).insertViewerBlacklist(data.capture());
        assertEquals("SHOP:7", data.getValue().get("scopeKey"));
        assertEquals(7L, data.getValue().get("shopConfigId"));
    }

    @Test
    void restoreBlacklistDeletesDistinctPositiveIds()
    {
        when(rpaOutreachMapper.deleteBlacklistByIds(List.of(4L, 5L))).thenReturn(2);

        assertEquals(2, service.restoreBlacklist(java.util.Arrays.asList(4L, 5L, 4L, null, -1L)));
        verify(rpaOutreachMapper).deleteBlacklistByIds(List.of(4L, 5L));
    }

    @Test
    void workbenchDefaultsToYesterdayThroughToday()
    {
        service.listWorkbench(new HashMap<>());

        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(rpaOutreachMapper).selectWorkbenchCandidates(query.capture());
        assertEquals(java.time.LocalDate.now().minusDays(1).toString(), query.getValue().get("beginDate"));
        assertEquals(java.time.LocalDate.now().toString(), query.getValue().get("endDate"));
        assertEquals("CANDIDATE", query.getValue().get("view"));
    }

    @Test
    void mapSingleRoomDoesNotClearOtherShopBindings()
    {
        when(rpaOutreachMapper.selectShopConfigById(7L)).thenReturn(Map.of("shopConfigId", 7L));
        when(rpaOutreachMapper.upsertRoomBindings(7L, List.of("room-1"))).thenReturn(1);

        assertEquals(1, service.mapRoomToShop("room-1", 7L));
        verify(rpaOutreachMapper).upsertRoomBindings(7L, List.of("room-1"));
        verify(rpaOutreachMapper, never()).deleteRoomBindings(anyLong());
    }

    @Test
    void unmapRoomDeletesOnlyRequestedRoom()
    {
        when(rpaOutreachMapper.deleteRoomBinding("room-1")).thenReturn(1);

        assertEquals(1, service.unmapRoom("room-1"));
        verify(rpaOutreachMapper).deleteRoomBinding("room-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void recommendedTemplateLibraryContainsGeneralAndKeywordTemplatesWithoutCommentPlaceholder()
    {
        List<Map<String, Object>> templates = ReflectionTestUtils.invokeMethod(service, "defaultTemplates");

        assertEquals(30, templates.size());
        assertTrue(templates.stream().noneMatch(item -> String.valueOf(item.get("content")).contains("{{comment}}")));
        Map<String, Long> groupCounts = templates.stream().collect(Collectors.groupingBy(
                item -> String.valueOf(item.get("scene")), Collectors.counting()));
        assertEquals(Set.of("NO_PURCHASE", "REFUND"), groupCounts.keySet());
        assertEquals(20L, groupCounts.get("NO_PURCHASE"));
        assertEquals(10L, groupCounts.get("REFUND"));
        assertEquals(10, templates.stream().filter(item -> item.get("keywords") instanceof List<?> values
                && !values.isEmpty()).count());
    }

    @Test
    @SuppressWarnings("unchecked")
    void legacyTemplateResponseAlsoReplacesOldBatchDefaultMessage() throws Exception
    {
        List<Map<String, Object>> legacyTemplates = List.of(
                Map.of("templateKey", "GENERAL", "content", "旧通用模板", "defaultTemplate", true),
                Map.of("templateKey", "COMMENT", "content", "看到你的评论：{{comment}}"),
                Map.of("templateKey", "PRODUCT", "content", "旧产品模板"),
                Map.of("templateKey", "PROMOTION", "content", "旧优惠模板"),
                Map.of("templateKey", "AFTER_SALES", "content", "旧售后模板"),
                Map.of("templateKey", "FALLBACK", "content", "旧兜底模板"));
        Map<String, Object> source = new HashMap<>();
        source.put("messageTemplate", "看到你的评论：{{comment}}");
        source.put("messageTemplatesJson", new ObjectMapper().writeValueAsString(legacyTemplates));

        Map<String, Object> hydrated = ReflectionTestUtils.invokeMethod(service, "hydrateShopConfig", source);

        assertEquals(30, ((List<Map<String, Object>>) hydrated.get("messageTemplates")).size());
        assertFalse(String.valueOf(hydrated.get("messageTemplate")).contains("{{comment}}"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void templateRandomizationNeverLeavesRequestedBusinessScene()
    {
        List<Map<String, Object>> templates = ReflectionTestUtils.invokeMethod(service, "defaultTemplates");
        Set<String> selectedKeys = new java.util.HashSet<>();

        for (int i = 0; i < 50; i++)
        {
            Map<String, Object> selected = ReflectionTestUtils.invokeMethod(service, "selectTemplate", templates,
                    "REFUND", "");
            String key = String.valueOf(selected.get("templateKey"));
            assertTrue(key.startsWith("REFUND_"));
            selectedKeys.add(key);
        }
        assertTrue(selectedKeys.size() > 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void keywordTemplateUsesLongestMatchingCommentKeywordAndGeneralFallback()
    {
        List<Map<String, Object>> templates = ReflectionTestUtils.invokeMethod(service, "defaultTemplates");

        Map<String, Object> shipping = ReflectionTestUtils.invokeMethod(service, "selectTemplate", templates,
                "NO_PURCHASE", "请问什么时候发货，大概几天到");
        assertEquals("NO_PURCHASE_KEYWORD_SHIPPING", shipping.get("templateKey"));
        assertEquals("什么时候发货", shipping.get("matchedKeyword"));

        for (int i = 0; i < 30; i++)
        {
            Map<String, Object> general = ReflectionTestUtils.invokeMethod(service, "selectTemplate", templates,
                    "NO_PURCHASE", "好的谢谢");
            assertTrue(String.valueOf(general.get("templateKey")).matches("NO_PURCHASE_\\d{2}"));
            assertFalse(general.containsKey("matchedKeyword"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchingCommentCarriesDirectedNoPurchaseAndRefundOptionsTogether()
    {
        List<Map<String, Object>> templates = ReflectionTestUtils.invokeMethod(service, "defaultTemplates");
        Map<String, Object> shop = new HashMap<>();
        shop.put("shopName", "测试店铺");
        shop.put("messageTemplates", templates);
        Map<String, Object> task = new HashMap<>();
        task.put("taskNo", "TASK-KEYWORD");
        task.put("recentCommentContent", "请问什么时候发货");

        List<Map<String, Object>> enriched = ReflectionTestUtils.invokeMethod(service, "enrichTasks",
                List.of(task), shop);
        Map<String, Object> options = (Map<String, Object>) enriched.get(0).get("messageOptions");
        Map<String, Object> noPurchase = (Map<String, Object>) options.get("NO_PURCHASE");
        Map<String, Object> refund = (Map<String, Object>) options.get("REFUND");

        assertEquals("NO_PURCHASE_KEYWORD_SHIPPING", noPurchase.get("templateKey"));
        assertEquals(Set.of("templateKey", "templateName", "content"), noPurchase.keySet());
        assertTrue(String.valueOf(refund.get("templateKey")).startsWith("REFUND_"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void customTemplateLibraryAcceptsOnlyTwoBusinessScenesAndKnownVariables()
    {
        List<RpaMessageTemplateRequest> valid = List.of(
                messageTemplate("CUSTOM_NO_ORDER", "未购买", "NO_PURCHASE", "你好{{nickname}}，这里是{{shopName}}"),
                messageTemplate("CUSTOM_REFUND", "退款关怀", "REFUND", "你好，需要协助处理退款吗？"));

        List<Map<String, Object>> normalized = ReflectionTestUtils.invokeMethod(service, "normalizeTemplates", valid);

        assertEquals(2, normalized.size());
        assertTrue(Boolean.TRUE.equals(normalized.get(0).get("defaultTemplate")));

        List<RpaMessageTemplateRequest> unknownVariable = List.of(
                messageTemplate("A", "未购买", "NO_PURCHASE", "你好{{userName}}"),
                messageTemplate("B", "退款", "REFUND", "需要协助处理退款吗？"));
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "normalizeTemplates", unknownVariable));

        List<RpaMessageTemplateRequest> unsupportedScene = List.of(
                messageTemplate("A", "未购买", "NO_PURCHASE", "你好"),
                messageTemplate("B", "优惠", "PROMOTION", "需要了解优惠吗？"));
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "normalizeTemplates", unsupportedScene));
    }

    @Test
    void duplicateKeywordInSameSceneIsRejected()
    {
        RpaMessageTemplateRequest first = messageTemplate("PRICE_A", "价格咨询一", "NO_PURCHASE", "我来帮你确认当前价格");
        first.setKeywords(List.of("多少钱", "优惠"));
        RpaMessageTemplateRequest second = messageTemplate("PRICE_B", "价格咨询二", "NO_PURCHASE", "我来帮你确认当前活动");
        second.setKeywords(List.of("多少钱", "活动"));
        RpaMessageTemplateRequest refund = messageTemplate("REFUND_A", "退款关怀", "REFUND", "需要协助处理退款吗？");

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "normalizeTemplates", List.of(first, second, refund)));
    }

    @Test
    void templateLibraryWithoutGeneralNoPurchaseFallbackIsRejected()
    {
        RpaMessageTemplateRequest directed = messageTemplate("SHIPPING", "发货咨询", "NO_PURCHASE",
                "我来帮你确认发货时间");
        directed.setKeywords(List.of("什么时候发货"));
        RpaMessageTemplateRequest refund = messageTemplate("REFUND_A", "退款关怀", "REFUND",
                "需要协助处理退款吗？");

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "normalizeTemplates", List.of(directed, refund)));
    }

    private RpaMessageTemplateRequest messageTemplate(String key, String name, String scene, String content)
    {
        RpaMessageTemplateRequest request = new RpaMessageTemplateRequest();
        request.setTemplateKey(key);
        request.setTemplateName(name);
        request.setScene(scene);
        request.setContent(content);
        request.setEnabled(true);
        return request;
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
