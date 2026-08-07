package com.xinke.erp.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
            rpaOutreachMapper.initializeTrackingConfig();
            rpaOutreachMapper.upgradeRoomShopKeyLength();
            rpaOutreachMapper.upgradeTaskSecUidLength();
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
        result.put("batch", rpaOutreachMapper.selectBatch(batchNo));
        result.put("tasks", rpaOutreachMapper.selectBatchTasks(batchNo));
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
        if ("ORDERED".equals(outcome) && !StringUtils.hasText(orderNo))
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
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("taskId", task.get("taskId"));
        resultData.put("requestId", requestId);
        resultData.put("status", status);
        resultData.put("outcome", outcome);
        resultData.put("douyinNo", douyinNo);
        resultData.put("orderNo", orderNo);
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
        rpaOutreachMapper.refreshBatchProgress(request.getBatchNo());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("idempotent", false);
        result.put("taskNo", taskNo);
        result.put("status", status);
        result.put("outcome", outcome);
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
        return rpaOutreachMapper.selectShopConfigList();
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
    public Map<String, Object> getTrackingConfig()
    {
        Map<String, Object> config = rpaOutreachMapper.selectTrackingConfig();
        if (config == null || config.isEmpty())
        {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("enabled", true);
            defaults.put("lookbackDays", 1);
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
        if ("ORDERED".equals(outcome))
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
            case "ORDERED" -> "ordered";
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
        data.put("shopCode", trim(request.getShopCode(), 64));
        data.put("shopName", trim(request.getShopName(), 100));
        data.put("douyinAccountCode", trim(request.getDouyinAccountCode(), 128));
        data.put("douyinShopName", trim(request.getDouyinShopName(), 128));
        data.put("messageTemplate", trim(request.getMessageTemplate(), 1000));
        data.put("dailyLimit", request.getDailyLimit() == null ? 200 : request.getDailyLimit());
        data.put("status", "1".equals(request.getStatus()) ? "1" : "0");
        data.put("remark", trim(request.getRemark(), 500));
        return data;
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
