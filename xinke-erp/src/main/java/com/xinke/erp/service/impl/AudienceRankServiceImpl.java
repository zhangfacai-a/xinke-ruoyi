package com.xinke.erp.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Date;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.constant.HttpStatus;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.domain.audience.AudienceCommentRankItem;
import com.xinke.erp.domain.audience.AudienceRankBatch;
import com.xinke.erp.domain.audience.AudienceRankBatchDetail;
import com.xinke.erp.domain.audience.AudienceRankBatchQuery;
import com.xinke.erp.domain.audience.AudienceRankImportRequest;
import com.xinke.erp.domain.audience.AudienceRankImportResult;
import com.xinke.erp.domain.audience.AudienceRankProfile;
import com.xinke.erp.domain.audience.AudienceRankQuery;
import com.xinke.erp.domain.audience.AudienceRankRoomMatch;
import com.xinke.erp.domain.audience.AudienceRankSnapshot;
import com.xinke.erp.domain.audience.AudienceRankSummary;
import com.xinke.erp.domain.audience.AudienceRankUserItem;
import com.xinke.erp.domain.audience.AudienceWatchRankItem;
import com.xinke.erp.domain.audience.AudienceFollowup;
import com.xinke.erp.domain.audience.AudienceFollowupLog;
import com.xinke.erp.domain.audience.AudienceFollowupQuery;
import com.xinke.erp.domain.audience.AudienceVisitRecord;
import com.xinke.erp.domain.audience.AudienceAssignmentRule;
import com.xinke.erp.domain.audience.AudienceOpportunity;
import com.xinke.erp.domain.audience.AudienceCustomerOrder;
import com.xinke.erp.mapper.AudienceRankMapper;
import com.xinke.erp.service.IAudienceRankService;

@Service
public class AudienceRankServiceImpl implements IAudienceRankService
{
    private static final int MAX_ROWS_PER_RANKING = 500;
    private static final long MAX_CLOCK_SKEW_MILLIS = 24L * 60L * 60L * 1000L;
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("uuuu.MM.dd")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final DateTimeFormatter DASH_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> FOLLOWUP_STATUSES = Set.of("OBSERVING", "UNASSIGNED", "PENDING", "CONTACTED",
            "QUALIFIED", "QUOTED", "ORDER_PENDING", "ORDERED", "CLOSED", "PAUSED", "INVALID");
    private static final Set<String> TERMINAL_FOLLOWUP_STATUSES = Set.of("ORDERED", "CLOSED", "INVALID");
    private static final Set<String> FOLLOW_RESULT_CODES = Set.of("NO_RESPONSE", "CONTACTED", "QUOTED",
            "CONSIDERING", "ORDER_PENDING", "ORDERED", "PAUSED", "INVALID");
    private static final Set<String> INTENT_LEVELS = Set.of("HIGH", "MEDIUM", "LOW", "UNKNOWN");
    private static final Set<String> CLOSE_REASON_CODES = Set.of("NO_NEED", "PRICE", "NO_RESPONSE",
            "DUPLICATE", "OTHER");
    private static final Set<String> FOLLOWUP_STAGES = Set.of("OBSERVING", "UNASSIGNED", "FOLLOWING", "DEAL_PENDING",
            "ORDERED", "ENDED");
    private static final Set<String> BATCH_CHANGE_FIELDS = Set.of("claim", "status", "ownerUserId", "anchorUserId",
            "controllerUserId", "priority", "nextFollowAt", "consultModel");
    private static final Set<String> BATCH_EDIT_FIELDS = Set.of("status", "priority", "nextFollowAt",
            "consultModel");

    private final AudienceRankMapper mapper;

    public AudienceRankServiceImpl(AudienceRankMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudienceRankImportResult importRanks(AudienceRankImportRequest request, String uploadedIp)
    {
        PreparedImport prepared = prepare(request);
        AudienceRankBatch existing = mapper.selectBatchByHash(prepared.payloadHash);
        if (existing != null)
        {
            ensureBatchRoom(existing);
            activateAsCurrent(existing);
            ensureImportedAudience(existing, mapper.selectSnapshotsByBatchId(existing.getBatchId()));
            return toResult(existing, true);
        }

        Date capturedAt = resolveCapturedAt(request.getCapturedAt());
        AudienceRankBatch batch = buildBatch(prepared, capturedAt, uploadedIp);
        resolveOrCreateRoom(batch);

        try
        {
            mapper.insertBatch(batch);
            mapper.supersedeCurrentBatches(batch);
        }
        catch (DuplicateKeyException e)
        {
            existing = mapper.selectBatchByHash(prepared.payloadHash);
            if (existing != null)
            {
                ensureBatchRoom(existing);
                activateAsCurrent(existing);
                ensureImportedAudience(existing, mapper.selectSnapshotsByBatchId(existing.getBatchId()));
                return toResult(existing, true);
            }
            throw e;
        }
        if (batch.getBatchId() == null)
        {
            throw new ServiceException("榜单批次保存失败");
        }

        List<AudienceRankSnapshot> snapshots = prepared.snapshots;
        for (AudienceRankSnapshot snapshot : snapshots)
        {
            snapshot.setBatchId(batch.getBatchId());
            snapshot.setRoomScopeKey(batch.getRoomScopeKey());
            snapshot.setRoomName(batch.getRoomName());
            snapshot.setRoomId(batch.getRoomId());
            snapshot.setCapturedAt(capturedAt);
        }
        int inserted = mapper.insertSnapshots(snapshots);
        if (inserted != snapshots.size())
        {
            throw new ServiceException("榜单明细保存不完整，已取消本次上传");
        }

        List<AudienceRankProfile> profiles = buildProfiles(batch, snapshots);
        if (mapper.upsertProfiles(profiles) <= 0)
        {
            throw new ServiceException("观众资料更新失败，已取消本次上传");
        }
        createOrRefreshFollowups(batch, snapshots);
        return toResult(batch, false);
    }

    private void ensureImportedAudience(AudienceRankBatch batch, List<AudienceRankSnapshot> snapshots)
    {
        if (batch == null || snapshots == null || snapshots.isEmpty()) return;
        mapper.upsertProfiles(buildProfiles(batch, snapshots));
        createOrRefreshFollowups(batch, snapshots);
    }

    private void ensureBatchRoom(AudienceRankBatch batch)
    {
        if (batch.getRoomId() != null && "MATCHED".equals(batch.getRoomMatchStatus())) return;
        resolveOrCreateRoom(batch);
        mapper.updateBatchRoom(batch);
    }

    private void activateAsCurrent(AudienceRankBatch batch)
    {
        mapper.activateBatch(batch);
        mapper.supersedeCurrentBatches(batch);
        batch.setIsCurrent(true);
    }

    private void resolveOrCreateRoom(AudienceRankBatch batch)
    {
        List<AudienceRankRoomMatch> matches = mapper.selectMatchingRooms(batch.getRoomName());
        if (matches != null && !matches.isEmpty())
        {
            applyRoomMatch(batch, matches);
            return;
        }
        AudienceRankRoomMatch room = new AudienceRankRoomMatch();
        room.setRoomCode("DY-" + sha256Hex(batch.getRoomName().trim().toLowerCase(Locale.ROOT)).substring(0, 24));
        room.setRoomName(batch.getRoomName());
        room.setLiveAccount(batch.getRoomName());
        try
        {
            mapper.insertAutoRoom(room);
        }
        catch (DuplicateKeyException ignored)
        {
            matches = mapper.selectMatchingRooms(batch.getRoomName());
            if (matches == null || matches.isEmpty()) throw new ServiceException("直播间自动创建失败，请重试");
            applyRoomMatch(batch, matches);
            return;
        }
        if (room.getRoomId() == null) throw new ServiceException("直播间自动创建失败");
        applyRoomMatch(batch, List.of(room));
    }

    @Override
    public List<AudienceRankSnapshot> selectSnapshotList(AudienceRankQuery query)
    {
        normalizeQuery(query);
        return mapper.selectSnapshotList(query);
    }

    @Override
    public List<AudienceRankBatch> selectBatchList(AudienceRankBatchQuery query)
    {
        if (query == null)
        {
            query = new AudienceRankBatchQuery();
        }
        query.setRoomName(normalizeOptionalText(query.getRoomName(), "直播间名称", 128));
        String matchStatus = upper(trimToNull(query.getRoomMatchStatus()));
        if (matchStatus != null && !Set.of("MATCHED", "UNMATCHED", "AMBIGUOUS", "NEEDS_MATCH").contains(matchStatus))
        {
            throw badRequest("直播间匹配状态不正确");
        }
        query.setRoomMatchStatus(matchStatus);
        if (query.getBeginCapturedAt() != null && query.getEndCapturedAt() != null
                && query.getBeginCapturedAt().after(query.getEndCapturedAt()))
        {
            throw badRequest("采集开始日期不能晚于结束日期");
        }
        return mapper.selectBatchList(query);
    }

    @Override
    public AudienceRankBatchDetail selectBatchDetail(Long batchId)
    {
        if (batchId == null || batchId <= 0)
        {
            throw badRequest("批次编号不正确");
        }
        AudienceRankBatch batch = mapper.selectBatchById(batchId);
        if (batch == null)
        {
            throw new ServiceException("榜单批次不存在", HttpStatus.NOT_FOUND);
        }
        AudienceRankBatchDetail detail = new AudienceRankBatchDetail();
        detail.setBatch(batch);
        detail.setSnapshots(mapper.selectSnapshotsByBatchId(batchId));
        return detail;
    }

    @Override
    public AudienceRankSummary selectSummary(AudienceRankQuery query)
    {
        normalizeQuery(query);
        AudienceRankSummary summary = mapper.selectSummary(query);
        return summary == null ? new AudienceRankSummary() : summary;
    }

    @Override
    public List<AudienceFollowup> selectFollowupList(AudienceFollowupQuery query, Long currentUserId)
    {
        normalizeFollowupQuery(query);
        applyFollowupVisibility(query, currentUserId);
        List<AudienceFollowup> rows = mapper.selectFollowupList(query);
        enrichFollowupStats(rows);
        return rows;
    }

    @Override
    public AudienceFollowup selectFollowup(Long followupId)
    {
        if (followupId == null || followupId <= 0) throw badRequest("跟单档案编号不正确");
        AudienceFollowup value = mapper.selectFollowupById(followupId);
        if (value == null) throw new ServiceException("跟单档案不存在", HttpStatus.NOT_FOUND);
        enrichFollowupStats(List.of(value));
        List<AudienceOpportunity> opportunities = mapper.selectOpportunities(followupId);
        value.setOpportunities(opportunities == null ? List.of() : opportunities);
        value.setCurrentOpportunity(value.getOpportunities().stream()
                .filter(item -> Boolean.TRUE.equals(item.getCurrent())).findFirst().orElse(null));
        List<AudienceCustomerOrder> orders = mapper.selectCustomerOrders(followupId);
        value.setOrders(orders == null ? List.of() : orders);
        return value;
    }

    @Override
    public AudienceFollowup selectFollowup(Long followupId, Long currentUserId)
    {
        AudienceFollowup value = selectFollowup(followupId);
        ensureFollowupReadAccess(value, currentUserId);
        return value;
    }

    @Override
    public List<AudienceFollowupLog> selectFollowupLogs(Long followupId)
    {
        selectFollowup(followupId);
        return mapper.selectFollowupLogs(followupId);
    }

    @Override
    public List<AudienceFollowupLog> selectFollowupLogs(Long followupId, Long currentUserId)
    {
        AudienceFollowup value = selectFollowup(followupId, currentUserId);
        return mapper.selectFollowupLogs(value.getFollowupId());
    }

    @Override
    public List<AudienceVisitRecord> selectFollowupVisits(Long followupId, Long currentUserId)
    {
        AudienceFollowup value = selectFollowup(followupId, currentUserId);
        return mapper.selectFollowupVisits(value.getSecUid());
    }

    @Override
    public List<Map<String, Object>> selectFollowupSummary(AudienceFollowupQuery query, Long currentUserId)
    {
        normalizeFollowupQuery(query);
        applyFollowupVisibility(query, currentUserId);
        return mapper.selectFollowupSummary(query);
    }

    @Override
    public Map<String, Object> selectTeamDashboard(AudienceFollowupQuery query)
    {
        normalizeFollowupQuery(query);
        if (query.getBeginDate() == null) query.setBeginDate(LocalDate.now().minusDays(29));
        if (query.getEndDate() == null) query.setEndDate(LocalDate.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", mapper.selectTeamOverview(query));
        result.put("funnel", mapper.selectTeamFunnel(query));
        result.put("owners", mapper.selectOwnerPerformance(query));
        result.put("rooms", mapper.selectRoomPerformance(query));
        result.put("trend", mapper.selectDailyTrend(query));
        result.put("beginDate", query.getBeginDate());
        result.put("endDate", query.getEndDate());
        return result;
    }

    @Override
    public List<Map<String, Object>> selectFollowupRooms()
    {
        return mapper.selectFollowupRoomOptions();
    }

    @Override
    public List<Map<String, Object>> selectFollowupAssignees(Long roomId, String roleCode)
    {
        if (roomId != null && roomId <= 0) throw badRequest("直播间编号不正确");
        String role = trimToNull(roleCode);
        if (role != null && !Set.of("owner", "anchor", "controller").contains(role)) throw badRequest("人员角色不正确");
        if ("owner".equals(role)) return mapper.selectAllActiveUsers();
        return mapper.selectFollowupAssignees(roomId, role);
    }

    @Override
    public List<AudienceAssignmentRule> selectAssignmentRules()
    {
        List<AudienceAssignmentRule> rules = mapper.selectAssignmentRules();
        if (rules == null) return List.of();
        for (AudienceAssignmentRule rule : rules)
        {
            List<Map<String, Object>> members = mapper.selectAssignmentRuleMembers(rule.getRuleId());
            rule.setMembers(members == null ? List.of() : members);
            rule.setMemberUserIds(rule.getMembers().stream().map(item -> toLong(item.get("userId")))
                    .filter(Objects::nonNull).toList());
        }
        return rules;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudienceAssignmentRule saveAssignmentRule(AudienceAssignmentRule rule, Long operatorUserId,
                                                       String operatorName)
    {
        requireAssignPermission(operatorUserId);
        if (rule == null || rule.getRoomId() == null || rule.getRoomId() <= 0)
            throw badRequest("请选择直播间");
        boolean roomExists = mapper.selectFollowupRoomOptions().stream()
                .anyMatch(item -> rule.getRoomId().equals(toLong(item.get("roomId"))));
        if (!roomExists) throw badRequest("直播间不存在或已停用");
        List<Long> memberIds = rule.getMemberUserIds() == null ? List.of() : rule.getMemberUserIds().stream()
                .filter(Objects::nonNull).filter(value -> value > 0).distinct().toList();
        if (Boolean.TRUE.equals(rule.getEnabled()) && memberIds.isEmpty())
            throw badRequest("启用智能分配前请至少选择一位领取人");
        int maxActive = rule.getMaxActivePerOwner() == null ? 100 : rule.getMaxActivePerOwner();
        int reclaimHours = rule.getReclaimHours() == null ? 24 : rule.getReclaimHours();
        if (maxActive < 1 || maxActive > 10000) throw badRequest("每人进行中客户数应为1至10000");
        if (reclaimHours < 1 || reclaimHours > 720) throw badRequest("超时回收时间应为1至720小时");
        int commentThreshold = normalizeQualificationThreshold(rule.getCommentRankThreshold(), 30, "评论榜名次");
        int watchThreshold = normalizeQualificationThreshold(rule.getWatchRankThreshold(), 30, "观看榜名次");
        int minPayLevel = normalizeQualificationThreshold(rule.getMinPayLevel(), 10, "最低消费等级");
        int minVisitDays = normalizeQualificationThreshold(rule.getMinVisitDays(), 2, "累计到访天数");
        Set<Long> activeUserIds = mapper.selectAllActiveUsers().stream().map(item -> toLong(item.get("userId")))
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (!activeUserIds.containsAll(memberIds)) throw badRequest("领取人中包含已停用账号，请重新选择");

        AudienceAssignmentRule before = mapper.selectAssignmentRuleByRoomId(rule.getRoomId());
        rule.setRuleId(before == null ? null : before.getRuleId());
        rule.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        rule.setMaxActivePerOwner(maxActive);
        rule.setReclaimHours(reclaimHours);
        rule.setQualificationEnabled(rule.getQualificationEnabled() == null || Boolean.TRUE.equals(rule.getQualificationEnabled()));
        rule.setCommentRankThreshold(commentThreshold);
        rule.setWatchRankThreshold(watchThreshold);
        rule.setMinPayLevel(minPayLevel);
        rule.setMinVisitDays(minVisitDays);
        rule.setFollowerQualifies(Boolean.TRUE.equals(rule.getFollowerQualifies()));
        rule.setFollowingQualifies(Boolean.TRUE.equals(rule.getFollowingQualifies()));
        rule.setNextMemberIndex(before == null || before.getNextMemberIndex() == null ? 0 : before.getNextMemberIndex());
        rule.setUpdateBy(operatorName);
        mapper.upsertAssignmentRule(rule);
        AudienceAssignmentRule saved = mapper.selectAssignmentRuleByRoomId(rule.getRoomId());
        if (saved == null || saved.getRuleId() == null) throw new ServiceException("分配规则保存失败");
        mapper.deleteAssignmentRuleMembers(saved.getRuleId());
        if (!memberIds.isEmpty()) mapper.insertAssignmentRuleMembers(saved.getRuleId(), memberIds);
        requalifyObservingCustomers(rule.getRoomId(), saved, operatorName);
        if (Boolean.TRUE.equals(rule.getEnabled())) autoAssignRoom(rule.getRoomId(), operatorUserId, operatorName);
        return selectAssignmentRules().stream().filter(item -> rule.getRoomId().equals(item.getRoomId()))
                .findFirst().orElse(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoAssignFollowups(Long roomId, Long operatorUserId, String operatorName)
    {
        requireAssignPermission(operatorUserId);
        if (roomId == null || roomId <= 0) throw badRequest("请选择直播间");
        return autoAssignRoom(roomId, operatorUserId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowup(AudienceFollowup followup, Long operatorUserId, String operatorName)
    {
        if (followup == null || followup.getFollowupId() == null) throw badRequest("跟单档案不能为空");
        AudienceFollowup before = selectFollowup(followup.getFollowupId());
        ensureFollowupAccess(before, operatorUserId);
        Long resolvedRoomId = resolveFollowupRoomId(before);
        boolean ownerChanged = !Objects.equals(before.getOwnerUserId(), followup.getOwnerUserId());
        boolean anchorChanged = !Objects.equals(before.getAnchorUserId(), followup.getAnchorUserId());
        boolean controllerChanged = !Objects.equals(before.getControllerUserId(), followup.getControllerUserId());
        if (ownerChanged || anchorChanged || controllerChanged) requireAssignPermission(operatorUserId);
        followup.setRoomId(resolvedRoomId);
        if (followup.getPriority() == null) followup.setPriority(before.getPriority());
        validateFollowupFields(followup, before);
        applyResultStatus(followup);
        if ("OBSERVING".equals(before.getStatus()) && Boolean.TRUE.equals(followup.getPriority()))
        {
            followup.setStatus("UNASSIGNED");
            followup.setQualificationReason("人工标记重点客户");
            followup.setQualifiedAt(new Date());
        }
        if (ownerChanged) syncOwnerSnapshot(followup);
        else followup.setOwnerNameSnapshot(before.getOwnerNameSnapshot());
        if (anchorChanged) syncAssigneeSnapshot(followup, resolvedRoomId, "anchor");
        else followup.setAnchorNameSnapshot(before.getAnchorNameSnapshot());
        if (controllerChanged) syncAssigneeSnapshot(followup, resolvedRoomId, "controller");
        else followup.setControllerNameSnapshot(before.getControllerNameSnapshot());
        if (ownerChanged) alignAssignmentStatus(followup);
        if (!Objects.equals(before.getFollowResultCode(), followup.getFollowResultCode())
                || !Objects.equals(before.getLastFollowResult(), followup.getLastFollowResult()))
        {
            followup.setLastContactAt(new Date());
        }
        clearReminderForTerminalStatus(followup);
        applyResultReminder(followup);
        applyDefaultReminder(followup);
        followup.setUpdateBy(operatorName);
        if (followup.getVersion() == null) followup.setVersion(before.getVersion());
        int changed = mapper.updateFollowup(followup);
        if (changed != 1) throw new ServiceException("记录已被其他人修改，请刷新后重试", HttpStatus.CONFLICT);
        AudienceFollowup after = selectFollowup(followup.getFollowupId());
        syncCurrentOpportunity(after, operatorName);
        syncOrderFromFollowup(after);
        String action = !Objects.equals(before.getFollowResultCode(), after.getFollowResultCode())
                || !Objects.equals(before.getLastFollowResult(), after.getLastFollowResult()) ? "CONTACT" : "UPDATE";
        insertLog(after.getFollowupId(), action, before, after, null, after.getLastFollowResult(),
                after.getFollowResultCode(),
                before.getStatus(), after.getStatus(), after.getNextFollowAt(), operatorUserId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimFollowup(Long followupId, Long operatorUserId, String operatorName)
    {
        AudienceFollowup before = selectFollowup(followupId);
        if (!Set.of("OBSERVING", "UNASSIGNED").contains(before.getStatus())) ensureFollowupAccess(before, operatorUserId);
        AudienceFollowup update = copyFollowup(before);
        update.setOwnerUserId(operatorUserId);
        update.setOwnerNameSnapshot(operatorName);
        if (Set.of("OBSERVING", "UNASSIGNED").contains(before.getStatus())) update.setStatus("PENDING");
        if ("OBSERVING".equals(before.getStatus()))
        {
            update.setQualificationReason("人工加入跟进");
            update.setQualifiedAt(new Date());
        }
        applyDefaultReminder(update);
        update.setVersion(before.getVersion());
        update.setUpdateBy(operatorName);
        if (mapper.updateFollowup(update) != 1) throw new ServiceException("领取失败，请刷新后重试", HttpStatus.CONFLICT);
        AudienceFollowup after = selectFollowup(followupId);
        syncCurrentOpportunity(after, operatorName);
        insertLog(followupId, "ASSIGN", before, after, null, "领取跟单", null,
                before.getStatus(), after.getStatus(), after.getNextFollowAt(), operatorUserId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowupStatus(Long followupId, String status, String content, Date nextFollowAt,
                                     Long operatorUserId, String operatorName)
    {
        String normalized = upper(trimToNull(status));
        if (normalized == null || !FOLLOWUP_STATUSES.contains(normalized)) throw badRequest("跟进状态不正确");
        AudienceFollowup before = selectFollowup(followupId);
        ensureFollowupAccess(before, operatorUserId);
        String text = trimToNull(content);
        if (("ORDERED".equals(normalized) || "CLOSED".equals(normalized))
                && trimToNull(before.getOrderNo()) == null) throw badRequest("已下单或已完成必须先填写订单号");
        if ("INVALID".equals(normalized) && text == null) throw badRequest("无效记录请填写关闭原因");
        AudienceFollowup update = copyFollowup(before);
        update.setStatus(normalized);
        if (text != null) update.setLastFollowResult(text);
        if ("INVALID".equals(normalized)) update.setCloseReason(text);
        if (nextFollowAt != null) update.setNextFollowAt(nextFollowAt);
        clearReminderForTerminalStatus(update);
        applyDefaultReminder(update);
        update.setLastContactAt(new Date());
        update.setVersion(before.getVersion());
        update.setUpdateBy(operatorName);
        if (mapper.updateFollowup(update) != 1) throw new ServiceException("状态已被其他人修改，请刷新后重试", HttpStatus.CONFLICT);
        AudienceFollowup after = selectFollowup(followupId);
        syncCurrentOpportunity(after, operatorName);
        insertLog(followupId, "STATUS", before, after, null, text, text,
                before.getStatus(), after.getStatus(), after.getNextFollowAt(), operatorUserId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateFollowups(List<Long> followupIds, Map<String, Object> changes,
                                     Long operatorUserId, String operatorName)
    {
        if (followupIds == null || followupIds.isEmpty() || followupIds.size() > 500) throw badRequest("请选择要处理的记录");
        if (changes == null || changes.isEmpty()) throw badRequest("没有需要修改的内容");
        Set<String> unsupportedFields = new HashSet<>(changes.keySet());
        unsupportedFields.removeAll(BATCH_CHANGE_FIELDS);
        if (!unsupportedFields.isEmpty()) throw badRequest("批量修改包含不支持的字段");
        boolean changesAssignee = changes.containsKey("ownerUserId") || changes.containsKey("anchorUserId") || changes.containsKey("controllerUserId");
        boolean changesBusiness = changes.keySet().stream().anyMatch(BATCH_EDIT_FIELDS::contains);
        boolean claim = Boolean.TRUE.equals(toBoolean(changes.get("claim")));
        if (changesAssignee) requireAssignPermission(operatorUserId);
        if (changesBusiness) requireEditPermission(operatorUserId);
        if (claim) requireClaimPermission(operatorUserId);
        if (!claim && !changesAssignee && !changesBusiness) throw badRequest("没有需要修改的内容");
        for (Long id : followupIds)
        {
            AudienceFollowup before = selectFollowup(id);
            if (claim && "UNASSIGNED".equals(before.getStatus()))
            {
                claimFollowup(id, operatorUserId, operatorName);
                before = selectFollowup(id);
            }
            ensureFollowupAccess(before, operatorUserId);
            AudienceFollowup update = copyFollowup(before);
            if (changes.containsKey("status")) update.setStatus(upper(Objects.toString(changes.get("status"), null)));
            if (changes.containsKey("ownerUserId"))
            {
                update.setOwnerUserId(toLong(changes.get("ownerUserId")));
                syncOwnerSnapshot(update);
            }
            if (changes.containsKey("anchorUserId"))
            {
                update.setAnchorUserId(toLong(changes.get("anchorUserId")));
                syncAssigneeSnapshot(update, before.getRoomId(), "anchor");
            }
            if (changes.containsKey("controllerUserId"))
            {
                update.setControllerUserId(toLong(changes.get("controllerUserId")));
                syncAssigneeSnapshot(update, before.getRoomId(), "controller");
            }
            if (changes.containsKey("ownerUserId")) alignAssignmentStatus(update);
            if (changes.containsKey("priority"))
            {
                Boolean priority = toBoolean(changes.get("priority"));
                if (priority == null) throw badRequest("重点跟进设置不正确");
                update.setPriority(priority);
                if (priority && "OBSERVING".equals(before.getStatus()))
                {
                    update.setStatus("UNASSIGNED");
                    update.setQualificationReason("人工标记重点客户");
                    update.setQualifiedAt(new Date());
                }
            }
            if (changes.containsKey("nextFollowAt")) update.setNextFollowAt(toDate(changes.get("nextFollowAt")));
            if (changes.containsKey("consultModel")) update.setConsultModel(Objects.toString(changes.get("consultModel"), null));
            if (claim && changes.size() == 1) continue;
            validateFollowupFields(update, before);
            clearReminderForTerminalStatus(update);
            applyDefaultReminder(update);
            update.setVersion(before.getVersion());
            update.setUpdateBy(operatorName);
            if (mapper.updateFollowup(update) != 1) throw new ServiceException("批量处理过程中记录已变化，请刷新后重试", HttpStatus.CONFLICT);
            AudienceFollowup after = selectFollowup(id);
            insertLog(id, "UPDATE", before, after, null, "批量修改", null,
                    before.getStatus(), after.getStatus(), after.getNextFollowAt(), operatorUserId, operatorName);
        }
    }

    private void createOrRefreshFollowups(AudienceRankBatch batch, List<AudienceRankSnapshot> snapshots)
    {
        List<AudienceFollowup> values = new ArrayList<>(snapshots.size());
        Map<String, AudienceFollowup> previousByUid = new LinkedHashMap<>();
        AudienceAssignmentRule qualificationRule = effectiveQualificationRule(batch.getRoomId());
        List<Map<String, Object>> anchors = batch.getRoomId() == null ? List.of()
                : mapper.selectFollowupAssignees(batch.getRoomId(), "anchor");
        List<Map<String, Object>> controllers = batch.getRoomId() == null ? List.of()
                : mapper.selectFollowupAssignees(batch.getRoomId(), "controller");
        for (AudienceRankSnapshot snapshot : snapshots)
        {
            AudienceFollowup previous = mapper.selectFollowupByUid(snapshot.getSecUid());
            if (previous != null) previousByUid.put(snapshot.getSecUid(), previous);
            AudienceFollowup value = new AudienceFollowup();
            value.setRoomScopeKey(batch.getRoomScopeKey());
            value.setRoomId(batch.getRoomId());
            value.setRoomNameSnapshot(batch.getRoomName());
            value.setSecUid(snapshot.getSecUid());
            value.setNicknameSnapshot(snapshot.getNickname());
            value.setCommentRank(snapshot.getCommentRank());
            value.setWatchRank(snapshot.getWatchRank());
            value.setIsFollower(snapshot.getIsFollower());
            value.setIsFollowing(snapshot.getIsFollowing());
            value.setPayLevel(snapshot.getPayLevel());
            value.setAppearanceDays(1);
            String qualificationReason = qualificationReason(value, qualificationRule);
            value.setStatus(qualificationReason == null ? "OBSERVING" : "UNASSIGNED");
            value.setQualificationReason(qualificationReason);
            if (qualificationReason != null) value.setQualifiedAt(batch.getCapturedAt());
            value.setFirstSourceBatchId(batch.getBatchId());
            value.setLastSourceBatchId(batch.getBatchId());
            value.setFirstSeenAt(batch.getCapturedAt());
            value.setLastSeenAt(batch.getCapturedAt());
            if (batch.getRoomId() != null)
            {
                assignDefault(value, anchors, true);
                assignDefault(value, controllers, false);
            }
            values.add(value);
        }
        if (!values.isEmpty()) mapper.insertFollowups(values);
        for (AudienceFollowup value : values)
        {
            AudienceFollowup previous = previousByUid.get(value.getSecUid());
            if (previous != null && TERMINAL_FOLLOWUP_STATUSES.contains(previous.getStatus())
                    && previous.getLastSeenAt() != null && value.getLastSeenAt() != null
                    && value.getLastSeenAt().after(previous.getLastSeenAt()))
            {
                mapper.markReactivationPending(previous.getFollowupId(), value.getLastSeenAt());
            }
            mapper.refreshFollowupSource(value);
            AudienceFollowup persisted = mapper.selectFollowupByUid(value.getSecUid());
            if (persisted != null)
            {
                enrichFollowupStats(List.of(persisted));
                if ("OBSERVING".equals(persisted.getStatus()))
                {
                    String reason = qualificationReason(persisted, qualificationRule);
                    if (reason != null)
                    {
                        AudienceFollowup promoted = copyFollowup(persisted);
                        promoted.setStatus("UNASSIGNED");
                        promoted.setQualificationReason(reason);
                        promoted.setQualifiedAt(new Date());
                        promoted.setVersion(persisted.getVersion());
                        promoted.setUpdateBy("system");
                        if (mapper.updateFollowup(promoted) == 1)
                        {
                            insertLog(persisted.getFollowupId(), "QUALIFY", persisted, promoted, null,
                                    "命中进客规则：" + reason, null, persisted.getStatus(), promoted.getStatus(),
                                    null, null, "系统");
                            persisted = mapper.selectFollowupByUid(value.getSecUid());
                        }
                    }
                }
                ensureInitialOpportunity(persisted);
            }
            boolean roomChanged = previous != null && !Objects.equals(previous.getRoomId(), value.getRoomId());
            if (persisted != null && (roomChanged || (persisted.getAnchorUserId() == null && persisted.getControllerUserId() == null)))
            {
                AudienceFollowup assignment = copyFollowup(persisted);
                assignment.setAnchorUserId(value.getAnchorUserId());
                assignment.setAnchorNameSnapshot(value.getAnchorNameSnapshot());
                assignment.setControllerUserId(value.getControllerUserId());
                assignment.setControllerNameSnapshot(value.getControllerNameSnapshot());
                assignment.setVersion(persisted.getVersion());
                assignment.setUpdateBy("system");
                mapper.updateFollowup(assignment);
            }
        }
        if (batch.getRoomId() != null) autoAssignRoom(batch.getRoomId(), null, "系统");
    }

    private void ensureInitialOpportunity(AudienceFollowup followup)
    {
        if (mapper.selectCurrentOpportunity(followup.getFollowupId()) != null) return;
        AudienceOpportunity opportunity = new AudienceOpportunity();
        opportunity.setFollowupId(followup.getFollowupId());
        opportunity.setSequenceNo(1);
        opportunity.setCurrent(true);
        opportunity.setStatus(followup.getStatus());
        opportunity.setFollowResultCode(followup.getFollowResultCode());
        opportunity.setIntentLevel(followup.getIntentLevel());
        opportunity.setConsultModel(followup.getConsultModel());
        opportunity.setSourceRoomId(followup.getRoomId());
        opportunity.setSourceRoomName(followup.getRoomNameSnapshot());
        opportunity.setOwnerUserId(followup.getOwnerUserId());
        opportunity.setOwnerNameSnapshot(followup.getOwnerNameSnapshot());
        opportunity.setCloseReasonCode(followup.getCloseReasonCode());
        opportunity.setCloseReason(followup.getCloseReason());
        opportunity.setOpenedAt(followup.getFirstSeenAt() == null ? new Date() : followup.getFirstSeenAt());
        if (TERMINAL_FOLLOWUP_STATUSES.contains(followup.getStatus())) opportunity.setClosedAt(followup.getStatusChangedAt());
        mapper.insertOpportunity(opportunity);
    }

    private void syncCurrentOpportunity(AudienceFollowup followup, String operatorName)
    {
        if (followup == null || followup.getFollowupId() == null) return;
        AudienceOpportunity opportunity = mapper.selectCurrentOpportunity(followup.getFollowupId());
        if (opportunity == null)
        {
            ensureInitialOpportunity(followup);
            opportunity = mapper.selectCurrentOpportunity(followup.getFollowupId());
        }
        if (opportunity == null) return;
        opportunity.setStatus(followup.getStatus());
        opportunity.setFollowResultCode(followup.getFollowResultCode());
        opportunity.setIntentLevel(followup.getIntentLevel());
        opportunity.setConsultModel(followup.getConsultModel());
        opportunity.setSourceRoomId(followup.getRoomId());
        opportunity.setSourceRoomName(followup.getRoomNameSnapshot());
        opportunity.setOwnerUserId(followup.getOwnerUserId());
        opportunity.setOwnerNameSnapshot(followup.getOwnerNameSnapshot());
        opportunity.setCloseReasonCode(followup.getCloseReasonCode());
        opportunity.setCloseReason(followup.getCloseReason());
        opportunity.setClosedAt(TERMINAL_FOLLOWUP_STATUSES.contains(followup.getStatus()) ?
                (followup.getStatusChangedAt() == null ? new Date() : followup.getStatusChangedAt()) : null);
        opportunity.setCurrent(true);
        mapper.updateOpportunity(opportunity);
    }

    private void syncOrderFromFollowup(AudienceFollowup followup)
    {
        String orderNo = followup == null ? null : trimToNull(followup.getOrderNo());
        if (orderNo == null || mapper.selectCustomerOrderByNo(orderNo) != null) return;
        AudienceCustomerOrder order = new AudienceCustomerOrder();
        order.setFollowupId(followup.getFollowupId());
        AudienceOpportunity current = mapper.selectCurrentOpportunity(followup.getFollowupId());
        if (current != null) order.setOpportunityId(current.getOpportunityId());
        order.setOrderNo(orderNo);
        order.setOrderStatus("ORDERED");
        order.setProductModel(followup.getConsultModel());
        order.setOrderedAt(followup.getStatusChangedAt() == null ? new Date() : followup.getStatusChangedAt());
        mapper.insertCustomerOrder(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudienceFollowup reactivateFollowup(Long followupId, Long operatorUserId, String operatorName)
    {
        AudienceFollowup before = selectFollowup(followupId);
        ensureFollowupAccess(before, operatorUserId);
        if (!TERMINAL_FOLLOWUP_STATUSES.contains(before.getStatus()) && !Boolean.TRUE.equals(before.getReactivationPending()))
            throw badRequest("当前客户仍在跟进中，无需重新激活");
        mapper.closeCurrentOpportunities(followupId);
        AudienceOpportunity opportunity = new AudienceOpportunity();
        opportunity.setFollowupId(followupId);
        List<AudienceOpportunity> history = mapper.selectOpportunities(followupId);
        opportunity.setSequenceNo((history == null ? 0 : history.size()) + 1);
        opportunity.setCurrent(true);
        opportunity.setStatus("UNASSIGNED");
        opportunity.setIntentLevel("UNKNOWN");
        opportunity.setSourceRoomId(before.getRoomId());
        opportunity.setSourceRoomName(before.getRoomNameSnapshot());
        opportunity.setOpenedAt(new Date());
        mapper.insertOpportunity(opportunity);
        AudienceFollowup update = copyFollowup(before);
        update.setStatus("UNASSIGNED");
        update.setFollowResultCode(null);
        update.setIntentLevel("UNKNOWN");
        update.setConsultModel(null);
        update.setOrderNo(null);
        update.setOwnerUserId(null);
        update.setOwnerNameSnapshot(null);
        update.setLastFollowResult(null);
        update.setLastContactAt(null);
        update.setNextFollowAt(null);
        update.setCloseReason(null);
        update.setCloseReasonCode(null);
        update.setReactivationPending(false);
        update.setVersion(before.getVersion());
        update.setUpdateBy(operatorName);
        if (mapper.updateFollowup(update) != 1) throw new ServiceException("客户已被其他人处理，请刷新后重试", HttpStatus.CONFLICT);
        AudienceFollowup after = selectFollowup(followupId);
        insertLog(followupId, "REACTIVATE", before, after, null, "客户再次到访，重新开启跟进", null,
                before.getStatus(), after.getStatus(), null, operatorUserId, operatorName);
        return after;
    }

    @Override
    public List<AudienceCustomerOrder> selectCustomerOrders(Long followupId, Long currentUserId)
    {
        AudienceFollowup followup = selectFollowup(followupId, currentUserId);
        return mapper.selectCustomerOrders(followup.getFollowupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudienceCustomerOrder saveCustomerOrder(Long followupId, AudienceCustomerOrder order,
                                                    Long operatorUserId, String operatorName)
    {
        requireEditPermission(operatorUserId);
        AudienceFollowup followup = selectFollowup(followupId, operatorUserId);
        if (order == null) throw badRequest("订单信息不能为空");
        String orderNo = trimToNull(order.getOrderNo());
        if (orderNo == null || orderNo.length() > 64) throw badRequest("请填写正确的订单号");
        AudienceCustomerOrder conflict = mapper.selectCustomerOrderByNo(orderNo);
        if (conflict != null && !Objects.equals(conflict.getCustomerOrderId(), order.getCustomerOrderId()))
            throw badRequest("订单号已属于其他客户，请核对后再保存");
        order.setFollowupId(followupId);
        order.setOrderNo(orderNo);
        if (order.getOrderStatus() == null || order.getOrderStatus().isBlank()) order.setOrderStatus("ORDERED");
        if (order.getOrderedAt() == null) order.setOrderedAt(new Date());
        AudienceOpportunity current = mapper.selectCurrentOpportunity(followupId);
        if (current != null) order.setOpportunityId(current.getOpportunityId());
        if (order.getCustomerOrderId() == null)
        {
            mapper.insertCustomerOrder(order);
        }
        else if (mapper.updateCustomerOrder(order) != 1)
        {
            throw new ServiceException("订单已被其他人修改，请刷新后重试", HttpStatus.CONFLICT);
        }
        AudienceFollowup update = copyFollowup(followup);
        update.setOrderNo(orderNo);
        update.setStatus("ORDERED");
        update.setFollowResultCode("ORDERED");
        update.setNextFollowAt(null);
        update.setVersion(followup.getVersion());
        update.setUpdateBy(operatorName);
        if (mapper.updateFollowup(update) != 1) throw new ServiceException("客户已被其他人修改，请刷新后重试", HttpStatus.CONFLICT);
        AudienceFollowup after = selectFollowup(followupId);
        syncCurrentOpportunity(after, operatorName);
        insertLog(followupId, "ORDER", followup, after, null, "保存订单 " + orderNo, "ORDERED",
                followup.getStatus(), after.getStatus(), null, operatorUserId, operatorName);
        return mapper.selectCustomerOrderByNo(orderNo);
    }

    private Map<String, Object> autoAssignRoom(Long roomId, Long operatorUserId, String operatorName)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", roomId);
        AudienceAssignmentRule rule = mapper.selectAssignmentRuleByRoomId(roomId);
        if (rule == null || !Boolean.TRUE.equals(rule.getEnabled()))
        {
            result.put("assignedCount", 0);
            result.put("reclaimedCount", 0);
            result.put("remainingCount", 0);
            result.put("message", "该直播间尚未启用智能分配");
            return result;
        }
        List<Map<String, Object>> members = mapper.selectAssignmentRuleMembers(rule.getRuleId());
        if (members == null || members.isEmpty())
        {
            result.put("assignedCount", 0);
            result.put("reclaimedCount", 0);
            result.put("remainingCount", 0);
            result.put("message", "分配规则没有可用领取人");
            return result;
        }

        int reclaimed = 0;
        int reclaimHours = rule.getReclaimHours() == null ? 24 : rule.getReclaimHours();
        List<AudienceFollowup> staleRows = mapper.selectReclaimableFollowups(roomId, reclaimHours, 500);
        if (staleRows != null) for (AudienceFollowup before : staleRows)
        {
            AudienceFollowup update = copyFollowup(before);
            update.setOwnerUserId(null);
            update.setOwnerNameSnapshot(null);
            update.setStatus("UNASSIGNED");
            update.setNextFollowAt(null);
            update.setVersion(before.getVersion());
            update.setUpdateBy(operatorName == null ? "系统" : operatorName);
            if (mapper.updateFollowup(update) == 1)
            {
                reclaimed++;
                insertLog(before.getFollowupId(), "RECLAIM", before, update, null,
                        "超过" + reclaimHours + "小时未联系，回收到待领取", null,
                        before.getStatus(), update.getStatus(), null, operatorUserId, operatorName);
            }
        }

        Map<Long, Integer> activeCounts = new LinkedHashMap<>();
        for (Map<String, Object> member : members)
        {
            Long userId = toLong(member.get("userId"));
            if (userId != null) activeCounts.put(userId, mapper.countActiveFollowupsByOwner(userId));
        }
        int maxActive = rule.getMaxActivePerOwner() == null ? 100 : rule.getMaxActivePerOwner();
        int cursor = Math.floorMod(rule.getNextMemberIndex() == null ? 0 : rule.getNextMemberIndex(), members.size());
        int assigned = 0;
        List<AudienceFollowup> pendingRows = mapper.selectUnassignedForAutoAssign(roomId, 500);
        if (pendingRows != null) for (AudienceFollowup before : pendingRows)
        {
            int chosen = -1;
            for (int offset = 0; offset < members.size(); offset++)
            {
                int index = (cursor + offset) % members.size();
                Long candidateId = toLong(members.get(index).get("userId"));
                if (candidateId != null && activeCounts.getOrDefault(candidateId, 0) < maxActive)
                {
                    chosen = index;
                    break;
                }
            }
            if (chosen < 0) break;
            Map<String, Object> member = members.get(chosen);
            Long ownerId = toLong(member.get("userId"));
            String ownerName = trimToNull(Objects.toString(member.get("userName"), null));
            AudienceFollowup update = copyFollowup(before);
            update.setOwnerUserId(ownerId);
            update.setOwnerNameSnapshot(ownerName);
            update.setStatus("PENDING");
            applyDefaultReminder(update);
            update.setVersion(before.getVersion());
            update.setUpdateBy(operatorName == null ? "系统" : operatorName);
            if (mapper.updateFollowup(update) == 1)
            {
                assigned++;
                activeCounts.put(ownerId, activeCounts.getOrDefault(ownerId, 0) + 1);
                cursor = (chosen + 1) % members.size();
                insertLog(before.getFollowupId(), "AUTO_ASSIGN", before, update, null,
                        "按直播间规则智能分配", null, before.getStatus(), update.getStatus(),
                        update.getNextFollowAt(), operatorUserId, operatorName);
            }
        }
        mapper.updateAssignmentRuleCursor(rule.getRuleId(), cursor);
        List<AudienceFollowup> remainingRows = mapper.selectUnassignedForAutoAssign(roomId, 501);
        int remaining = remainingRows == null ? 0 : remainingRows.size();
        result.put("assignedCount", assigned);
        result.put("reclaimedCount", reclaimed);
        result.put("remainingCount", remaining);
        result.put("message", assigned > 0 ? "已完成智能分配" : "当前没有可分配客户或领取人已达上限");
        return result;
    }

    private void assignDefault(AudienceFollowup target, List<Map<String, Object>> candidates, boolean anchor)
    {
        // Multiple mapped staff are intentional; only a single candidate is safe as an automatic default.
        if (candidates == null || candidates.size() != 1) return;
        Map<String, Object> row = candidates.get(0);
        Long id = toLong(row.get("userId"));
        String name = Objects.toString(row.get("userName"), null);
        if (anchor) { target.setAnchorUserId(id); target.setAnchorNameSnapshot(name); }
        else { target.setControllerUserId(id); target.setControllerNameSnapshot(name); }
    }

    private AudienceAssignmentRule effectiveQualificationRule(Long roomId)
    {
        AudienceAssignmentRule rule = roomId == null ? null : mapper.selectAssignmentRuleByRoomId(roomId);
        if (rule == null) rule = new AudienceAssignmentRule();
        if (rule.getQualificationEnabled() == null) rule.setQualificationEnabled(true);
        if (rule.getCommentRankThreshold() == null) rule.setCommentRankThreshold(30);
        if (rule.getWatchRankThreshold() == null) rule.setWatchRankThreshold(30);
        if (rule.getMinPayLevel() == null) rule.setMinPayLevel(10);
        if (rule.getMinVisitDays() == null) rule.setMinVisitDays(2);
        if (rule.getFollowerQualifies() == null) rule.setFollowerQualifies(false);
        if (rule.getFollowingQualifies() == null) rule.setFollowingQualifies(false);
        return rule;
    }

    private String qualificationReason(AudienceFollowup value, AudienceAssignmentRule rule)
    {
        if (Boolean.TRUE.equals(value.getPriority())) return "人工重点客户";
        if (rule == null || !Boolean.TRUE.equals(rule.getQualificationEnabled())) return null;
        List<String> reasons = new ArrayList<>();
        Integer commentRank = value.getBestCommentRank() == null ? value.getCommentRank() : value.getBestCommentRank();
        Integer watchRank = value.getBestWatchRank() == null ? value.getWatchRank() : value.getBestWatchRank();
        if (matchesPositiveMaximum(commentRank, rule.getCommentRankThreshold()))
            reasons.add("评论榜前" + rule.getCommentRankThreshold() + "名");
        if (matchesPositiveMaximum(watchRank, rule.getWatchRankThreshold()))
            reasons.add("观看榜前" + rule.getWatchRankThreshold() + "名");
        if (matchesPositiveMinimum(value.getPayLevel(), rule.getMinPayLevel()))
            reasons.add("消费等级达到" + rule.getMinPayLevel() + "级");
        if (matchesPositiveMinimum(value.getAppearanceDays(), rule.getMinVisitDays()))
            reasons.add("累计到访" + value.getAppearanceDays() + "天");
        if (Boolean.TRUE.equals(rule.getFollowerQualifies()) && Boolean.TRUE.equals(value.getIsFollower()))
            reasons.add("已关注直播间");
        if (Boolean.TRUE.equals(rule.getFollowingQualifies()) && Boolean.TRUE.equals(value.getIsFollowing()))
            reasons.add("主播已回关");
        return reasons.isEmpty() ? null : String.join("、", reasons.subList(0, Math.min(3, reasons.size())));
    }

    private boolean matchesPositiveMaximum(Integer value, Integer threshold)
    {
        return value != null && value > 0 && threshold != null && threshold > 0 && value <= threshold;
    }

    private boolean matchesPositiveMinimum(Integer value, Integer threshold)
    {
        return value != null && threshold != null && threshold > 0 && value >= threshold;
    }

    private int normalizeQualificationThreshold(Integer value, int fallback, String label)
    {
        int normalized = value == null ? fallback : value;
        if (normalized < 0 || normalized > 500) throw badRequest(label + "应为0至500，0表示不启用");
        return normalized;
    }

    private void requalifyObservingCustomers(Long roomId, AudienceAssignmentRule rule, String operatorName)
    {
        AudienceFollowupQuery query = new AudienceFollowupQuery();
        query.setRoomId(roomId);
        query.setStatus("OBSERVING");
        List<AudienceFollowup> rows = mapper.selectFollowupList(query);
        enrichFollowupStats(rows);
        if (rows == null) return;
        for (AudienceFollowup before : rows)
        {
            String reason = qualificationReason(before, rule);
            if (reason == null) continue;
            AudienceFollowup update = copyFollowup(before);
            update.setStatus("UNASSIGNED");
            update.setQualificationReason(reason);
            update.setQualifiedAt(new Date());
            update.setVersion(before.getVersion());
            update.setUpdateBy(operatorName == null ? "系统" : operatorName);
            if (mapper.updateFollowup(update) == 1)
                insertLog(before.getFollowupId(), "QUALIFY", before, update, null,
                        "命中进客规则：" + reason, null, before.getStatus(), update.getStatus(),
                        null, null, operatorName == null ? "系统" : operatorName);
        }
    }

    private void alignAssignmentStatus(AudienceFollowup value)
    {
        boolean assigned = value.getOwnerUserId() != null;
        if (assigned && Set.of("OBSERVING", "UNASSIGNED").contains(value.getStatus())) value.setStatus("PENDING");
        else if (!assigned && "PENDING".equals(value.getStatus()) && value.getLastContactAt() == null)
            value.setStatus(value.getQualifiedAt() == null ? "OBSERVING" : "UNASSIGNED");
    }

    private void validateFollowupFields(AudienceFollowup value, AudienceFollowup before)
    {
        String status = upper(trimToNull(value.getStatus()));
        if (status != null && !FOLLOWUP_STATUSES.contains(status)) throw badRequest("跟进状态不正确");
        if (status == null && before != null) status = before.getStatus();
        value.setStatus(status);
        String resultCode = upper(trimToNull(value.getFollowResultCode()));
        if (resultCode != null && !FOLLOW_RESULT_CODES.contains(resultCode)) throw badRequest("本次跟进结果不正确");
        value.setFollowResultCode(resultCode);
        String intentLevel = upper(trimToNull(value.getIntentLevel()));
        if (intentLevel != null && !INTENT_LEVELS.contains(intentLevel)) throw badRequest("客户意向等级不正确");
        value.setIntentLevel(intentLevel);
        String closeReasonCode = upper(trimToNull(value.getCloseReasonCode()));
        if (closeReasonCode != null && !CLOSE_REASON_CODES.contains(closeReasonCode)) throw badRequest("无效原因分类不正确");
        value.setCloseReasonCode(closeReasonCode);
        applyResultStatus(value);
        status = value.getStatus();
        if (("ORDERED".equals(status) || "CLOSED".equals(status)) && trimToNull(value.getOrderNo()) == null)
            throw badRequest("已下单或已完成必须先填写订单号");
        if ("INVALID".equals(status) && closeReasonCode == null && trimToNull(value.getCloseReason()) == null)
            throw badRequest("无效记录请选择或填写原因");
        if (value.getConsultModel() != null && value.getConsultModel().length() > 256) throw badRequest("咨询型号不能超过256个字符");
        if (value.getOrderNo() != null && value.getOrderNo().length() > 64) throw badRequest("订单号不能超过64个字符");
        String orderNo = trimToNull(value.getOrderNo());
        String previousOrderNo = before == null ? null : trimToNull(before.getOrderNo());
        if (orderNo != null && !orderNo.equals(previousOrderNo))
        {
            AudienceFollowup conflict = mapper.selectFollowupByOrderNo(orderNo, value.getFollowupId());
            if (conflict != null)
            {
                String customer = trimToNull(conflict.getNicknameSnapshot());
                throw badRequest("订单号已用于客户" + (customer == null ? "" : "“" + customer + "”") + "，请核对后再保存");
            }
        }
        if (value.getVersion() == null && before != null) value.setVersion(before.getVersion());
    }

    private void applyResultStatus(AudienceFollowup value)
    {
        if (value == null || value.getFollowResultCode() == null) return;
        String status = switch (value.getFollowResultCode())
        {
            case "NO_RESPONSE" -> "PENDING";
            case "CONTACTED" -> "CONTACTED";
            case "QUOTED" -> "QUOTED";
            case "CONSIDERING" -> "QUALIFIED";
            case "ORDER_PENDING" -> "ORDER_PENDING";
            case "ORDERED" -> "ORDERED";
            case "PAUSED" -> "PAUSED";
            case "INVALID" -> "INVALID";
            default -> value.getStatus();
        };
        value.setStatus(status);
    }

    private void applyResultReminder(AudienceFollowup value)
    {
        if (value == null || value.getNextFollowAt() != null || value.getFollowResultCode() == null
                || TERMINAL_FOLLOWUP_STATUSES.contains(value.getStatus())) return;
        int days = switch (value.getFollowResultCode())
        {
            case "CONSIDERING" -> 3;
            case "PAUSED" -> 7;
            default -> 1;
        };
        LocalDateTime reminder = LocalDateTime.now().plusDays(days)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        value.setNextFollowAt(Date.from(reminder.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void clearReminderForTerminalStatus(AudienceFollowup value)
    {
        if (value != null && TERMINAL_FOLLOWUP_STATUSES.contains(value.getStatus()))
        {
            value.setNextFollowAt(null);
        }
    }

    private void applyDefaultReminder(AudienceFollowup value)
    {
        if (value == null || value.getOwnerUserId() == null || value.getNextFollowAt() != null
                || TERMINAL_FOLLOWUP_STATUSES.contains(value.getStatus()) || "UNASSIGNED".equals(value.getStatus())) return;
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        value.setNextFollowAt(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void enrichFollowupStats(List<AudienceFollowup> rows)
    {
        if (rows == null || rows.isEmpty()) return;
        List<String> secUids = rows.stream().map(AudienceFollowup::getSecUid)
                .filter(Objects::nonNull).distinct().toList();
        if (secUids.isEmpty()) return;
        List<AudienceFollowup> stats = mapper.selectFollowupVisitStats(secUids);
        if (stats == null || stats.isEmpty()) return;
        Map<String, AudienceFollowup> byUid = new LinkedHashMap<>();
        for (AudienceFollowup stat : stats) byUid.put(stat.getSecUid(), stat);
        for (AudienceFollowup row : rows)
        {
            AudienceFollowup stat = byUid.get(row.getSecUid());
            if (stat == null) continue;
            row.setAppearanceDays(stat.getAppearanceDays());
            row.setBestCommentRank(stat.getBestCommentRank());
            row.setBestWatchRank(stat.getBestWatchRank());
            row.setLatestVisitDate(stat.getLatestVisitDate());
            row.setVisitDatesCsv(stat.getVisitDatesCsv());
            row.setConsecutiveDays(calculateConsecutiveDays(stat.getVisitDatesCsv()));
        }
    }

    private int calculateConsecutiveDays(String csv)
    {
        if (csv == null || csv.isBlank()) return 0;
        List<LocalDate> dates = new ArrayList<>();
        for (String value : csv.split(","))
        {
            try { dates.add(LocalDate.parse(value.trim(), DASH_DATE)); }
            catch (DateTimeParseException ignored) { }
        }
        if (dates.isEmpty()) return 0;
        dates = dates.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        int consecutive = 1;
        for (int index = 1; index < dates.size(); index++)
        {
            if (!dates.get(index - 1).minusDays(1).equals(dates.get(index))) break;
            consecutive++;
        }
        return consecutive;
    }

    private void ensureFollowupAccess(AudienceFollowup value, Long operatorUserId)
    {
        if (isElevated(operatorUserId)) return;
        if (operatorUserId == null || !operatorUserId.equals(value.getOwnerUserId()))
            throw new ServiceException("只能处理分配给自己的跟单记录", HttpStatus.FORBIDDEN);
    }

    private void ensureFollowupReadAccess(AudienceFollowup value, Long operatorUserId)
    {
        if (isElevated(operatorUserId)) return;
        boolean assigned = value.getOwnerUserId() != null;
        boolean owns = operatorUserId != null && operatorUserId.equals(value.getOwnerUserId());
        if (operatorUserId == null || (assigned && !owns))
            throw new ServiceException("无权查看其他人员的跟单记录", HttpStatus.FORBIDDEN);
    }

    private void applyFollowupVisibility(AudienceFollowupQuery query, Long currentUserId)
    {
        if (!isElevated(currentUserId))
        {
            query.setOnlyMine(true);
            query.setCurrentUserId(currentUserId);
        }
        else if (Boolean.TRUE.equals(query.getOnlyMine())) query.setCurrentUserId(currentUserId);
    }

    private boolean isElevated(Long operatorUserId)
    {
        return hasAssignPermission(operatorUserId);
    }

    boolean hasAssignPermission(Long operatorUserId)
    {
        if (operatorUserId != null && operatorUserId == 1L) return true;
        try { return SecurityUtils.hasPermi("live:audienceRank:followup:assign"); }
        catch (Exception ignored) { return false; }
    }

    boolean hasEditPermission(Long operatorUserId)
    {
        if (operatorUserId != null && operatorUserId == 1L) return true;
        try { return SecurityUtils.hasPermi("live:audienceRank:followup:edit"); }
        catch (Exception ignored) { return false; }
    }

    private void requireAssignPermission(Long operatorUserId)
    {
        if (!hasAssignPermission(operatorUserId))
            throw new ServiceException("修改跟单主播或场控需要分配权限", HttpStatus.FORBIDDEN);
    }

    private void requireEditPermission(Long operatorUserId)
    {
        if (!hasEditPermission(operatorUserId))
            throw new ServiceException("批量修改跟单资料需要编辑权限", HttpStatus.FORBIDDEN);
    }

    private void requireClaimPermission(Long operatorUserId)
    {
        if (!hasAssignPermission(operatorUserId) && !hasEditPermission(operatorUserId))
            throw new ServiceException("领取跟单需要编辑或分配权限", HttpStatus.FORBIDDEN);
    }

    private void syncOwnerSnapshot(AudienceFollowup target)
    {
        Long userId = target.getOwnerUserId();
        if (userId == null)
        {
            target.setOwnerNameSnapshot(null);
            return;
        }
        Map<String, Object> matched = mapper.selectAllActiveUsers().stream()
                .filter(row -> userId.equals(toLong(row.get("userId"))))
                .findFirst()
                .orElseThrow(() -> badRequest("选择的领取人不是启用账号"));
        String name = trimToNull(Objects.toString(matched.get("userName"), null));
        if (name == null) throw badRequest("选择的领取人名称无效");
        target.setOwnerNameSnapshot(name);
    }

    private void syncAssigneeSnapshot(AudienceFollowup target, Long roomId, String roleCode)
    {
        boolean anchor = "anchor".equals(roleCode);
        Long userId = anchor ? target.getAnchorUserId() : target.getControllerUserId();
        if (userId == null)
        {
            if (anchor) target.setAnchorNameSnapshot(null);
            else target.setControllerNameSnapshot(null);
            return;
        }
        if (roomId == null) throw badRequest("该跟单记录尚未匹配直播间，不能分配主播或场控");
        Map<String, Object> matched = mapper.selectFollowupAssignees(roomId, roleCode).stream()
                .filter(row -> userId.equals(toLong(row.get("userId"))))
                .findFirst()
                .orElseThrow(() -> badRequest("选择的" + (anchor ? "主播" : "场控") + "未映射到该直播间"));
        String name = trimToNull(Objects.toString(matched.get("userName"), null));
        if (name == null) throw badRequest("选择的" + (anchor ? "主播" : "场控") + "名称无效");
        if (anchor) target.setAnchorNameSnapshot(name);
        else target.setControllerNameSnapshot(name);
    }

    private Long resolveFollowupRoomId(AudienceFollowup followup)
    {
        if (followup.getRoomId() != null) return followup.getRoomId();
        String roomName = trimToNull(followup.getRoomNameSnapshot());
        if (roomName == null) return null;
        List<AudienceRankRoomMatch> matches = mapper.selectMatchingRooms(roomName);
        if (matches == null || matches.isEmpty()) return null;
        if (matches.size() > 1)
            throw badRequest("直播间名称匹配到多条直播资料，请先确保直播间名称唯一");
        return matches.get(0).getRoomId();
    }

    private AudienceFollowup copyFollowup(AudienceFollowup source)
    {
        AudienceFollowup target = new AudienceFollowup();
        target.setFollowupId(source.getFollowupId()); target.setProfileId(source.getProfileId());
        target.setRoomScopeKey(source.getRoomScopeKey()); target.setRoomId(source.getRoomId());
        target.setRoomNameSnapshot(source.getRoomNameSnapshot()); target.setSecUid(source.getSecUid());
        target.setNicknameSnapshot(source.getNicknameSnapshot()); target.setContactPhone(source.getContactPhone());
        target.setContactWechat(source.getContactWechat()); target.setOwnerUserId(source.getOwnerUserId());
        target.setOwnerNameSnapshot(source.getOwnerNameSnapshot()); target.setAnchorUserId(source.getAnchorUserId());
        target.setAnchorNameSnapshot(source.getAnchorNameSnapshot()); target.setControllerUserId(source.getControllerUserId());
        target.setControllerNameSnapshot(source.getControllerNameSnapshot()); target.setStatus(source.getStatus());
        target.setFollowResultCode(source.getFollowResultCode()); target.setIntentLevel(source.getIntentLevel());
        target.setConsultModel(source.getConsultModel()); target.setOrderNo(source.getOrderNo());
        target.setOrderCount(source.getOrderCount());
        target.setPriority(source.getPriority()); target.setReactivationPending(source.getReactivationPending());
        target.setQualificationReason(source.getQualificationReason()); target.setQualifiedAt(source.getQualifiedAt());
        target.setLastContactAt(source.getLastContactAt());
        target.setNextFollowAt(source.getNextFollowAt()); target.setLastFollowResult(source.getLastFollowResult());
        target.setRemark(source.getRemark()); target.setCloseReason(source.getCloseReason());
        target.setCloseReasonCode(source.getCloseReasonCode()); target.setVersion(source.getVersion());
        return target;
    }

    private void insertLog(Long followupId, String action, AudienceFollowup before, AudienceFollowup after,
                           String method, String content, String result, String statusBefore, String statusAfter,
                           Date nextFollowAt, Long operatorUserId, String operatorName)
    {
        AudienceFollowupLog log = new AudienceFollowupLog();
        log.setFollowupId(followupId); log.setActionType(action);
        log.setBeforeJson(before == null ? null : JSON.toJSONString(before));
        log.setAfterJson(after == null ? null : JSON.toJSONString(after));
        log.setContactMethod(method); log.setContent(content); log.setResult(result);
        log.setStatusBefore(statusBefore); log.setStatusAfter(statusAfter); log.setNextFollowAt(nextFollowAt);
        log.setOperatorUserId(operatorUserId); log.setOperatorNameSnapshot(operatorName);
        mapper.insertFollowupLog(log);
    }

    private void normalizeFollowupQuery(AudienceFollowupQuery query)
    {
        if (query == null) throw badRequest("查询条件不能为空");
        query.setRoomName(normalizeOptionalText(query.getRoomName(), "直播间名称", 128));
        query.setKeyword(normalizeOptionalText(query.getKeyword(), "搜索内容", 256));
        query.setStatus(upper(trimToNull(query.getStatus())));
        if (query.getStatus() != null && !FOLLOWUP_STATUSES.contains(query.getStatus())) throw badRequest("跟进状态不正确");
        query.setStage(upper(trimToNull(query.getStage())));
        if (query.getStage() != null && !FOLLOWUP_STAGES.contains(query.getStage())) throw badRequest("客户阶段不正确");
        query.setIntentLevel(upper(trimToNull(query.getIntentLevel())));
        if (query.getIntentLevel() != null && !INTENT_LEVELS.contains(query.getIntentLevel())) throw badRequest("意向等级不正确");
        query.setFollowResultCode(upper(trimToNull(query.getFollowResultCode())));
        if (query.getFollowResultCode() != null && !FOLLOW_RESULT_CODES.contains(query.getFollowResultCode())) throw badRequest("跟进结果不正确");
        if (query.getBeginDate() != null && query.getEndDate() != null && query.getBeginDate().isAfter(query.getEndDate()))
            throw badRequest("开始日期不能晚于结束日期");
        if (query.getMinPayLevel() != null && (query.getMinPayLevel() < 0 || query.getMinPayLevel() > 1000))
            throw badRequest("消费等级筛选不正确");
        if (query.getMaxCommentRank() != null && (query.getMaxCommentRank() < 1 || query.getMaxCommentRank() > 500))
            throw badRequest("评论榜名次筛选不正确");
        if (query.getMaxWatchRank() != null && (query.getMaxWatchRank() < 1 || query.getMaxWatchRank() > 500))
            throw badRequest("观看榜名次筛选不正确");
    }

    private Long toLong(Object value)
    {
        if (value == null || Objects.toString(value, "").isBlank()) return null;
        try { return Long.valueOf(value.toString()); } catch (NumberFormatException ex) { throw badRequest("人员编号格式不正确"); }
    }

    private Boolean toBoolean(Object value)
    {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.valueOf(value.toString());
    }

    private Date toDate(Object value)
    {
        if (value == null || Objects.toString(value, "").isBlank()) return null;
        if (value instanceof Date date) return date;
        String text = value.toString().trim();
        try { return Date.from(Instant.parse(text)); }
        catch (DateTimeException ex)
        {
            try
            {
                return Date.from(java.time.LocalDateTime.parse(text,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant());
            }
            catch (DateTimeException ignored)
            {
                throw badRequest("跟进时间格式不正确");
            }
        }
    }

    private PreparedImport prepare(AudienceRankImportRequest request)
    {
        if (request == null)
        {
            throw badRequest("上传内容不能为空");
        }
        String roomName = normalizeRequiredText(request.getRoomName(), "直播间名称", 128);
        LocalDate commentDataDate = parseDataDate(request.getCommentDataDate(), "评论榜数据日期");
        LocalDate watchDataDate = parseDataDate(request.getWatchDataDate(), "观看榜数据日期");
        List<AudienceCommentRankItem> commentRanks = request.getCommentRanks();
        List<AudienceWatchRankItem> watchRanks = request.getWatchRanks();
        validateList(commentRanks, "评论榜");
        validateList(watchRanks, "观看榜");
        if (commentRanks.isEmpty() && watchRanks.isEmpty())
        {
            throw badRequest("评论榜和观看榜不能同时为空");
        }

        Map<String, AudienceRankSnapshot> merged = new LinkedHashMap<>();
        mergeCommentRanks(commentRanks, commentDataDate, merged);
        mergeWatchRanks(watchRanks, watchDataDate, merged);
        List<AudienceRankSnapshot> snapshots = new ArrayList<>(merged.values());
        snapshots.sort(Comparator.comparing(AudienceRankSnapshot::getSecUid));

        PreparedImport prepared = new PreparedImport();
        prepared.roomName = roomName;
        prepared.roomScopeKey = sha256Hex(roomName.toLowerCase(Locale.ROOT));
        prepared.commentDataDate = commentDataDate;
        prepared.watchDataDate = watchDataDate;
        prepared.commentRowCount = commentRanks.size();
        prepared.watchRowCount = watchRanks.size();
        prepared.snapshots = snapshots;
        prepared.payloadHash = calculatePayloadHash(prepared);
        return prepared;
    }

    private void mergeCommentRanks(List<AudienceCommentRankItem> items, LocalDate dataDate,
            Map<String, AudienceRankSnapshot> merged)
    {
        Set<String> secUids = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (AudienceCommentRankItem item : items)
        {
            ValidatedCommon common = validateCommon(item, "评论榜");
            if (!secUids.add(common.secUid))
            {
                throw badRequest("评论榜存在重复的secUid：" + common.secUid);
            }
            if (!ranks.add(common.rank))
            {
                throw badRequest("评论榜存在重复名次：" + common.rank);
            }
            if (item.getCommentCount() == null || item.getCommentCount() < 0)
            {
                throw badRequest("评论次数不能小于0");
            }
            AudienceRankSnapshot snapshot = merged.computeIfAbsent(common.secUid, key -> new AudienceRankSnapshot());
            mergeCommon(snapshot, common);
            snapshot.setCommentCount(item.getCommentCount());
            snapshot.setCommentRank(common.rank);
            snapshot.setCommentDataDate(dataDate);
        }
    }

    private void mergeWatchRanks(List<AudienceWatchRankItem> items, LocalDate dataDate,
            Map<String, AudienceRankSnapshot> merged)
    {
        Set<String> secUids = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (AudienceWatchRankItem item : items)
        {
            ValidatedCommon common = validateCommon(item, "观看榜");
            if (!secUids.add(common.secUid))
            {
                throw badRequest("观看榜存在重复的secUid：" + common.secUid);
            }
            if (!ranks.add(common.rank))
            {
                throw badRequest("观看榜存在重复名次：" + common.rank);
            }
            if (item.getWatchSeconds() == null || item.getWatchSeconds() < 0)
            {
                throw badRequest("观看时长不能小于0");
            }
            AudienceRankSnapshot snapshot = merged.computeIfAbsent(common.secUid, key -> new AudienceRankSnapshot());
            mergeCommon(snapshot, common);
            snapshot.setWatchSeconds(item.getWatchSeconds());
            snapshot.setWatchRank(common.rank);
            snapshot.setWatchDataDate(dataDate);
        }
    }

    private ValidatedCommon validateCommon(AudienceRankUserItem item, String rankingName)
    {
        if (item == null)
        {
            throw badRequest(rankingName + "存在空记录");
        }
        if (item.getRank() == null || item.getRank() <= 0 || item.getRank() > 100000)
        {
            throw badRequest(rankingName + "名次必须在1到100000之间");
        }
        ValidatedCommon common = new ValidatedCommon();
        common.rank = item.getRank();
        common.secUid = normalizeRequiredText(item.getSecUid(), "secUid", 256);
        common.nickname = normalizeRequiredText(item.getNickname(), "昵称", 128);
        common.isFollower = item.getIsFollower();
        common.isFollowing = item.getIsFollowing();
        common.payLevel = item.getPayLevel();
        if (common.payLevel != null && (common.payLevel < 0 || common.payLevel > 1000))
        {
            throw badRequest("消费等级必须在0到1000之间");
        }
        common.payIconUrl = normalizeUrl(item.getPayIconUrl());
        return common;
    }

    private void mergeCommon(AudienceRankSnapshot snapshot, ValidatedCommon common)
    {
        if (snapshot.getSecUid() == null)
        {
            snapshot.setSecUid(common.secUid);
            snapshot.setNickname(common.nickname);
        }
        snapshot.setIsFollower(mergeBoolean(snapshot.getIsFollower(), common.isFollower));
        snapshot.setIsFollowing(mergeBoolean(snapshot.getIsFollowing(), common.isFollowing));
        Integer currentLevel = snapshot.getPayLevel();
        if (common.payLevel != null && (currentLevel == null || common.payLevel > currentLevel))
        {
            snapshot.setPayLevel(common.payLevel);
            snapshot.setPayIconUrl(common.payIconUrl);
        }
        else if (currentLevel == null && common.payIconUrl != null)
        {
            snapshot.setPayIconUrl(common.payIconUrl);
        }
        else if (common.payLevel != null && common.payLevel.equals(currentLevel)
                && snapshot.getPayIconUrl() == null)
        {
            snapshot.setPayIconUrl(common.payIconUrl);
        }
    }

    private Boolean mergeBoolean(Boolean current, Boolean incoming)
    {
        if (current == null) return incoming;
        if (incoming == null) return current;
        return current || incoming;
    }

    private AudienceRankBatch buildBatch(PreparedImport prepared, Date capturedAt, String uploadedIp)
    {
        AudienceRankBatch batch = new AudienceRankBatch();
        batch.setPayloadHash(prepared.payloadHash);
        batch.setRoomScopeKey(prepared.roomScopeKey);
        batch.setRoomName(prepared.roomName);
        batch.setCommentDataDate(prepared.commentDataDate);
        batch.setWatchDataDate(prepared.watchDataDate);
        batch.setCapturedAt(capturedAt);
        batch.setCommentRowCount(prepared.commentRowCount);
        batch.setWatchRowCount(prepared.watchRowCount);
        batch.setUniqueUserCount(prepared.snapshots.size());
        batch.setUploadedIp(normalizeIp(uploadedIp));
        return batch;
    }

    private void applyRoomMatch(AudienceRankBatch batch, List<AudienceRankRoomMatch> matches)
    {
        if (matches == null || matches.isEmpty())
        {
            batch.setRoomMatchStatus("UNMATCHED");
            return;
        }
        if (matches.size() > 1)
        {
            batch.setRoomMatchStatus("AMBIGUOUS");
            return;
        }
        AudienceRankRoomMatch match = matches.get(0);
        batch.setRoomId(match.getRoomId());
        batch.setMatchedRoomName(match.getRoomName());
        batch.setRoomMatchStatus("MATCHED");
    }

    private List<AudienceRankProfile> buildProfiles(AudienceRankBatch batch, List<AudienceRankSnapshot> snapshots)
    {
        List<AudienceRankProfile> profiles = new ArrayList<>(snapshots.size());
        for (AudienceRankSnapshot snapshot : snapshots)
        {
            AudienceRankProfile profile = new AudienceRankProfile();
            profile.setRoomScopeKey(batch.getRoomScopeKey());
            profile.setRoomName(batch.getRoomName());
            profile.setRoomId(batch.getRoomId());
            profile.setSecUid(snapshot.getSecUid());
            profile.setNickname(snapshot.getNickname());
            profile.setIsFollower(snapshot.getIsFollower());
            profile.setIsFollowing(snapshot.getIsFollowing());
            profile.setPayLevel(snapshot.getPayLevel());
            profile.setPayIconUrl(snapshot.getPayIconUrl());
            profile.setFirstBatchId(batch.getBatchId());
            profile.setLastBatchId(batch.getBatchId());
            profile.setFirstSeenAt(batch.getCapturedAt());
            profile.setLastSeenAt(batch.getCapturedAt());
            profiles.add(profile);
        }
        return profiles;
    }

    private AudienceRankImportResult toResult(AudienceRankBatch batch, boolean duplicate)
    {
        AudienceRankImportResult result = new AudienceRankImportResult();
        result.setBatchId(batch.getBatchId());
        result.setDuplicate(duplicate);
        result.setPayloadHash(batch.getPayloadHash());
        result.setRoomName(batch.getRoomName());
        result.setRoomId(batch.getRoomId());
        result.setMatchedRoomName(batch.getMatchedRoomName());
        result.setRoomMatchStatus(batch.getRoomMatchStatus());
        result.setCommentRowCount(batch.getCommentRowCount());
        result.setWatchRowCount(batch.getWatchRowCount());
        result.setUniqueUserCount(batch.getUniqueUserCount());
        result.setNewCustomerCount(batch.getNewCustomerCount());
        result.setUpdatedCustomerCount(batch.getUpdatedCustomerCount());
        result.setCapturedAt(batch.getCapturedAt());
        return result;
    }

    private String calculatePayloadHash(PreparedImport prepared)
    {
        MessageDigest digest = newSha256();
        updateDigest(digest, "audience-rank-v1");
        updateDigest(digest, prepared.roomScopeKey);
        updateDigest(digest, prepared.commentDataDate);
        updateDigest(digest, prepared.watchDataDate);
        updateDigest(digest, prepared.commentRowCount);
        updateDigest(digest, prepared.watchRowCount);
        for (AudienceRankSnapshot snapshot : prepared.snapshots)
        {
            updateDigest(digest, snapshot.getSecUid());
            updateDigest(digest, snapshot.getNickname());
            updateDigest(digest, snapshot.getIsFollower());
            updateDigest(digest, snapshot.getIsFollowing());
            updateDigest(digest, snapshot.getPayLevel());
            updateDigest(digest, snapshot.getPayIconUrl());
            updateDigest(digest, snapshot.getCommentCount());
            updateDigest(digest, snapshot.getCommentRank());
            updateDigest(digest, snapshot.getWatchSeconds());
            updateDigest(digest, snapshot.getWatchRank());
        }
        return toHex(digest.digest());
    }

    private void updateDigest(MessageDigest digest, Object value)
    {
        if (value == null)
        {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private String sha256Hex(String value)
    {
        return toHex(newSha256().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private MessageDigest newSha256()
    {
        try
        {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String toHex(byte[] bytes)
    {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes)
        {
            value.append(Character.forDigit((item >>> 4) & 0x0f, 16));
            value.append(Character.forDigit(item & 0x0f, 16));
        }
        return value.toString();
    }

    private void normalizeQuery(AudienceRankQuery query)
    {
        if (query == null)
        {
            throw badRequest("查询条件不能为空");
        }
        query.setRoomName(normalizeOptionalText(query.getRoomName(), "直播间名称", 128));
        query.setKeyword(normalizeOptionalText(query.getKeyword(), "搜索内容", 256));
        query.setNickname(normalizeOptionalText(query.getNickname(), "昵称", 128));
        query.setSecUid(normalizeOptionalText(query.getSecUid(), "secUid", 256));
        String rankType = lower(trimToNull(query.getRankType()));
        if (rankType != null && !Set.of("comment", "watch", "all", "both").contains(rankType))
        {
            throw badRequest("榜单类型不正确");
        }
        if ("both".equals(rankType))
        {
            rankType = "all";
        }
        query.setRankType(rankType);
        if (query.getBeginDataDate() != null && query.getEndDataDate() != null
                && query.getBeginDataDate().isAfter(query.getEndDataDate()))
        {
            throw badRequest("数据开始日期不能晚于结束日期");
        }
    }

    private LocalDate parseDataDate(String value, String fieldName)
    {
        String normalized = trimToNull(value);
        if (normalized == null)
        {
            throw badRequest(fieldName + "不能为空");
        }
        try
        {
            return normalized.indexOf('.') >= 0 ? LocalDate.parse(normalized, DOT_DATE)
                    : LocalDate.parse(normalized, DASH_DATE);
        }
        catch (DateTimeParseException e)
        {
            throw badRequest(fieldName + "格式必须为yyyy.MM.dd或yyyy-MM-dd");
        }
    }

    private Date resolveCapturedAt(Long capturedAt)
    {
        long now = System.currentTimeMillis();
        long value = capturedAt == null ? now : capturedAt;
        if (value <= 0 || value > now + MAX_CLOCK_SKEW_MILLIS)
        {
            throw badRequest("采集时间不正确");
        }
        try
        {
            return Date.from(Instant.ofEpochMilli(value));
        }
        catch (DateTimeException e)
        {
            throw badRequest("采集时间不正确");
        }
    }

    private <T> void validateList(List<T> list, String fieldName)
    {
        if (list == null)
        {
            throw badRequest(fieldName + "数据不能为空");
        }
        if (list.size() > MAX_ROWS_PER_RANKING)
        {
            throw badRequest(fieldName + "单次不能超过" + MAX_ROWS_PER_RANKING + "人");
        }
    }

    private String normalizeRequiredText(String value, String fieldName, int maxLength)
    {
        String normalized = normalizeText(value);
        if (normalized == null)
        {
            throw badRequest(fieldName + "不能为空");
        }
        if (normalized.length() > maxLength)
        {
            throw badRequest(fieldName + "长度不能超过" + maxLength);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String fieldName, int maxLength)
    {
        String normalized = normalizeText(value);
        if (normalized != null && normalized.length() > maxLength)
        {
            throw badRequest(fieldName + "长度不能超过" + maxLength);
        }
        return normalized;
    }

    private String normalizeText(String value)
    {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        return Normalizer.normalize(trimmed, Normalizer.Form.NFKC).replaceAll("\\s+", " ").trim();
    }

    private String normalizeUrl(String value)
    {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        if (normalized.length() > 1000)
        {
            throw badRequest("等级图标地址长度不能超过1000");
        }
        try
        {
            URI uri = new URI(normalized);
            String scheme = lower(uri.getScheme());
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null)
            {
                throw badRequest("等级图标地址必须是有效的http或https地址");
            }
        }
        catch (URISyntaxException e)
        {
            throw badRequest("等级图标地址格式不正确");
        }
        return normalized;
    }

    private String normalizeIp(String uploadedIp)
    {
        String value = trimToNull(uploadedIp);
        if (value == null) return null;
        int comma = value.indexOf(',');
        if (comma >= 0) value = value.substring(0, comma).trim();
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private String trimToNull(String value)
    {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String lower(String value)
    {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String upper(String value)
    {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private ServiceException badRequest(String message)
    {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    private static class ValidatedCommon
    {
        private Integer rank;
        private String secUid;
        private String nickname;
        private Boolean isFollower;
        private Boolean isFollowing;
        private Integer payLevel;
        private String payIconUrl;
    }

    private static class PreparedImport
    {
        private String roomName;
        private String roomScopeKey;
        private LocalDate commentDataDate;
        private LocalDate watchDataDate;
        private int commentRowCount;
        private int watchRowCount;
        private List<AudienceRankSnapshot> snapshots;
        private String payloadHash;
    }
}
