package com.ruoyi.erp.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        dyViewerLeadMapper.releaseExpiredOwners();
        return dyViewerLeadMapper.selectLeadList(query);
    }

    @Override
    public Map<String, Object> getLeadDetail(Long leadId)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("lead", dyViewerLeadMapper.selectLeadById(leadId));
        data.put("comments", dyViewerLeadMapper.selectCommentsByLeadId(leadId));
        data.put("followRecords", dyViewerLeadMapper.selectFollowRecordsByLeadId(leadId));
        return data;
    }

    @Override
    public int updateLead(Long leadId, Map<String, Object> form)
    {
        dyViewerLeadMapper.releaseExpiredOwners();
        Map<String, Object> current = dyViewerLeadMapper.selectLeadById(leadId);
        if (current == null)
        {
            throw new ServiceException("Lead does not exist");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("leadId", leadId);
        copy(data, form, "status");
        copy(data, form, "intent");
        copy(data, form, "ownerId");
        copy(data, form, "orderNo");
        copy(data, form, "remark");

        String nextStatus = hasText(str(form == null ? null : form.get("status"))) ? str(form.get("status")) : str(current.get("status"));
        String nextOrderNo = form != null && form.containsKey("orderNo") ? str(form.get("orderNo")) : str(current.get("order_no"));
        if ("ordered".equals(nextStatus) && !hasText(nextOrderNo))
        {
            throw new ServiceException("Ordered leads must have orderNo");
        }

        if (form != null && form.containsKey("ownerName"))
        {
            applyOwnerRule(data, current, str(form.get("ownerName")), nextStatus);
        }
        return dyViewerLeadMapper.updateLead(data);
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
        data.put("operatorName", operatorName);
        int rows = dyViewerLeadMapper.insertFollowRecord(data);

        Map<String, Object> leadUpdate = new HashMap<>();
        leadUpdate.put("leadId", leadId);
        leadUpdate.put("status", hasText(str(form.get("status"))) ? str(form.get("status")) : "following");
        dyViewerLeadMapper.updateLead(leadUpdate);
        return rows;
    }

    @Override
    public Map<String, Object> summary(Map<String, Object> query)
    {
        dyViewerLeadMapper.releaseExpiredOwners();
        return dyViewerLeadMapper.selectSummary(query);
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
            if (lead != null)
            {
                leadId = asLong(lead.get("lead_id"));
                viewerId = asLong(lead.get("viewer_id"));
                secUid = str(lead.get("sec_uid"));
                matchType = "nickname";
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
        if (audience != null && hasText(audience.getRoomId()))
        {
            return trim(audience.getRoomId(), 64);
        }
        return trim(report.getRoomKey(), 64);
    }

    private void applyOwnerRule(Map<String, Object> data, Map<String, Object> current, String nextOwnerName, String nextStatus)
    {
        String currentStatus = str(current.get("status"));
        String currentOwner = str(current.get("owner_name"));
        if (("ordered".equals(currentStatus) || "ordered".equals(nextStatus)) && hasText(nextOwnerName) && !nextOwnerName.equals(currentOwner))
        {
            throw new ServiceException("Ordered leads cannot assign owner");
        }
        if (hasText(currentOwner) && hasText(nextOwnerName) && !nextOwnerName.equals(currentOwner) && !ownerExpired(current))
        {
            throw new ServiceException("Owner is locked for 24 hours");
        }
        data.put("ownerNameTouched", true);
        data.put("ownerName", trim(nextOwnerName, 50));
        data.put("ownerAssignedTime", hasText(nextOwnerName) ? LocalDateTime.now().format(DATE_TIME_FMT) : null);
    }

    private boolean ownerExpired(Map<String, Object> current)
    {
        Object value = current.get("owner_assigned_time");
        if (value == null)
        {
            return false;
        }
        String text = String.valueOf(value);
        if (!hasText(text) || text.length() < 19)
        {
            return false;
        }
        LocalDateTime assigned = LocalDateTime.parse(text.substring(0, 19), DATE_TIME_FMT);
        return assigned.plusHours(24).isBefore(LocalDateTime.now());
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
}
