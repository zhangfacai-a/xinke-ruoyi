package com.xinke.erp.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
    public Map<String, Object> claim(RpaTaskClaimRequest request)
    {
        cleanupExpiredLeases();
        rpaOutreachMapper.removeIneligiblePendingTasks();
        rpaOutreachMapper.prepareTasks();

        int requestedLimit = request.getLimit() == null ? DEFAULT_BATCH_SIZE : request.getLimit();
        requestedLimit = Math.max(1, Math.min(MAX_BATCH_SIZE, requestedLimit));
        Map<String, Object> shop = rpaOutreachMapper.selectClaimableShop(trim(request.getPreferredShopCode(), 64));
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
        batchData.put("workerId", trim(request.getWorkerId(), 128));
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
        leaseData.put("workerId", trim(request.getWorkerId(), 128));
        leaseData.put("leaseMinutes", safeLeaseMinutes);
        int leased = rpaOutreachMapper.leaseTasks(leaseData);
        if (leased != taskIds.size())
        {
            throw new ServiceException("任务领取发生并发冲突，请重试");
        }
        rpaOutreachMapper.updateBatchTaskCount(batchNo, leased);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", true);
        Map<String, Object> batch = hydrateShopConfig(rpaOutreachMapper.selectBatch(batchNo));
        result.put("batch", batch);
        result.put("tasks", enrichTasks(rpaOutreachMapper.selectBatchTasks(batchNo), batch));
        result.put("leaseSeconds", safeLeaseMinutes * 60);
        result.put("heartbeatAfterSeconds", Math.max(60, safeLeaseMinutes * 30));
        return result;
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
            result.put("messageTemplates", StringUtils.hasText(json)
                    ? objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {}) : defaultTemplates());
        }
        catch (Exception ex) { result.put("messageTemplates", defaultTemplates()); }
        return result;
    }

    private List<Map<String, Object>> normalizeTemplates(List<RpaMessageTemplateRequest> requested)
    {
        if (requested == null || requested.isEmpty()) return defaultTemplates();
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (RpaMessageTemplateRequest item : requested)
        {
            Map<String, Object> template = new LinkedHashMap<>();
            template.put("templateKey", StringUtils.hasText(item.getTemplateKey()) ? trim(item.getTemplateKey(), 64) : "TPL_" + (++index));
            template.put("templateName", trim(item.getTemplateName(), 100));
            template.put("scene", trim(item.getScene(), 32).toUpperCase(Locale.ROOT));
            template.put("content", trim(item.getContent(), 1000));
            template.put("enabled", !Boolean.FALSE.equals(item.getEnabled()));
            template.put("defaultTemplate", Boolean.TRUE.equals(item.getDefaultTemplate()));
            template.put("priority", item.getPriority() == null ? index * 10 : item.getPriority());
            template.put("keywords", item.getKeywords() == null ? List.of() : item.getKeywords().stream()
                    .map(value -> trim(value, 30)).filter(StringUtils::hasText).distinct().limit(30).toList());
            result.add(template);
        }
        if (result.stream().noneMatch(item -> Boolean.TRUE.equals(item.get("defaultTemplate")))) result.get(0).put("defaultTemplate", true);
        return result;
    }

    private List<Map<String, Object>> defaultTemplates()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(template("GENERAL", "通用问候", "GENERAL", "你好，看到你刚刚来过{{liveRoomName}}，想了解哪款产品呢？有问题可以直接告诉我。", true, List.of(), 60));
        result.add(template("COMMENT", "评论用户", "COMMENT", "你好，看到你在直播间咨询了“{{comment}}”，这边可以继续帮你详细解答。", false, List.of(), 50));
        result.add(template("PRODUCT", "产品咨询", "KEYWORD", "你好，看到你在直播间关注了我们的产品，需要了解型号、价格或活动都可以直接问我。", false, List.of("型号", "区别", "功能", "适合"), 10));
        result.add(template("PROMOTION", "优惠咨询", "KEYWORD", "你好，看到你在直播间咨询优惠活动，目前具体活动可以在这里继续了解，有需要我可以帮你确认。", false, List.of("优惠", "价格", "赠品", "活动", "便宜"), 20));
        result.add(template("AFTER_SALES", "售后咨询", "KEYWORD", "你好，看到你在直播间咨询安装或售后问题，可以把具体情况发给我，这边帮你确认。", false, List.of("安装", "发货", "维修", "退换", "售后"), 30));
        result.add(template("FALLBACK", "兜底模板", "FALLBACK", "你好，感谢关注{{shopName}}，有任何产品问题都可以直接告诉我。", false, List.of(), 99));
        return result;
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
            Map<String, Object> task = new LinkedHashMap<>(source);
            Map<String, Object> selected = selectTemplate(templates, str(task.get("lastCommentContent")));
            task.put("messageTemplateKey", selected.get("templateKey"));
            task.put("messageTemplateName", selected.get("templateName"));
            task.put("messageContent", renderTemplate(str(selected.get("content")), task, shop));
            result.add(task);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> selectTemplate(List<Map<String, Object>> templates, String comment)
    {
        List<Map<String, Object>> enabled = templates.stream().filter(item -> !Boolean.FALSE.equals(item.get("enabled")))
                .sorted(Comparator.comparingInt(item -> asInt(item.get("priority")))).toList();
        if (StringUtils.hasText(comment))
        {
            for (Map<String, Object> item : enabled)
            {
                if (!"KEYWORD".equals(str(item.get("scene")).toUpperCase(Locale.ROOT))) continue;
                List<String> keywords = item.get("keywords") instanceof List<?> values ? (List<String>) values : List.of();
                if (keywords.stream().anyMatch(comment::contains)) return item;
            }
            Map<String, Object> commentTemplate = enabled.stream()
                    .filter(item -> "COMMENT".equals(str(item.get("scene")).toUpperCase(Locale.ROOT))).findFirst().orElse(null);
            if (commentTemplate != null) return commentTemplate;
        }
        return enabled.stream().filter(item -> Boolean.TRUE.equals(item.get("defaultTemplate"))).findFirst()
                .orElseGet(() -> enabled.stream().findFirst().orElse(defaultTemplates().get(0)));
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
