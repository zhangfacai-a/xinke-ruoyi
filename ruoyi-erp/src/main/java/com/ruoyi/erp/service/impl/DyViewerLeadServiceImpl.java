package com.ruoyi.erp.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.domain.DyCaptureReport;
import com.ruoyi.erp.domain.DyViewerLeadExport;
import com.ruoyi.erp.domain.DyViewerCommentPayload;
import com.ruoyi.erp.domain.DyViewerPayload;
import com.ruoyi.erp.mapper.DyViewerLeadMapper;
import com.ruoyi.erp.service.IDyViewerLeadService;

@Service
public class DyViewerLeadServiceImpl implements IDyViewerLeadService
{
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DyViewerLeadMapper dyViewerLeadMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> report(DyCaptureReport report)
    {
        validateReport(report);
        String payloadType = normalize(report.getPayloadType());
        int itemCount = itemCount(payloadType, report);
        Long batchId = insertBatch(report, payloadType, itemCount);

        int inserted = 0;
        int leadTouched = 0;
        if ("audiences".equals(payloadType) || "online_audiences".equals(payloadType))
        {
            List<DyViewerPayload> audiences = report.getAudiences();
            if (audiences != null)
            {
                for (DyViewerPayload audience : audiences)
                {
                    if (!hasText(audience.getSecUid()))
                    {
                        continue;
                    }
                    upsertViewerLead(report, audience);
                    inserted++;
                    leadTouched++;
                }
            }
        }
        else if ("comments".equals(payloadType))
        {
            List<DyViewerCommentPayload> comments = report.getComments();
            if (comments != null)
            {
                for (DyViewerCommentPayload comment : comments)
                {
                    int saved = insertComment(report, comment);
                    inserted += saved;
                    if (saved > 0)
                    {
                        leadTouched++;
                    }
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("batchId", batchId);
        data.put("payloadType", payloadType);
        data.put("received", itemCount);
        data.put("inserted", inserted);
        data.put("duplicated", Math.max(itemCount - inserted, 0));
        data.put("leadTouched", leadTouched);
        return data;
    }

    @Override
    public List<Map<String, Object>> listRooms(Map<String, Object> query)
    {
        return dyViewerLeadMapper.selectLiveRoomList(query);
    }

    @Override
    public Map<String, Object> getRoom(String roomKey)
    {
        return dyViewerLeadMapper.selectLiveRoomByKey(roomKey);
    }

    @Override
    public int addRoom(Map<String, Object> form)
    {
        Map<String, Object> safeForm = form == null ? new HashMap<>() : form;
        String roomKey = trim(safeForm.get("roomKey"), 64);
        String roomName = trim(safeForm.get("roomName"), 100);
        if (!hasText(roomKey))
        {
            throw new ServiceException("roomKey is required");
        }
        if (!hasText(roomName))
        {
            throw new ServiceException("roomName is required");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("roomKey", roomKey);
        data.put("roomName", roomName);
        data.put("source", trim(safeForm.get("source"), 64));
        data.put("status", hasText(str(safeForm.get("status"))) ? str(safeForm.get("status")) : "0");
        data.put("remark", trim(safeForm.get("remark"), 500));
        return dyViewerLeadMapper.insertLiveRoom(data);
    }

    @Override
    public int updateRoom(String roomKey, Map<String, Object> form)
    {
        if (!hasText(roomKey))
        {
            throw new ServiceException("roomKey is required");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("roomKey", roomKey);
        copy(data, form, "roomName");
        copy(data, form, "source");
        copy(data, form, "status");
        copy(data, form, "remark");
        return dyViewerLeadMapper.updateLiveRoom(data);
    }

    @Override
    public int deleteRoom(String roomKey)
    {
        return dyViewerLeadMapper.deleteLiveRoomByKey(roomKey);
    }

    @Override
    public List<Map<String, Object>> listLeads(Map<String, Object> query)
    {
        refreshLeadRules();
        return dyViewerLeadMapper.selectLeadList(query);
    }

    @Override
    public List<DyViewerLeadExport> exportLeads(Map<String, Object> query)
    {
        refreshLeadRules();
        List<Map<String, Object>> leads = dyViewerLeadMapper.selectLeadList(query);
        List<DyViewerLeadExport> rows = new ArrayList<>();
        for (Map<String, Object> lead : leads)
        {
            DyViewerLeadExport row = new DyViewerLeadExport();
            Long leadId = asLong(lead.get("lead_id"));
            row.setLeadDate(str(lead.get("lead_date")));
            row.setLiveRoomName(str(lead.get("live_room_name")));
            row.setNickname(str(lead.get("nickname")));
            row.setProfileUrl(profileUrl(lead.get("sec_uid")));
            row.setCommentCount(asInteger(lead.get("comment_count")));
            row.setComments(joinComments(leadId));
            row.setIntent(intentLabel(str(lead.get("intent"))));
            row.setStatus(statusLabel(str(lead.get("status"))));
            row.setOwnerName(str(lead.get("owner_name")));
            row.setOrderNo(str(lead.get("order_no")));
            row.setRemark(str(lead.get("remark")));
            row.setUpdateTime(str(lead.get("update_time")));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<String> listRoomSuggestions(String keyword)
    {
        return dyViewerLeadMapper.selectRoomNames(trim(keyword, 100));
    }

    @Override
    public List<String> listOwnerSuggestions(String keyword)
    {
        return dyViewerLeadMapper.selectOwnerNames(trim(keyword, 50));
    }

    @Override
    public Map<String, Object> getLeadDetail(Long leadId)
    {
        refreshLeadRules();
        Map<String, Object> data = new HashMap<>();
        data.put("lead", dyViewerLeadMapper.selectLeadById(leadId));
        data.put("comments", dyViewerLeadMapper.selectCommentsByLeadId(leadId));
        data.put("visits", dyViewerLeadMapper.selectVisitsByLeadId(leadId));
        data.put("followRecords", dyViewerLeadMapper.selectFollowRecordsByLeadId(leadId));
        return data;
    }

    @Override
    public int updateLead(Long leadId, Map<String, Object> form)
    {
        refreshLeadRules();
        Map<String, Object> current = dyViewerLeadMapper.selectLeadById(leadId);
        if (current == null)
        {
            throw new ServiceException("Lead does not exist");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("leadId", leadId);
        copy(data, form, "ownerId");
        copy(data, form, "orderNo");
        copy(data, form, "remark");

        String nextOrderNo = form != null && form.containsKey("orderNo") ? str(form.get("orderNo")) : str(current.get("order_no"));
        String requestedStatus = form != null && form.containsKey("status") ? str(form.get("status")) : str(current.get("status"));
        String nextOwnerName = str(current.get("owner_name"));

        if (form != null && form.containsKey("ownerName"))
        {
            nextOwnerName = str(form.get("ownerName"));
            applyOwnerRule(data, current, nextOwnerName);
        }
        data.put("status", resolveLeadStatus(current, nextOwnerName, nextOrderNo, requestedStatus));
        return dyViewerLeadMapper.updateViewerLead(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addFollowRecord(Long leadId, Map<String, Object> form, Long operatorId, String operatorName)
    {
        Map<String, Object> lead = dyViewerLeadMapper.selectLeadById(leadId);
        if (lead == null)
        {
            throw new ServiceException("Lead does not exist");
        }
        String content = str(form.get("followContent"));
        if (!hasText(content))
        {
            throw new ServiceException("followContent is required");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("leadId", leadId);
        data.put("viewerId", lead.get("viewer_id"));
        data.put("followContent", content);
        data.put("followResult", str(form.get("followResult")));
        data.put("nextFollowTime", null);
        data.put("operatorId", operatorId);
        String leadOwnerName = str(lead.get("owner_name"));
        data.put("operatorName", hasText(leadOwnerName) ? leadOwnerName : operatorName);
        int rows = dyViewerLeadMapper.insertFollowRecord(data);

        Map<String, Object> leadUpdate = new HashMap<>();
        leadUpdate.put("leadId", leadId);
        dyViewerLeadMapper.updateViewerLead(leadUpdate);
        dyViewerLeadMapper.syncLeadStatusRules();
        dyViewerLeadMapper.syncViewerLeadRules();
        return rows;
    }

    @Override
    public Map<String, Object> summary(Map<String, Object> query)
    {
        refreshLeadRules();
        return dyViewerLeadMapper.selectSummary(query);
    }

    @Override
    public Map<String, Object> biDashboard(Map<String, Object> query)
    {
        refreshLeadRules();
        Map<String, Object> safeQuery = normalizeBiQuery(query);
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> overview = dyViewerLeadMapper.selectBiOverview(safeQuery);
        data.put("query", safeQuery);
        data.put("overview", overview == null ? new HashMap<>() : overview);
        data.put("trend", dyViewerLeadMapper.selectBiDailyTrend(safeQuery));
        data.put("roomRank", dyViewerLeadMapper.selectBiRoomRank(safeQuery));
        data.put("shopRank", dyViewerLeadMapper.selectBiShopRank(safeQuery));
        data.put("ownerRank", dyViewerLeadMapper.selectBiOwnerRank(safeQuery));
        data.put("hourHeat", dyViewerLeadMapper.selectBiHourHeat(safeQuery));
        data.put("keywords", dyViewerLeadMapper.selectBiCommentKeywords(safeQuery));
        data.put("signalWords", dyViewerLeadMapper.selectBiSignalWords(safeQuery));
        data.put("quality", dyViewerLeadMapper.selectBiDataQuality(safeQuery));
        return data;
    }

    private void validateReport(DyCaptureReport report)
    {
        if (report == null)
        {
            throw new ServiceException("Report is required");
        }
        if (!hasText(report.getRoomKey()))
        {
            throw new ServiceException("roomKey is required");
        }
        if (!hasText(report.getPayloadType()))
        {
            throw new ServiceException("payloadType is required");
        }
    }

    private Map<String, Object> normalizeBiQuery(Map<String, Object> query)
    {
        Map<String, Object> data = query == null ? new HashMap<>() : new HashMap<>(query);
        if (!hasText(str(data.get("beginLeadDate"))) && !hasText(str(data.get("leadDate"))))
        {
            data.put("beginLeadDate", LocalDate.now().minusDays(6).format(DATE_FMT));
        }
        if (!hasText(str(data.get("endLeadDate"))) && !hasText(str(data.get("leadDate"))))
        {
            data.put("endLeadDate", LocalDate.now().format(DATE_FMT));
        }
        data.put("roomName", trim(data.get("roomName"), 100));
        data.put("ownerName", trim(data.get("ownerName"), 50));
        return data;
    }

    private Long insertBatch(DyCaptureReport report, String payloadType, int itemCount)
    {
        upsertLiveRoom(report.getRoomKey(), report.getLiveRoomName(), report.getSource());
        Map<String, Object> data = new HashMap<>();
        data.put("batchNo", hasText(report.getBatchNo()) ? report.getBatchNo() : nextBatchNo(report.getRoomKey(), payloadType));
        data.put("roomKey", report.getRoomKey());
        data.put("douyinUserId", report.getUserId());
        data.put("payloadType", payloadType);
        data.put("source", report.getSource());
        data.put("liveRoomName", trim(report.getLiveRoomName(), 100));
        data.put("pageUrl", trim(report.getPageUrl(), 1000));
        data.put("requestUrl", trim(report.getRequestUrl(), 1000));
        data.put("queueSize", report.getQueueSize() == null ? 0 : report.getQueueSize());
        data.put("itemCount", itemCount);
        data.put("rawPayload", JSON.toJSONString(report));
        dyViewerLeadMapper.insertCaptureBatch(data);
        return asLong(data.get("batchId"));
    }

    private void upsertViewerLead(DyCaptureReport report, DyViewerPayload audience)
    {
        String leadDate = dateOf(audience.getCapturedAt());
        String roomKey = captureRoomKey(report, audience);
        String roomName = trim(report.getLiveRoomName(), 100);
        upsertLiveRoom(roomKey, roomName, report.getSource());

        Map<String, Object> viewer = new HashMap<>();
        viewer.put("secUid", audience.getSecUid());
        viewer.put("nickname", trim(audience.getNickname(), 100));
        viewer.put("avatar", trim(audience.getAvatar(), 1000));
        viewer.put("gender", audience.getGender() == null ? 0 : audience.getGender());
        viewer.put("leadDate", leadDate);
        dyViewerLeadMapper.insertViewer(viewer);
        Long viewerId = dyViewerLeadMapper.selectViewerIdBySecUid(audience.getSecUid());

        Map<String, Object> lead = new HashMap<>();
        lead.put("leadDate", leadDate);
        lead.put("roomKey", roomKey);
        lead.put("douyinUserId", report.getUserId());
        lead.put("liveRoomName", roomName);
        lead.put("viewerId", viewerId);
        lead.put("secUid", audience.getSecUid());
        lead.put("nickname", trim(audience.getNickname(), 100));
        lead.put("avatar", trim(audience.getAvatar(), 1000));
        dyViewerLeadMapper.insertDailyLead(lead);

        Long leadId = dyViewerLeadMapper.selectLeadId(leadDate, roomKey, audience.getSecUid());
        if (leadId != null && hasText(audience.getNickname()))
        {
            dyViewerLeadMapper.bindUnmatchedComments(leadId, viewerId, leadDate, roomKey, audience.getNickname());
            dyViewerLeadMapper.refreshLeadCommentStats(leadId);
            dyViewerLeadMapper.updateViewerCommentStats(viewerId);
        }
    }

    private int insertComment(DyCaptureReport report, DyViewerCommentPayload comment)
    {
        if (comment == null || !hasText(comment.getContent()))
        {
            return 0;
        }
        String roomKey = trim(report.getRoomKey(), 64);
        String capturedAt = dateTimeOf(comment.getCapturedAt());
        String leadDate = dateOf(comment.getCapturedAt());
        Long leadId = null;
        Long viewerId = null;
        String secUid = trim(comment.getSecUid(), 128);
        String matchType = "unmatched";

        if (hasText(secUid))
        {
            viewerId = dyViewerLeadMapper.selectViewerIdBySecUid(secUid);
            leadId = dyViewerLeadMapper.selectLeadId(leadDate, roomKey, secUid);
            matchType = leadId == null ? "unmatched" : "sec_uid";
        }
        if (leadId == null && hasText(comment.getNickname()))
        {
            Map<String, Object> lead = dyViewerLeadMapper.selectLeadByNickname(leadDate, roomKey, comment.getNickname());
            if (lead == null)
            {
                lead = dyViewerLeadMapper.selectLeadByNicknameAnyRoom(leadDate, comment.getNickname());
            }
            if (lead != null)
            {
                leadId = asLong(lead.get("lead_id"));
                viewerId = asLong(lead.get("viewer_id"));
                secUid = str(lead.get("sec_uid"));
                matchType = roomKey.equals(str(lead.get("room_key"))) ? "nickname" : "nickname_any_room";
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("leadId", leadId);
        data.put("viewerId", viewerId);
        data.put("leadDate", leadDate);
        data.put("roomKey", roomKey);
        data.put("douyinUserId", report.getUserId());
        data.put("secUid", secUid);
        data.put("nickname", trim(comment.getNickname(), 100));
        data.put("content", trim(comment.getContent(), 1000));
        data.put("dedupeKey", dedupeKey(comment));
        data.put("matchType", matchType);
        data.put("source", trim(comment.getSource(), 64));
        data.put("pageUrl", trim(hasText(comment.getPageUrl()) ? comment.getPageUrl() : report.getPageUrl(), 1000));
        data.put("capturedAt", capturedAt);
        int inserted = dyViewerLeadMapper.insertComment(data);
        if (inserted > 0 && leadId != null)
        {
            dyViewerLeadMapper.updateLeadCommentStats(leadId, trim(comment.getContent(), 1000), capturedAt);
            if (viewerId != null)
            {
                dyViewerLeadMapper.updateViewerCommentStats(viewerId);
            }
        }
        return inserted;
    }

    private void upsertLiveRoom(String roomKey, String roomName, String source)
    {
        if (!hasText(roomKey))
        {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("roomKey", trim(roomKey, 64));
        data.put("roomName", trim(roomName, 100));
        data.put("source", trim(source, 64));
        dyViewerLeadMapper.upsertLiveRoom(data);
    }

    private String captureRoomKey(DyCaptureReport report, DyViewerPayload audience)
    {
        if (report != null && hasText(report.getRoomKey()))
        {
            return trim(report.getRoomKey(), 64);
        }
        return audience == null ? "" : trim(audience.getRoomId(), 64);
    }

    private void applyOwnerRule(Map<String, Object> data, Map<String, Object> current, String nextOwnerName)
    {
        String currentStatus = str(current.get("status"));
        String currentOwner = str(current.get("owner_name"));
        if ((isSoldStatus(currentStatus) || hasText(str(current.get("order_no")))) && hasText(nextOwnerName) && !nextOwnerName.equals(currentOwner))
        {
            throw new ServiceException("Ordered leads cannot assign owner");
        }
        if (hasText(currentOwner) && !nextOwnerName.equals(currentOwner))
        {
            throw new ServiceException("Lead already has owner");
        }
        data.put("ownerNameTouched", true);
        data.put("ownerName", trim(nextOwnerName, 50));
        if (!hasText(nextOwnerName))
        {
            data.put("ownerAssignedTime", null);
        }
        else if (nextOwnerName.equals(currentOwner))
        {
            data.put("ownerAssignedTime", current.get("owner_assigned_time"));
        }
        else
        {
            data.put("ownerAssignedTime", LocalDateTime.now().format(DATE_TIME_FMT));
        }
    }

    private void refreshLeadRules()
    {
        dyViewerLeadMapper.releaseExpiredOwners();
        dyViewerLeadMapper.syncLeadStatusRules();
        dyViewerLeadMapper.syncViewerLeadRules();
    }

    private String resolveLeadStatus(Map<String, Object> current, String ownerName, String orderNo, String requestedStatus)
    {
        if (hasText(orderNo))
        {
            return "pre_ordered".equals(requestedStatus) ? "pre_ordered" : "ordered";
        }
        String leadDateText = hasText(str(current.get("last_seen_date"))) ? str(current.get("last_seen_date")) : str(current.get("lead_date"));
        if (isOlderThanOneMonth(leadDateText))
        {
            return "invalid";
        }
        if (hasText(ownerName))
        {
            return "following";
        }
        return "new";
    }

    private boolean isSoldStatus(String status)
    {
        return "ordered".equals(status) || "pre_ordered".equals(status);
    }

    private boolean isOlderThanOneMonth(String leadDateText)
    {
        if (!hasText(leadDateText) || leadDateText.length() < 10)
        {
            return false;
        }
        LocalDate leadDate = LocalDate.parse(leadDateText.substring(0, 10), DATE_FMT);
        return leadDate.isBefore(LocalDate.now().minusMonths(1));
    }

    private int itemCount(String payloadType, DyCaptureReport report)
    {
        if ("comments".equals(payloadType))
        {
            return report.getComments() == null ? 0 : report.getComments().size();
        }
        if ("audiences".equals(payloadType) || "online_audiences".equals(payloadType))
        {
            return report.getAudiences() == null ? 0 : report.getAudiences().size();
        }
        return 0;
    }

    private String dedupeKey(DyViewerCommentPayload comment)
    {
        if (hasText(comment.getDedupeKey()))
        {
            return trim(comment.getDedupeKey(), 255);
        }
        return trim(str(comment.getNickname()).trim().toLowerCase() + "\u0001" + str(comment.getContent()).trim().toLowerCase(), 255);
    }

    private String dateOf(Long millis)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis == null ? System.currentTimeMillis() : millis), ZONE).toLocalDate().format(DATE_FMT);
    }

    private String dateTimeOf(Long millis)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis == null ? System.currentTimeMillis() : millis), ZONE).format(DATE_TIME_FMT);
    }

    private String nextBatchNo(String roomKey, String payloadType)
    {
        return trim(roomKey, 32) + "-" + payloadType + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void copy(Map<String, Object> target, Map<String, Object> source, String key)
    {
        if (source != null && source.containsKey(key))
        {
            target.put(key, source.get(key));
        }
    }

    private String normalize(String value)
    {
        return str(value).trim().toLowerCase();
    }

    private boolean hasText(String value)
    {
        return StringUtils.hasText(value);
    }

    private String str(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private String trim(String value, int maxLength)
    {
        String text = str(value).trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String trim(Object value, int maxLength)
    {
        return trim(str(value), maxLength);
    }

    private Long asLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number number)
        {
            return number.longValue();
        }
        String text = String.valueOf(value);
        return hasText(text) ? Long.valueOf(text) : null;
    }

    private Integer asInteger(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return hasText(text) ? Integer.valueOf(text) : 0;
    }

    private String joinComments(Long leadId)
    {
        if (leadId == null)
        {
            return "";
        }
        List<Map<String, Object>> comments = dyViewerLeadMapper.selectCommentsByLeadId(leadId);
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> comment : comments)
        {
            String content = str(comment.get("content")).trim();
            if (!hasText(content))
            {
                continue;
            }
            if (text.length() > 0)
            {
                text.append('\n');
            }
            String capturedAt = str(comment.get("captured_at"));
            if (hasText(capturedAt))
            {
                text.append(capturedAt).append("  ");
            }
            text.append(content);
        }
        return text.toString();
    }

    private String profileUrl(Object secUid)
    {
        String value = str(secUid).trim();
        return hasText(value) ? "https://www.douyin.com/user/" + value : "";
    }

    private String intentLabel(String value)
    {
        return switch (value)
        {
            case "low" -> "低意向";
            case "medium" -> "中意向";
            case "high" -> "高意向";
            default -> "未知";
        };
    }

    private String statusLabel(String value)
    {
        return switch (value)
        {
            case "following" -> "跟进中";
            case "pre_ordered" -> "追单前已下单";
            case "ordered" -> "追单后已下单";
            case "invalid" -> "无效";
            default -> "新线索";
        };
    }
}
