package com.xinke.erp.livecrm.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.livecrm.mapper.LiveCrmCenterMapper;

@Service
public class LiveCrmCenterService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RANGE_DAYS = 31;
    private static final int MAX_EXPORT_ROWS = 10000;
    private final LiveCrmCenterMapper mapper;
    private final Semaphore exportPermit = new Semaphore(1, true);

    public LiveCrmCenterService(LiveCrmCenterMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> list(String day, String beginDate, String endDate, String roomName,
                                    String queue, String keyword,
                                    String stage, boolean commentedOnly, int pageNum, int pageSize) {
        DateWindow window = dateWindow(day, beginDate, endDate, false);
        String safeQueue = normalizeQueue(queue);
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        String roomKey = normalizeRoom(roomName);
        String safeKeyword = blankToNull(keyword);
        String safeStage = blankToNull(stage);
        List<Map<String, Object>> rows = mapper.listCustomers(window.beginAt(), window.endAt(), roomKey,
                safeQueue, safeKeyword, safeStage, commentedOnly, (safePage - 1) * safeSize, safeSize);
        hydrateLatestComments(rows);
        long total = rows.isEmpty() ? 0L : number(rows.get(0).get("totalCount"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("beginDate", window.beginDate().toString());
        result.put("endDate", window.endDate().toString());
        return result;
    }

    public Map<String, Object> dashboard(String day, String beginDate, String endDate, String roomName) {
        DateWindow window = dateWindow(day, beginDate, endDate, true);
        String roomKey = normalizeRoom(roomName);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> summary = mapper.dashboard(window.beginAt(), window.endAt(), roomKey);
        if (summary != null) result.putAll(summary);
        result.put("ranking", mapper.ownerRanking(window.beginAt(), window.endAt(), roomKey));
        result.put("roomStats", mapper.roomStats(window.beginAt(), window.endAt(), roomKey));
        result.put("beginDate", window.beginDate().toString());
        result.put("endDate", window.endDate().toString());
        return result;
    }

    public void export(String beginDate, String endDate, String roomName, String queue, String stage,
                       boolean commentedOnly,
                       HttpServletResponse response) throws IOException {
        if (!exportPermit.tryAcquire()) {
            throw new ServiceException("当前已有下载任务，请稍后再试");
        }
        try {
            DateWindow window = dateWindow(null, beginDate, endDate, false);
            String roomKey = normalizeRoom(roomName);
            List<Map<String, Object>> rows = mapper.exportCustomers(window.beginAt(), window.endAt(),
                    roomKey, normalizeQueue(queue), null, blankToNull(stage), commentedOnly, MAX_EXPORT_ROWS);
            String range = window.beginDate() + "至" + window.endDate();
            String filename = URLEncoder.encode("直播客户明细-" + range + ".csv", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            response.setHeader("X-Export-Limit", String.valueOf(MAX_EXPORT_ROWS));
            response.setHeader("Cache-Control", "no-store");
            try (PrintWriter writer = response.getWriter()) {
                writer.write('\ufeff');
                writer.println("日期,直播间,客户昵称,抖音号,手机号,微信号,首次互动,最近互动,最新评论,评论数,来访次数,有效观看分钟,客户阶段,负责人,场控首次跟进时间,场控");
                for (Map<String, Object> row : rows) {
                    writer.println(csv(row.get("liveDate")) + ',' + csv(row.get("roomName")) + ','
                            + csv(row.get("nickname")) + ',' + csv(row.get("douyinAccount")) + ','
                            + csv(row.get("phone")) + ',' + csv(row.get("wechat")) + ','
                            + csv(row.get("firstInteractionAt")) + ',' + csv(row.get("lastInteractionAt")) + ','
                            + csv(row.get("lastComment")) + ',' + csv(row.get("commentCount")) + ','
                            + csv(row.get("visitCount")) + ',' + csv(minutes(row.get("watchSeconds"))) + ','
                            + csv(stageLabel(row.get("stage"))) + ',' + csv(row.get("ownerName")) + ','
                            + csv(row.get("controllerFirstFollowupAt")) + ',' + csv(row.get("controllerFirstFollowupName")));
                }
            }
        } finally {
            exportPermit.release();
        }
    }

    private DateWindow dateWindow(String day, String beginValue, String endValue, boolean yesterdayDefault) {
        LocalDate today = LocalDate.now();
        LocalDate fallback = yesterdayDefault ? today.minusDays(1) : today;
        if ("yesterday".equalsIgnoreCase(day)) fallback = today.minusDays(1);
        LocalDate begin = parseDate(beginValue, fallback);
        LocalDate end = parseDate(endValue, begin);
        if (begin.isAfter(today)) begin = today;
        if (end.isAfter(today)) end = today;
        if (begin.isAfter(end)) begin = end;
        if (begin.isBefore(end.minusDays(MAX_RANGE_DAYS - 1L))) begin = end.minusDays(MAX_RANGE_DAYS - 1L);
        return new DateWindow(begin, end, begin + " 00:00:00", end.plusDays(1) + " 00:00:00");
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String normalizeQueue(String value) {
        String queue = value == null ? "INBOX" : value.trim().toUpperCase(Locale.ROOT);
        return switch (queue) {
            case "FOLLOWUP", "ALL" -> queue;
            default -> "INBOX";
        };
    }

    private String normalizeRoom(String value) {
        String room = blankToNull(value);
        return room == null ? null : room.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) text = '\'' + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private String stageLabel(Object value) {
        if (value == null) return "未沟通";
        return switch (String.valueOf(value)) {
            case "CONTACTED" -> "已沟通";
            case "CONSULTING" -> "咨询中";
            case "HIGH_INTENT" -> "高意向";
            case "ORDERED" -> "已下单";
            case "INVALID" -> "无效";
            default -> "未沟通";
        };
    }

    private long minutes(Object value) {
        return value instanceof Number number ? Math.max(0, Math.round(number.doubleValue() / 60D)) : 0;
    }

    private long number(Object value) {
        return value instanceof Number number ? Math.max(0, number.longValue()) : 0L;
    }

    private void hydrateLatestComments(List<Map<String, Object>> rows) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long id = number(row.get("lastCommentId"));
            if (id > 0) ids.add(id);
        }
        if (ids.isEmpty()) return;
        Map<Long, Object> contents = new HashMap<>();
        for (Map<String, Object> item : mapper.listCommentContents(ids)) {
            contents.put(number(item.get("id")), item.get("content"));
        }
        for (Map<String, Object> row : rows) {
            row.put("lastComment", contents.get(number(row.get("lastCommentId"))));
        }
    }

    private record DateWindow(LocalDate beginDate, LocalDate endDate, String beginAt, String endAt) {}
}
