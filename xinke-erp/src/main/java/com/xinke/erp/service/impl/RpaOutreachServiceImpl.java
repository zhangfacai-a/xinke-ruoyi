package com.xinke.erp.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.RpaBatchRequest;
import com.xinke.erp.domain.RpaRoomBindingRequest;
import com.xinke.erp.domain.RpaShopConfigRequest;
import com.xinke.erp.domain.RpaTaskClaimRequest;
import com.xinke.erp.domain.RpaTaskResultRequest;
import com.xinke.erp.domain.RpaTrackingConfigRequest;
import com.xinke.erp.domain.RpaViewerTrackingRequest;
import com.xinke.erp.domain.RpaBlacklistRequest;
import com.xinke.erp.domain.RpaMessageTemplateRequest;
import com.xinke.erp.mapper.RpaOutreachMapper;
import com.xinke.erp.service.IRpaOutreachService;

@Service
public class RpaOutreachServiceImpl implements IRpaOutreachService
{
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int MAX_BATCH_SIZE = 10;
    private static final Set<String> MESSAGE_SCENES = Set.of("NO_PURCHASE", "REFUND");
    private static final Set<String> MESSAGE_VARIABLES = Set.of("{{nickname}}", "{{shopName}}", "{{liveRoomName}}");
    private static volatile boolean schemaReady;

    @Autowired
    private RpaOutreachMapper rpaOutreachMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${live.rpa.lease-minutes:30}")
    private int leaseMinutes;

    @Override
    public void ensureSchema()
    {
        if (schemaReady)
        {
            return;
        }
        synchronized (RpaOutreachServiceImpl.class)
        {
            if (schemaReady)
            {
                return;
            }
            rpaOutreachMapper.createShopConfigTable();
            rpaOutreachMapper.createRoomShopTable();
            rpaOutreachMapper.createTaskTable();
            rpaOutreachMapper.createBatchTable();
            rpaOutreachMapper.createTrackingConfigTable();
            rpaOutreachMapper.createViewerTrackingRuleTable();
            rpaOutreachMapper.createViewerBlacklistTable();
            rpaOutreachMapper.createMarketingSuppressionTable();
            rpaOutreachMapper.initializeTrackingConfig();
            rpaOutreachMapper.upgradeDefaultTrackingLookback();
            rpaOutreachMapper.upgradeRoomShopKeyLength();
            rpaOutreachMapper.upgradeTaskSecUidLength();
            upgradeExecutionSchema();
            schemaReady = true;
        }
    }

    @Override
    public Map<String, Object> health()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ready", true);
        data.put("leaseMinutes", safeLeaseMinutes());
        data.put("maxBatchSize", MAX_BATCH_SIZE);
        data.put("configuredShopCount", rpaOutreachMapper.selectShopConfigList().size());
        data.put("unmappedRoomCount", rpaOutreachMapper.selectUnmappedRooms().size());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reclaimExpiredLeases()
    {
        cleanupExpiredLeases();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> claim(RpaTaskClaimRequest request)
    {
        cleanupExpiredLeases();
        String workerId = trim(request.getWorkerId(), 128);
        String preferredShopCode = trim(request.getPreferredShopCode(), 64);
        String activeBatchNo = rpaOutreachMapper.selectActiveBatchNo(workerId, preferredShopCode);
        if (StringUtils.hasText(activeBatchNo))
        {
            Map<String, Object> activeBatch = hydrateShopConfig(rpaOutreachMapper.selectBatch(activeBatchNo));
            Map<String, Object> heartbeat = new HashMap<>();
            heartbeat.put("batchNo", activeBatchNo);
            heartbeat.put("leaseToken", activeBatch.get("leaseToken"));
            heartbeat.put("workerId", workerId);
            heartbeat.put("leaseMinutes", safeLeaseMinutes());
            if (rpaOutreachMapper.heartbeatBatch(heartbeat) > 0)
            {
                rpaOutreachMapper.heartbeatTasks(heartbeat);
                return claimedWork(activeBatch, true);
            }
            cleanupExpiredLeases();
        }
        rpaOutreachMapper.removeIneligiblePendingTasks();
        rpaOutreachMapper.prepareTasks();

        int requestedLimit = request.getLimit() == null ? DEFAULT_BATCH_SIZE : request.getLimit();
        requestedLimit = Math.max(1, Math.min(MAX_BATCH_SIZE, requestedLimit));
        Map<String, Object> shop = rpaOutreachMapper.selectClaimableShop(preferredShopCode);
        if (shop == null || shop.isEmpty())
        {
            return noWork(60, "当前没有可领取任务，请检查店铺和直播间绑定");
        }

        int dailyRemaining = asInt(shop.get("dailyRemaining"));
        int limit = Math.min(requestedLimit, Math.max(dailyRemaining, 0));
        if (limit <= 0)
        {
            return noWork(300, "该店铺今日任务额度已用完");
        }

        String batchNo = "RPA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
        String leaseToken = UUID.randomUUID().toString().replace("-", "");
        int safeLeaseMinutes = safeLeaseMinutes();
        Map<String, Object> batchData = new HashMap<>();
        batchData.put("batchNo", batchNo);
        batchData.put("shopConfigId", shop.get("shopConfigId"));
        batchData.put("workerId", workerId);
        batchData.put("leaseToken", leaseToken);
        batchData.put("leaseMinutes", safeLeaseMinutes);
        if (rpaOutreachMapper.insertBatch(batchData) <= 0)
        {
            return noWork(5, "该店铺刚被其他影刀实例领取，请稍后重试");
        }

        Long shopConfigId = asLong(shop.get("shopConfigId"));
        List<Long> taskIds = rpaOutreachMapper.selectPendingTaskIds(shopConfigId, limit);
        if (taskIds == null || taskIds.isEmpty())
        {
            RpaBatchRequest release = batchRequest(batchNo, leaseToken, request.getWorkerId());
            release(release);
            return noWork(30, "任务刚被其他实例处理，请稍后重试");
        }

        Map<String, Object> leaseData = new HashMap<>();
        leaseData.put("taskIds", taskIds);
        leaseData.put("batchNo", batchNo);
        leaseData.put("leaseToken", leaseToken);
        leaseData.put("workerId", workerId);
        leaseData.put("leaseMinutes", safeLeaseMinutes);
        int leased = rpaOutreachMapper.leaseTasks(leaseData);
        if (leased != taskIds.size())
        {
            throw new ServiceException("任务领取发生并发冲突，请重试");
        }
        rpaOutreachMapper.updateBatchTaskCount(batchNo, leased);

        Map<String, Object> batch = hydrateShopConfig(rpaOutreachMapper.selectBatch(batchNo));
        return claimedWork(batch, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> heartbeat(RpaBatchRequest request)
    {
        Map<String, Object> data = batchValues(request);
        data.put("leaseMinutes", safeLeaseMinutes());
        if (rpaOutreachMapper.heartbeatBatch(data) <= 0)
        {
            throw new ServiceException("批次不存在、租约已过期或租约信息不匹配");
        }
        int tasks = rpaOutreachMapper.heartbeatTasks(data);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", rpaOutreachMapper.selectBatch(request.getBatchNo()));
        result.put("activeTaskCount", tasks);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitResult(RpaTaskResultRequest request)
    {
        String requestId = trim(request.getRequestId(), 64);
        String taskNo = trim(request.getTaskNo(), 64);
        Map<String, Object> duplicate = rpaOutreachMapper.selectTaskByRequestId(requestId);
        if (duplicate != null && !duplicate.isEmpty())
        {
            if (!taskNo.equals(str(duplicate.get("taskNo"))))
            {
                throw new ServiceException("requestId已被其他任务使用");
            }
            return idempotentResult(duplicate);
        }

        Map<String, Object> task = rpaOutreachMapper.selectTaskForUpdate(taskNo);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        if (requestId.equals(str(task.get("resultRequestId"))))
        {
            return idempotentResult(task);
        }
        validateLease(task, request);

        String outcome = trim(request.getOutcome(), 32).toUpperCase(Locale.ROOT);
        boolean followed = Boolean.TRUE.equals(request.getFollowed());
        boolean messaged = Boolean.TRUE.equals(request.getMessaged());
        String douyinNo = trim(request.getDouyinNo(), 128);
        String orderNo = trim(request.getOrderNo(), 128);
        if (("ORDERED".equals(outcome) || "CONTACTED".equals(outcome)) && !StringUtils.hasText(douyinNo))
        {
            throw new ServiceException(outcome + "结果必须返回douyinNo");
        }
        if (List.of("ORDERED", "UNPAID", "PAID", "FULFILLING", "COMPLETED", "REFUNDING",
                "PARTIAL_REFUNDED", "REFUNDED").contains(outcome) && !StringUtils.hasText(orderNo))
        {
            throw new ServiceException("ORDERED结果必须返回orderNo");
        }
        if ("CONTACTED".equals(outcome) && !followed && !messaged)
        {
            throw new ServiceException("CONTACTED结果至少需要followed或messaged为true");
        }
        String resultCode = trim(request.getResultCode(), 64);
        String errorMessage = trim(request.getErrorMessage(), 1000);
        if ("SKIPPED".equals(outcome) && !StringUtils.hasText(resultCode))
        {
            throw new ServiceException("SKIPPED结果必须返回resultCode");
        }
        if (("FAILED".equals(outcome) || "RETRYABLE_ERROR".equals(outcome))
                && !StringUtils.hasText(resultCode) && !StringUtils.hasText(errorMessage))
        {
            throw new ServiceException(outcome + "结果必须返回resultCode或errorMessage");
        }

        int attempts = asInt(task.get("attemptCount"));
        int maxAttempts = asInt(task.get("maxAttempts"));
        String status = resultStatus(outcome, attempts, maxAttempts);
        String orderStatus = trim(request.getOrderStatus(), 32).toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(orderStatus) && isTransactionOutcome(outcome)) orderStatus = outcome;
        boolean marketingSuppressed = isMarketingSuppressedOutcome(outcome, orderStatus);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("taskId", task.get("taskId"));
        resultData.put("requestId", requestId);
        resultData.put("status", status);
        resultData.put("outcome", outcome);
        resultData.put("douyinNo", douyinNo);
        resultData.put("orderNo", orderNo);
        resultData.put("orderStatus", orderStatus);
        resultData.put("orderTime", request.getOrderTime());
        resultData.put("refundStatus", trim(request.getRefundStatus(), 32).toUpperCase(Locale.ROOT));
        resultData.put("refundReason", trim(request.getRefundReason(), 255));
        resultData.put("refundTime", request.getRefundTime());
        resultData.put("refundAmount", request.getRefundAmount());
        resultData.put("marketingSuppressed", marketingSuppressed ? 1 : 0);
        resultData.put("followed", followed ? 1 : 0);
        resultData.put("messaged", messaged ? 1 : 0);
        resultData.put("messageContent", trim(request.getMessageContent(), 1000));
        resultData.put("resultCode", resultCode);
        resultData.put("errorMessage", errorMessage);
        if (rpaOutreachMapper.updateTaskResult(resultData) <= 0)
        {
            throw new ServiceException("任务状态已变化，结果未写入");
        }

        updateLeadFromResult(task, request, outcome, followed, messaged, orderNo);
        if (marketingSuppressed)
        {
            saveMarketingSuppression(task, request, outcome, orderStatus, orderNo);
        }
        rpaOutreachMapper.refreshBatchProgress(request.getBatchNo());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("idempotent", false);
        result.put("taskNo", taskNo);
        result.put("status", status);
        result.put("outcome", outcome);
        result.put("orderStatus", orderStatus);
        result.put("marketingSuppressed", marketingSuppressed);
        result.put("batch", rpaOutreachMapper.selectBatch(request.getBatchNo()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> release(RpaBatchRequest request)
    {
        Map<String, Object> data = batchValues(request);
        int releasedTasks = rpaOutreachMapper.releaseBatchTasks(data);
        int releasedBatch = rpaOutreachMapper.markBatchReleased(data);
        if (releasedBatch <= 0)
        {
            throw new ServiceException("批次不存在、已结束或租约信息不匹配");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("released", true);
        result.put("releasedTaskCount", releasedTasks);
        result.put("batchNo", request.getBatchNo());
        return result;
    }

    @Override
    public List<Map<String, Object>> listShopConfigs()
    {
        return rpaOutreachMapper.selectShopConfigList().stream().map(this::hydrateShopConfig).toList();
    }

    @Override
    public List<Map<String, Object>> listUnmappedRooms()
    {
        return rpaOutreachMapper.selectUnmappedRooms();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveShopConfig(Long shopConfigId, RpaShopConfigRequest request)
    {
        Map<String, Object> data = shopConfigValues(request);
        if (shopConfigId == null)
        {
            rpaOutreachMapper.insertShopConfig(data);
            Long createdId = asLong(data.get("shopConfigId"));
            if (createdId == null)
            {
                throw new ServiceException("店铺配置创建失败");
            }
            return createdId;
        }
        if (rpaOutreachMapper.selectShopConfigById(shopConfigId) == null)
        {
            throw new ServiceException("店铺配置不存在");
        }
        Map<String, Object> existing = rpaOutreachMapper.selectShopConfigById(shopConfigId);
        data.put("shopCode", existing.get("shopCode"));
        data.put("shopConfigId", shopConfigId);
        if (rpaOutreachMapper.updateShopConfig(data) <= 0)
        {
            throw new ServiceException("店铺配置更新失败");
        }
        return shopConfigId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindRooms(Long shopConfigId, RpaRoomBindingRequest request)
    {
        if (rpaOutreachMapper.selectShopConfigById(shopConfigId) == null)
        {
            throw new ServiceException("店铺配置不存在");
        }
        List<String> roomKeys = request.getRoomKeys().stream()
                .map(value -> trim(value, 128))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        rpaOutreachMapper.deleteRoomBindings(shopConfigId);
        if (roomKeys.isEmpty())
        {
            return 0;
        }
        return rpaOutreachMapper.upsertRoomBindings(shopConfigId, roomKeys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int mapRoomToShop(String roomKey, Long shopConfigId)
    {
        String normalizedRoomKey = trim(roomKey, 128);
        if (!StringUtils.hasText(normalizedRoomKey)) throw new ServiceException("roomKey不能为空");
        if (shopConfigId == null || rpaOutreachMapper.selectShopConfigById(shopConfigId) == null)
        {
            throw new ServiceException("店铺配置不存在");
        }
        return rpaOutreachMapper.upsertRoomBindings(shopConfigId, List.of(normalizedRoomKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unmapRoom(String roomKey)
    {
        String normalizedRoomKey = trim(roomKey, 128);
        if (!StringUtils.hasText(normalizedRoomKey)) throw new ServiceException("roomKey不能为空");
        return rpaOutreachMapper.deleteRoomBinding(normalizedRoomKey);
    }

    @Override
    public Map<String, Object> getTrackingConfig()
    {
        Map<String, Object> config = rpaOutreachMapper.selectTrackingConfig();
        if (config == null || config.isEmpty())
        {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("enabled", true);
            defaults.put("lookbackDays", 2);
            return defaults;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", asInt(config.get("enabled")) == 1);
        result.put("lookbackDays", Math.max(1, asInt(config.get("lookbackDays"))));
        result.put("updateTime", config.get("updateTime"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTrackingConfig(RpaTrackingConfigRequest request)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        data.put("lookbackDays", request.getLookbackDays());
        if (rpaOutreachMapper.updateTrackingConfig(data) <= 0)
        {
            throw new ServiceException("自动追踪设置保存失败");
        }
        rpaOutreachMapper.removeIneligiblePendingTasks();
        return getTrackingConfig();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateViewerTracking(RpaViewerTrackingRequest request)
    {
        List<Long> viewerIds = request.getViewerIds().stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .toList();
        if (viewerIds.isEmpty())
        {
            throw new ServiceException("viewerIds不能为空");
        }
        String mode = trim(request.getMode(), 16).toUpperCase(Locale.ROOT);
        if (!List.of("AUTO", "INCLUDE", "EXCLUDE").contains(mode))
        {
            throw new ServiceException("不支持的追踪模式：" + mode);
        }

        rpaOutreachMapper.deleteViewerTrackingRules(viewerIds);
        if (!"AUTO".equals(mode))
        {
            Map<String, Object> data = new HashMap<>();
            data.put("viewerIds", viewerIds);
            data.put("mode", mode);
            data.put("remark", trim(request.getRemark(), 500));
            rpaOutreachMapper.insertViewerTrackingRules(data);
        }
        rpaOutreachMapper.deletePendingTasksByViewerIds(viewerIds);
        return viewerIds.size();
    }

    @Override
    public List<Map<String, Object>> listWorkbench(Map<String, Object> query)
    {
        cleanupExpiredLeases();
        Map<String, Object> normalized = workbenchQuery(query);
        String view = str(normalized.get("view")).toUpperCase(Locale.ROOT);
        return switch (view)
        {
            case "BLACKLIST" -> rpaOutreachMapper.selectWorkbenchBlacklist(normalized);
            case "PENDING", "LEASED", "HISTORY" -> rpaOutreachMapper.selectWorkbenchTasks(normalized);
            default -> rpaOutreachMapper.selectWorkbenchCandidates(normalized);
        };
    }

    @Override
    public Map<String, Object> workbenchStats(Map<String, Object> query)
    {
        cleanupExpiredLeases();
        return rpaOutreachMapper.selectWorkbenchStats(workbenchQuery(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int enqueueViewers(List<Long> requestedViewerIds)
    {
        List<Long> viewerIds = cleanViewerIds(requestedViewerIds);
        rpaOutreachMapper.deleteViewerTrackingRules(viewerIds);
        Map<String, Object> rule = new HashMap<>();
        rule.put("viewerIds", viewerIds);
        rule.put("mode", "INCLUDE");
        rule.put("remark", "从影刀任务池手工加入");
        rpaOutreachMapper.insertViewerTrackingRules(rule);
        int restored = rpaOutreachMapper.resetCancelledTasksForViewerIds(viewerIds);
        return restored + rpaOutreachMapper.prepareTasksForViewerIds(viewerIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int blacklistViewers(RpaBlacklistRequest request, Long userId, String username)
    {
        List<Long> viewerIds = cleanViewerIds(request.getViewerIds());
        String scope = trim(request.getScope(), 16).toUpperCase(Locale.ROOT);
        if (!List.of("GLOBAL", "SHOP").contains(scope))
        {
            throw new ServiceException("不支持的黑名单范围：" + scope);
        }
        if ("SHOP".equals(scope) && request.getShopConfigId() == null)
        {
            throw new ServiceException("店铺黑名单必须选择店铺");
        }
        String scopeKey = "GLOBAL".equals(scope) ? "GLOBAL" : "SHOP:" + request.getShopConfigId();
        Map<String, Object> data = new HashMap<>();
        data.put("viewerIds", viewerIds);
        data.put("scopeKey", scopeKey);
        data.put("shopConfigId", request.getShopConfigId());
        data.put("reason", trim(request.getReason(), 64));
        data.put("remark", trim(request.getRemark(), 500));
        data.put("operatorId", userId);
        data.put("operatorName", trim(username, 64));
        rpaOutreachMapper.insertViewerBlacklist(data);
        rpaOutreachMapper.cancelBlacklistedTasks(data);
        rpaOutreachMapper.closeBatchesWithoutActiveTasks();
        return viewerIds.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreBlacklist(List<Long> blacklistIds)
    {
        List<Long> ids = blacklistIds == null ? List.of() : blacklistIds.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) throw new ServiceException("blacklistIds不能为空");
        return rpaOutreachMapper.deleteBlacklistByIds(ids);
    }

    private Map<String, Object> workbenchQuery(Map<String, Object> source)
    {
        Map<String, Object> query = new HashMap<>(source == null ? Map.of() : source);
        query.put("view", trim(str(query.getOrDefault("view", "CANDIDATE")), 16).toUpperCase(Locale.ROOT));
        if (!StringUtils.hasText(str(query.get("beginDate")))) query.put("beginDate", LocalDate.now().minusDays(1).toString());
        if (!StringUtils.hasText(str(query.get("endDate")))) query.put("endDate", LocalDate.now().toString());
        return query;
    }

    private List<Long> cleanViewerIds(List<Long> requestedViewerIds)
    {
        List<Long> viewerIds = requestedViewerIds == null ? List.of() : requestedViewerIds.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .toList();
        if (viewerIds.isEmpty()) throw new ServiceException("viewerIds不能为空");
        return viewerIds;
    }

    private void cleanupExpiredLeases()
    {
        rpaOutreachMapper.releaseExpiredTasks();
        rpaOutreachMapper.expireBatches();
    }

    private void validateLease(Map<String, Object> task, RpaBatchRequest request)
    {
        if (!"leased".equals(str(task.get("status")))
                || !trim(request.getBatchNo(), 64).equals(str(task.get("batchNo")))
                || !trim(request.getLeaseToken(), 64).equals(str(task.get("leaseToken")))
                || !trim(request.getWorkerId(), 128).equals(str(task.get("workerId"))))
        {
            throw new ServiceException("任务租约信息不匹配或任务已结束");
        }
        Object expiresAt = task.get("leaseExpiresAt");
        if (expiresAt instanceof Date date && date.before(new Date()))
        {
            throw new ServiceException("任务租约已过期，请重新领取");
        }
    }

    private void updateLeadFromResult(Map<String, Object> task, RpaTaskResultRequest request, String outcome,
            boolean followed, boolean messaged, String orderNo)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("leadId", task.get("leadId"));
        data.put("viewerId", task.get("viewerId"));
        data.put("workerId", trim(request.getWorkerId(), 128));
        if (List.of("ORDERED", "UNPAID", "PAID", "FULFILLING", "COMPLETED").contains(outcome))
        {
            data.put("orderNo", orderNo);
            data.put("remark", "影刀查单确认已下单");
            rpaOutreachMapper.markViewerOrdered(data);
            data.put("followContent", "影刀查单：已下单，订单号 " + orderNo);
            data.put("followResult", "ordered");
            rpaOutreachMapper.insertOutreachFollowRecord(data);
        }
        else if ("CONTACTED".equals(outcome))
        {
            rpaOutreachMapper.markViewerContacted(data);
            String content = trim(request.getMessageContent(), 1000);
            if (!StringUtils.hasText(content))
            {
                content = "影刀执行：" + (followed ? "已关注" : "未关注") + "，" + (messaged ? "已私信" : "未私信");
            }
            data.put("followContent", content);
            data.put("followResult", messaged ? "messaged" : "followed");
            rpaOutreachMapper.insertOutreachFollowRecord(data);
        }
    }

    private String resultStatus(String outcome, int attempts, int maxAttempts)
    {
        return switch (outcome)
        {
            case "ORDERED", "PAID", "FULFILLING", "COMPLETED" -> "ordered";
            case "UNPAID" -> "unpaid";
            case "REFUNDING", "PARTIAL_REFUNDED", "REFUNDED" -> "suppressed";
            case "CANCELLED", "NOT_ORDERED" -> "skipped";
            case "ORDER_QUERY_FAILED" -> attempts >= maxAttempts ? "failed" : "pending";
            case "CONTACTED" -> "contacted";
            case "SKIPPED" -> "skipped";
            case "FAILED" -> "failed";
            case "RETRYABLE_ERROR" -> attempts >= maxAttempts ? "failed" : "pending";
            default -> throw new ServiceException("不支持的outcome：" + outcome);
        };
    }

    private Map<String, Object> noWork(int retryAfterSeconds, String reason)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", false);
        result.put("retryAfterSeconds", retryAfterSeconds);
        result.put("reason", reason);
        result.put("tasks", new ArrayList<>());
        return result;
    }

    private Map<String, Object> claimedWork(Map<String, Object> batch, boolean resumed)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", true);
        result.put("resumed", resumed);
        result.put("batch", claimBatchView(batch));
        result.put("tasks", enrichTasks(rpaOutreachMapper.selectBatchTasks(str(batch.get("batchNo"))), batch));
        result.put("leaseSeconds", safeLeaseMinutes() * 60);
        result.put("heartbeatAfterSeconds", Math.max(60, safeLeaseMinutes() * 30));
        return result;
    }

    private Map<String, Object> idempotentResult(Map<String, Object> task)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("idempotent", true);
        result.put("taskNo", task.get("taskNo"));
        result.put("status", task.get("status"));
        result.put("outcome", task.get("outcome"));
        result.put("orderNo", task.get("orderNo"));
        return result;
    }

    private Map<String, Object> batchValues(RpaBatchRequest request)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("batchNo", trim(request.getBatchNo(), 64));
        data.put("leaseToken", trim(request.getLeaseToken(), 64));
        data.put("workerId", trim(request.getWorkerId(), 128));
        return data;
    }

    private RpaBatchRequest batchRequest(String batchNo, String leaseToken, String workerId)
    {
        RpaBatchRequest request = new RpaBatchRequest();
        request.setBatchNo(batchNo);
        request.setLeaseToken(leaseToken);
        request.setWorkerId(workerId);
        return request;
    }

    private Map<String, Object> shopConfigValues(RpaShopConfigRequest request)
    {
        Map<String, Object> data = new HashMap<>();
        String shopName = trim(request.getShopName(), 100);
        String generatedCode = "SHOP_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        data.put("shopCode", StringUtils.hasText(request.getShopCode()) ? trim(request.getShopCode(), 64) : generatedCode);
        data.put("shopName", shopName);
        data.put("douyinAccountCode", trim(request.getDouyinAccountCode(), 128));
        data.put("douyinShopName", shopName);
        List<Map<String, Object>> templates = normalizeTemplates(request.getMessageTemplates());
        data.put("messageTemplatesJson", writeJson(templates));
        data.put("messageTemplate", templates.stream().filter(item -> Boolean.TRUE.equals(item.get("defaultTemplate")))
                .map(item -> str(item.get("content"))).findFirst().orElse(str(templates.get(0).get("content"))));
        data.put("dailyLimit", request.getDailyLimit() == null ? 100 : request.getDailyLimit());
        data.put("hourlyLimit", request.getHourlyLimit() == null ? 15 : request.getHourlyLimit());
        data.put("burstSize", request.getBurstSize() == null ? 10 : request.getBurstSize());
        data.put("restMinutes", request.getRestMinutes() == null ? 5 : request.getRestMinutes());
        data.put("allowedStartTime", StringUtils.hasText(request.getAllowedStartTime()) ? request.getAllowedStartTime() : "09:00");
        data.put("allowedEndTime", StringUtils.hasText(request.getAllowedEndTime()) ? request.getAllowedEndTime() : "22:00");
        data.put("refundCooldownDays", request.getRefundCooldownDays() == null ? 90 : request.getRefundCooldownDays());
        data.put("cancelledCooldownDays", request.getCancelledCooldownDays() == null ? 7 : request.getCancelledCooldownDays());
        data.put("pauseOnCaptcha", !Boolean.FALSE.equals(request.getPauseOnCaptcha()) ? 1 : 0);
        data.put("maxConsecutiveFailures", request.getMaxConsecutiveFailures() == null ? 5 : request.getMaxConsecutiveFailures());
        data.put("status", "1".equals(request.getStatus()) ? "1" : "0");
        data.put("remark", trim(request.getRemark(), 500));
        return data;
    }

    private Map<String, Object> hydrateShopConfig(Map<String, Object> source)
    {
        if (source == null) return null;
        Map<String, Object> result = new LinkedHashMap<>(source);
        String json = str(result.remove("messageTemplatesJson"));
        try
        {
            List<Map<String, Object>> templates = StringUtils.hasText(json)
                    ? objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {}) : defaultTemplates();
            List<Map<String, Object>> resolvedTemplates = hasRequiredBusinessScenes(templates)
                    && !isPreviousTwoSceneRecommendedTemplates(templates) ? templates : defaultTemplates();
            result.put("messageTemplates", resolvedTemplates);
            result.put("messageTemplate", resolvedTemplates.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("defaultTemplate")))
                    .map(item -> str(item.get("content"))).findFirst()
                    .orElse(resolvedTemplates.isEmpty() ? "" : str(resolvedTemplates.get(0).get("content"))));
        }
        catch (Exception ex)
        {
            List<Map<String, Object>> templates = defaultTemplates();
            result.put("messageTemplates", templates);
            result.put("messageTemplate", str(templates.get(0).get("content")));
        }
        return result;
    }

    private Map<String, Object> claimBatchView(Map<String, Object> batch)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchNo", batch.get("batchNo"));
        result.put("leaseToken", batch.get("leaseToken"));
        result.put("workerId", batch.get("workerId"));
        result.put("shopName", batch.get("shopName"));
        result.put("douyinAccountCode", batch.get("douyinAccountCode"));
        result.put("taskCount", batch.get("taskCount"));
        result.put("burstSize", batch.get("burstSize"));
        result.put("restMinutes", batch.get("restMinutes"));
        result.put("pauseOnCaptcha", batch.get("pauseOnCaptcha"));
        result.put("maxConsecutiveFailures", batch.get("maxConsecutiveFailures"));
        return result;
    }

    private List<Map<String, Object>> normalizeTemplates(List<RpaMessageTemplateRequest> requested)
    {
        if (requested == null || requested.isEmpty()) return defaultTemplates();
        if (requested.size() > 100) throw new ServiceException("每个店铺最多保存100条私信文案");
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (RpaMessageTemplateRequest item : requested)
        {
            List<String> keywords = normalizeKeywords(item.getKeywords(), item.getTemplateName());
            Map<String, Object> template = new LinkedHashMap<>();
            template.put("templateKey", StringUtils.hasText(item.getTemplateKey()) ? trim(item.getTemplateKey(), 64) : "TPL_" + (++index));
            template.put("templateName", trim(item.getTemplateName(), 100));
            template.put("scene", trim(item.getScene(), 32).toUpperCase(Locale.ROOT));
            template.put("content", trim(item.getContent(), 1000));
            template.put("enabled", !Boolean.FALSE.equals(item.getEnabled()));
            template.put("defaultTemplate", Boolean.TRUE.equals(item.getDefaultTemplate()));
            template.put("priority", item.getPriority() == null ? index * 10 : item.getPriority());
            template.put("keywords", keywords);
            result.add(template);
        }
        if (isLegacyRecommendedTemplates(result)) return defaultTemplates();
        validateTemplateLibrary(result);
        result.forEach(item -> item.put("defaultTemplate", false));
        result.stream().filter(item -> "NO_PURCHASE".equals(item.get("scene")) && Boolean.TRUE.equals(item.get("enabled")))
                .findFirst().ifPresent(item -> item.put("defaultTemplate", true));
        if (!hasRequiredBusinessScenes(result))
        {
            throw new ServiceException("普通跟进和退款关怀都必须至少保留一条启用文案");
        }
        return result;
    }

    private List<String> normalizeKeywords(List<String> requested, String templateName)
    {
        if (requested == null || requested.isEmpty()) return List.of();
        List<String> keywords = requested.stream().map(value -> str(value).trim())
                .filter(StringUtils::hasText).distinct().toList();
        if (keywords.size() > 20) throw new ServiceException("“" + str(templateName) + "”最多设置20个关键词");
        for (String keyword : keywords)
        {
            if (keyword.length() < 2 || keyword.length() > 20)
            {
                throw new ServiceException("关键词“" + keyword + "”应为2至20个字符");
            }
        }
        return keywords;
    }

    private void validateTemplateLibrary(List<Map<String, Object>> templates)
    {
        Set<String> keys = new HashSet<>();
        Set<String> sceneContents = new HashSet<>();
        Map<String, String> sceneKeywordOwners = new HashMap<>();
        Map<String, Integer> sceneCounts = new HashMap<>();
        for (Map<String, Object> template : templates)
        {
            String key = str(template.get("templateKey"));
            String name = str(template.get("templateName")).trim();
            String scene = str(template.get("scene")).toUpperCase(Locale.ROOT);
            String content = str(template.get("content")).trim();
            if (!keys.add(key)) throw new ServiceException("私信模板编号重复：" + key);
            if (!MESSAGE_SCENES.contains(scene)) throw new ServiceException("私信模板“" + name + "”使用了不支持的场景");
            if (!StringUtils.hasText(name) || !StringUtils.hasText(content)) throw new ServiceException("私信模板名称和内容不能为空");
            if (!sceneContents.add(scene + "\n" + content)) throw new ServiceException("“" + name + "”与同场景的其他文案重复");
            if (template.get("keywords") instanceof List<?> keywords)
            {
                for (Object value : keywords)
                {
                    String keyword = str(value).toLowerCase(Locale.ROOT);
                    String owner = sceneKeywordOwners.putIfAbsent(scene + "\n" + keyword, name);
                    if (owner != null) throw new ServiceException("关键词“" + value + "”已被“" + owner + "”使用");
                }
            }
            String textWithoutVariables = content;
            for (String variable : MESSAGE_VARIABLES) textWithoutVariables = textWithoutVariables.replace(variable, "");
            if (textWithoutVariables.contains("{{") || textWithoutVariables.contains("}}"))
            {
                throw new ServiceException("“" + name + "”包含无法识别的变量");
            }
            int count = sceneCounts.merge(scene, 1, Integer::sum);
            if (count > 50) throw new ServiceException("每个私信场景最多保存50条文案");
        }
    }

    private List<Map<String, Object>> defaultTemplates()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        addTemplateSet(result, "NO_PURCHASE", "未购买用户", "NO_PURCHASE", List.of(
                "你好，看到你刚刚来过{{liveRoomName}}，想了解哪款产品呢？有问题可以直接告诉我。",
                "你好，感谢关注{{shopName}}，产品功能、价格或活动方面有疑问都可以问我。",
                "你好，刚才在直播间看到你啦，有哪款产品想进一步了解吗？",
                "你好，感谢来到{{liveRoomName}}，如果还有没来得及问的问题，我可以继续帮你。",
                "你好，直播间里的产品如果还没选好，我可以根据你的需求帮你看看。",
                "你好，感谢你的关注，有产品方面的问题可以直接发给我。",
                "你好，直播间里的内容如果有没听清的地方，我可以再帮你说明。",
                "你好，看到你关注了我们的直播，需要我帮你找合适的产品吗？",
                "你好，感谢来过{{liveRoomName}}，选购方面有疑问可以随时问我。",
                "你好，这里是{{shopName}}，如果你还在比较产品，我可以帮你梳理一下。"), List.of(), 10, true);
        result.add(template("NO_PURCHASE_KEYWORD_PRICE", "价格与优惠咨询", "NO_PURCHASE",
                "你好，看到你比较关注价格和活动，需要我帮你确认当前到手价或可用优惠吗？",
                false, List.of("多少钱", "价格", "优惠", "国补", "补贴", "太贵", "便宜", "活动"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_GIFT", "赠品咨询", "NO_PURCHASE",
                "你好，看到你在问赠品，需要我帮你确认当前套餐包含的赠品和领取条件吗？",
                false, List.of("有什么赠品", "送什么", "送啥", "赠品", "礼品"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_COMPARE", "型号对比", "NO_PURCHASE",
                "你好，看到你在比较不同款式，需要我根据你的使用需求帮你说明区别吗？",
                false, List.of("有什么区别", "什么区别", "区别", "对比", "哪个好", "哪款好", "怎么选"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_SPEC", "参数尺寸咨询", "NO_PURCHASE",
                "你好，看到你比较关注产品参数，需要我帮你确认具体尺寸、配置或容量吗？",
                false, List.of("屏幕多大", "多大屏幕", "内存多大", "屏幕", "内存", "尺寸", "配置", "参数", "容量", "公斤"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_SHIPPING", "发货时效咨询", "NO_PURCHASE",
                "你好，看到你在关注发货时间，需要我帮你确认当前库存和预计发出时间吗？",
                false, List.of("什么时候发货", "延迟发货", "几天到", "多久到", "发货"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_INSTALL", "安装服务咨询", "NO_PURCHASE",
                "你好，看到你在咨询安装服务，需要我帮你确认是否包安装、服务范围和预约方式吗？",
                false, List.of("包安装吗", "包安装", "上门安装", "安装", "上门"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_WARRANTY", "售后保障咨询", "NO_PURCHASE",
                "你好，看到你比较关注售后保障，需要我帮你确认质保、退换或服务规则吗？",
                false, List.of("质保多久", "运费险", "可以试用吗", "质保", "保修", "试用", "售后"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_LINK", "购买链接咨询", "NO_PURCHASE",
                "你好，看到你在找对应商品，需要我帮你确认应该看哪个链接或哪款商品吗？",
                false, List.of("几号链接", "链接在哪", "哪个链接", "拍哪个", "哪个款"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_AGE", "年龄与使用场景", "NO_PURCHASE",
                "你好，看到你在确认适用阶段，可以告诉我使用者的年龄或主要需求，我帮你看看是否合适。",
                false, List.of("几岁", "幼儿园", "小班", "中班", "大班", "一年级", "二年级", "三年级"), 10));
        result.add(template("NO_PURCHASE_KEYWORD_FUNCTION", "具体功能咨询", "NO_PURCHASE",
                "你好，看到你比较关注具体功能，需要我结合你的使用需求帮你确认这款是否合适吗？",
                false, List.of("不要烘干", "带烘干", "洗烘一体", "单洗", "洗烘", "烘干", "英语", "动画片", "功能"), 10));
        addTemplateSet(result, "REFUND", "退款关怀", "REFUND", List.of(
                "你好，看到你的订单正在申请退款，想确认一下是否遇到了什么问题？需要的话我可以帮你跟进。",
                "你好，留意到你的订单有退款申请，如果是产品或服务方面的问题，可以告诉我，我来帮你处理。",
                "你好，看到你提交了退款申请，给你带来不便很抱歉。方便说一下遇到的情况吗？",
                "你好，你的订单退款情况我们已经关注到了，如需查询进度或协助处理，可以直接告诉我。",
                "你好，看到订单进入了退款流程，如果还有未解决的问题，我可以继续帮你核实。",
                "你好，关于这次退款，如果是使用、发货或商品方面的问题，可以告诉我具体情况。",
                "你好，留意到你的售后申请了，需要协助确认退款进度或处理方案吗？",
                "你好，很抱歉这次购物没有达到预期。退款过程中如果需要帮助，可以直接联系我。",
                "你好，看到你的订单有售后记录，我来确认一下是否还需要我们协助处理。",
                "你好，你的退款申请我们已经留意到了，有任何疑问都可以在这里告诉我，我会帮你跟进。"), List.of(), 20, false);
        return result;
    }

    private void addTemplateSet(List<Map<String, Object>> target, String prefix, String name, String scene,
            List<String> contents, List<String> keywords, int priority, boolean defaultGroup)
    {
        for (int index = 0; index < contents.size(); index++)
        {
            String number = String.format(Locale.ROOT, "%02d", index + 1);
            target.add(template(prefix + "_" + number, name + " " + (index + 1), scene,
                    contents.get(index), defaultGroup && index == 0, keywords, priority));
        }
    }

    private boolean isLegacyRecommendedTemplates(List<Map<String, Object>> templates)
    {
        if (templates == null || templates.isEmpty() || templates.size() > 60) return false;
        Set<String> oldGroups = Set.of("GENERAL", "COMMENT", "PRODUCT", "PROMOTION", "AFTER_SALES", "FALLBACK");
        return templates.stream().allMatch(item -> {
            String key = str(item.get("templateKey"));
            if (oldGroups.contains(key)) return true;
            int separator = key.lastIndexOf('_');
            return separator > 0 && oldGroups.contains(key.substring(0, separator))
                    && key.substring(separator + 1).matches("\\d{2}");
        });
    }

    private boolean isPreviousTwoSceneRecommendedTemplates(List<Map<String, Object>> templates)
    {
        if (templates == null || templates.size() != 20) return false;
        return templates.stream().allMatch(item -> {
            String key = str(item.get("templateKey"));
            boolean recommendedKey = key.matches("NO_PURCHASE_\\d{2}") || key.matches("REFUND_\\d{2}");
            return recommendedKey && !hasKeywords(item);
        });
    }

    private boolean hasRequiredBusinessScenes(List<Map<String, Object>> templates)
    {
        if (templates == null || templates.isEmpty()) return false;
        boolean hasGeneralNoPurchase = templates.stream()
                .anyMatch(item -> !Boolean.FALSE.equals(item.get("enabled"))
                        && "NO_PURCHASE".equals(str(item.get("scene")).toUpperCase(Locale.ROOT))
                        && !hasKeywords(item));
        boolean hasRefund = templates.stream()
                .anyMatch(item -> !Boolean.FALSE.equals(item.get("enabled"))
                        && "REFUND".equals(str(item.get("scene")).toUpperCase(Locale.ROOT)));
        return hasGeneralNoPurchase && hasRefund;
    }

    private Map<String, Object> template(String key, String name, String scene, String content,
            boolean defaultTemplate, List<String> keywords, int priority)
    {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("templateKey", key); value.put("templateName", name); value.put("scene", scene);
        value.put("content", content); value.put("enabled", true); value.put("defaultTemplate", defaultTemplate);
        value.put("keywords", keywords); value.put("priority", priority);
        return value;
    }

    private String writeJson(Object value)
    {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { throw new ServiceException("私信模板保存失败"); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> enrichTasks(List<Map<String, Object>> tasks, Map<String, Object> shop)
    {
        List<Map<String, Object>> templates = shop != null && shop.get("messageTemplates") instanceof List<?> list
                ? (List<Map<String, Object>>) list : defaultTemplates();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : tasks)
        {
            String commentText = StringUtils.hasText(str(source.get("recentCommentContent")))
                    ? str(source.get("recentCommentContent")) : str(source.get("lastCommentContent"));
            Map<String, Object> noPurchase = messageOption(selectTemplate(templates, "NO_PURCHASE", commentText), source, shop);
            Map<String, Object> refund = messageOption(selectTemplate(templates, "REFUND", commentText), source, shop);
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("NO_PURCHASE", noPurchase);
            options.put("REFUND", refund);

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("taskNo", source.get("taskNo"));
            task.put("nickname", source.get("nickname"));
            task.put("secUid", source.get("secUid"));
            task.put("profileUrl", source.get("profileUrl"));
            task.put("messageOptions", options);
            result.add(task);
        }
        return result;
    }

    private Map<String, Object> messageOption(Map<String, Object> template, Map<String, Object> task,
            Map<String, Object> shop)
    {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("templateKey", template.get("templateKey"));
        option.put("templateName", template.get("templateName"));
        option.put("content", renderTemplate(str(template.get("content")), task, shop));
        return option;
    }

    private Map<String, Object> selectTemplate(List<Map<String, Object>> templates, String scene, String comment)
    {
        List<Map<String, Object>> enabled = templates.stream()
                .filter(item -> !Boolean.FALSE.equals(item.get("enabled"))).toList();
        List<Map<String, Object>> sceneTemplates = enabled.stream()
                .filter(item -> scene.equals(str(item.get("scene")).toUpperCase(Locale.ROOT))).toList();
        if (!StringUtils.hasText(comment)) return randomTemplateWithoutKeywords(sceneTemplates, scene);
        List<Map<String, Object>> matches = new ArrayList<>();
        int longestKeywordLength = 0;
        for (Map<String, Object> item : sceneTemplates)
        {
            String matchedKeyword = longestMatchedKeyword(item, comment);
            if (matchedKeyword == null) continue;
            Map<String, Object> matched = new LinkedHashMap<>(item);
            matched.put("matchedKeyword", matchedKeyword);
            int length = matchedKeyword.length();
            if (length > longestKeywordLength)
            {
                matches.clear();
                longestKeywordLength = length;
            }
            if (length == longestKeywordLength) matches.add(matched);
        }
        if (!matches.isEmpty()) return randomTemplate(matches);
        return randomTemplateWithoutKeywords(sceneTemplates, scene);
    }

    @SuppressWarnings("unchecked")
    private String longestMatchedKeyword(Map<String, Object> template, String comment)
    {
        if (!(template.get("keywords") instanceof List<?> values)) return null;
        String normalizedComment = comment.toLowerCase(Locale.ROOT);
        return values.stream().map(String::valueOf).map(String::trim).filter(StringUtils::hasText)
                .filter(keyword -> normalizedComment.contains(keyword.toLowerCase(Locale.ROOT)))
                .max(java.util.Comparator.comparingInt(String::length)).orElse(null);
    }

    private Map<String, Object> randomTemplateWithoutKeywords(List<Map<String, Object>> templates, String scene)
    {
        List<Map<String, Object>> general = templates.stream().filter(item -> !hasKeywords(item)).toList();
        if (!general.isEmpty()) return randomTemplate(general);
        if (!templates.isEmpty()) return randomTemplate(templates);
        List<Map<String, Object>> defaults = defaultTemplates().stream()
                .filter(item -> scene.equals(item.get("scene")) && !hasKeywords(item)).toList();
        return defaults.isEmpty() ? defaultTemplates().get(0) : randomTemplate(defaults);
    }

    private boolean hasKeywords(Map<String, Object> template)
    {
        return template.get("keywords") instanceof List<?> values && values.stream().anyMatch(value -> StringUtils.hasText(str(value)));
    }

    private Map<String, Object> randomTemplate(List<Map<String, Object>> templates)
    {
        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private String renderTemplate(String content, Map<String, Object> task, Map<String, Object> shop)
    {
        return content.replace("{{nickname}}", str(task.get("nickname")))
                .replace("{{comment}}", str(task.get("lastCommentContent")))
                .replace("{{shopName}}", shop == null ? "" : str(shop.get("shopName")))
                .replace("{{liveRoomName}}", str(task.get("liveRoomName")))
                .replace("{{douyinNo}}", str(task.get("douyinNo"))).replaceAll("\\{\\{[^}]+}}", "").trim();
    }

    private boolean isTransactionOutcome(String outcome)
    {
        return List.of("UNPAID", "PAID", "FULFILLING", "COMPLETED", "CANCELLED", "REFUNDING",
                "PARTIAL_REFUNDED", "REFUNDED").contains(outcome);
    }

    private boolean isMarketingSuppressedOutcome(String outcome, String orderStatus)
    {
        return List.of("ORDERED", "UNPAID", "PAID", "FULFILLING", "COMPLETED", "REFUNDING",
                "PARTIAL_REFUNDED", "REFUNDED").contains(outcome)
                || List.of("UNPAID", "PAID", "FULFILLING", "COMPLETED", "REFUNDING",
                        "PARTIAL_REFUNDED", "REFUNDED").contains(orderStatus);
    }

    private void saveMarketingSuppression(Map<String, Object> task, RpaTaskResultRequest request,
            String outcome, String orderStatus, String orderNo)
    {
        Map<String, Object> shop = rpaOutreachMapper.selectShopConfigById(asLong(task.get("shopConfigId")));
        int cooldownDays = shop == null ? 90 : Math.max(1, asInt(shop.get("refundCooldownDays")));
        boolean refund = List.of("REFUNDING", "PARTIAL_REFUNDED", "REFUNDED").contains(outcome)
                || StringUtils.hasText(request.getRefundStatus());
        Map<String, Object> data = new HashMap<>();
        data.put("viewerId", task.get("viewerId"));
        data.put("shopConfigId", task.get("shopConfigId"));
        data.put("suppressionType", refund ? "REFUND_AFTER_SALES" : "PURCHASED");
        data.put("orderNo", orderNo);
        data.put("orderStatus", orderStatus);
        data.put("refundStatus", trim(request.getRefundStatus(), 32).toUpperCase(Locale.ROOT));
        data.put("refundReason", trim(request.getRefundReason(), 255));
        data.put("suppressionUntil", refund ? Date.from(LocalDateTime.now().plusDays(cooldownDays)
                .atZone(ZoneId.systemDefault()).toInstant()) : null);
        data.put("manualReviewRequired", 1);
        rpaOutreachMapper.upsertMarketingSuppression(data);
    }

    private void upgradeExecutionSchema()
    {
        ensureShopColumn("message_templates_json", "message_templates_json text default null after message_template");
        ensureShopColumn("hourly_limit", "hourly_limit int not null default 15 after daily_limit");
        ensureShopColumn("burst_size", "burst_size int not null default 10 after hourly_limit");
        ensureShopColumn("rest_minutes", "rest_minutes int not null default 5 after burst_size");
        ensureShopColumn("allowed_start_time", "allowed_start_time varchar(5) not null default '09:00' after rest_minutes");
        ensureShopColumn("allowed_end_time", "allowed_end_time varchar(5) not null default '22:00' after allowed_start_time");
        ensureShopColumn("refund_cooldown_days", "refund_cooldown_days int not null default 90 after allowed_end_time");
        ensureShopColumn("cancelled_cooldown_days", "cancelled_cooldown_days int not null default 7 after refund_cooldown_days");
        ensureShopColumn("pause_on_captcha", "pause_on_captcha tinyint not null default 1 after cancelled_cooldown_days");
        ensureShopColumn("max_consecutive_failures", "max_consecutive_failures int not null default 5 after pause_on_captcha");

        ensureTaskColumn("order_status", "order_status varchar(32) default null after order_no");
        ensureTaskColumn("order_time", "order_time datetime default null after order_status");
        ensureTaskColumn("refund_status", "refund_status varchar(32) default null after order_time");
        ensureTaskColumn("refund_reason", "refund_reason varchar(255) default null after refund_status");
        ensureTaskColumn("refund_time", "refund_time datetime default null after refund_reason");
        ensureTaskColumn("refund_amount", "refund_amount decimal(16,2) default null after refund_time");
        ensureTaskColumn("marketing_suppressed", "marketing_suppressed tinyint not null default 0 after refund_amount");
        ensureTaskColumn("message_template_key", "message_template_key varchar(64) default null after marketing_suppressed");
        ensureTaskColumn("message_template_name", "message_template_name varchar(100) default null after message_template_key");
    }

    private void ensureShopColumn(String columnName, String definition)
    {
        if (rpaOutreachMapper.selectColumnExists("dy_rpa_shop_config", columnName) == 0)
        {
            rpaOutreachMapper.addShopConfigColumn(definition);
        }
    }

    private void ensureTaskColumn(String columnName, String definition)
    {
        if (rpaOutreachMapper.selectColumnExists("dy_rpa_outreach_task", columnName) == 0)
        {
            rpaOutreachMapper.addTaskColumn(definition);
        }
    }

    private int safeLeaseMinutes()
    {
        return Math.max(5, Math.min(120, leaseMinutes));
    }

    private Long asLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    private int asInt(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private String str(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String trim(String value, int maxLength)
    {
        String text = value == null ? "" : value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
