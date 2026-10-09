package com.xinke.erp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinke.erp.event.LiveMetricsSubmittedEvent;
import com.xinke.erp.mapper.LiveMetricsMapper;

@Service
public class LiveMetricsReminderService
{
    private static final Logger log = LoggerFactory.getLogger(LiveMetricsReminderService.class);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private final LiveMetricsMapper mapper;
    private final TaskExecutor taskExecutor;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpiresAt;

    @Value("${live.dingtalk.app-key:}")
    private String appKey;

    @Value("${live.dingtalk.app-secret:}")
    private String appSecret;

    @Value("${live.dingtalk.robot-code:}")
    private String robotCode;

    public LiveMetricsReminderService(LiveMetricsMapper mapper,
            @Qualifier("threadPoolTaskExecutor") TaskExecutor taskExecutor)
    {
        this.mapper = mapper;
        this.taskExecutor = taskExecutor;
    }

    public Map<String, Object> sendPending()
    {
        List<Map<String, Object>> rows = mapper.findReminderCandidates();
        int sent = 0;
        for (Map<String, Object> row : rows)
        {
            try
            {
                sendText(row, pendingContent(row));
                writeLog(row, "FIRST", "SENT", null);
                sent++;
            }
            catch (Exception e)
            {
                writeLog(row, "FIRST", "FAILED", shortText(e.getMessage()));
                log.warn("直播数据待上报提醒发送失败，shiftId={}", row.get("shiftId"), e);
            }
        }
        return Map.of("checked", rows.size(), "sent", sent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMetricsSubmitted(LiveMetricsSubmittedEvent event)
    {
        taskExecutor.execute(() -> sendSubmitted(event));
    }

    private void sendSubmitted(LiveMetricsSubmittedEvent event)
    {
        Map<String, Object> shift = event.getShift();
        String dingUserId = text(shift.get("dingUserId"));
        if (dingUserId.isBlank())
        {
            log.warn("经营数据已保存，但主播没有绑定钉钉账号，shiftId={}, anchor={}",
                    shift.get("shiftId"), shift.get("anchorName"));
            return;
        }

        try
        {
            sendText(shift, submittedContent(shift, event.getSubmission()));
            writeLog(shift, "SUBMITTED", "SENT", null);
        }
        catch (Exception e)
        {
            // Data is already committed; DingTalk failure must not make the plugin report upload failure.
            writeLog(shift, "SUBMITTED", "FAILED", shortText(e.getMessage()));
            log.error("经营数据上传成功通知发送失败，shiftId={}", shift.get("shiftId"), e);
        }
    }

    private void sendText(Map<String, Object> row, String content) throws Exception
    {
        String dingUserId = text(row.get("dingUserId"));
        if (dingUserId.isBlank())
        {
            throw new IllegalStateException("主播没有绑定钉钉账号");
        }
        JSONObject response = post("https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend",
                Map.of(
                        "robotCode", robotCode,
                        "userIds", List.of(dingUserId),
                        "msgKey", "sampleText",
                        "msgParam", JSON.toJSONString(Map.of("content", content))),
                accessToken());
        ensureSuccess(response, "发送钉钉消息失败");
    }

    private String accessToken() throws Exception
    {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < accessTokenExpiresAt)
        {
            return cachedAccessToken;
        }
        synchronized (this)
        {
            now = System.currentTimeMillis();
            if (cachedAccessToken != null && now < accessTokenExpiresAt)
            {
                return cachedAccessToken;
            }
            return refreshAccessToken(now);
        }
    }

    private String refreshAccessToken(long now) throws Exception
    {
        if (appKey.isBlank() || appSecret.isBlank() || robotCode.isBlank())
        {
            throw new IllegalStateException("钉钉应用配置不完整");
        }
        JSONObject response = post("https://api.dingtalk.com/v1.0/oauth2/accessToken",
                Map.of("appKey", appKey, "appSecret", appSecret), null);
        ensureSuccess(response, "获取钉钉访问令牌失败");
        String token = response.getString("accessToken");
        if (token == null || token.isBlank())
        {
            throw new IllegalStateException("钉钉没有返回访问令牌：" + response);
        }
        long expiresIn = response.getLongValue("expireIn");
        if (expiresIn <= 0)
        {
            expiresIn = 7200;
        }
        cachedAccessToken = token;
        accessTokenExpiresAt = now + Math.max(60, expiresIn - 300) * 1000L;
        return cachedAccessToken;
    }

    private String pendingContent(Map<String, Object> row)
    {
        return "直播数据待上报\n"
                + "主播：" + row.get("anchorName") + "\n"
                + "直播间：" + row.get("roomName") + "\n"
                + "班次：" + displayTime(row.get("startTime")) + " 至 " + displayTime(row.get("endTime")) + "\n"
                + "请打开巨量百应对应直播场次，使用插件读取并确认上报。";
    }

    private String submittedContent(Map<String, Object> shift, Map<String, Object> submission)
    {
        boolean correction = number(submission.get("revisionCount")) > 1;
        boolean missingBefore = "SUSPECT_MISSING".equals(text(submission.get("matchStatus")));
        return (correction ? "直播数据已更新\n" : "直播数据匹配成功\n")
                + "主播：" + shift.get("anchorName") + "\n"
                + "直播间：" + shift.get("roomName") + "\n"
                + "班次：" + displayTime(shift.get("startTime")) + " 至 " + displayTime(shift.get("endTime")) + "\n\n"
                + "本班次经营数据\n"
                + "曝光人数：" + number(submission.get("exposureDelta")) + "\n"
                + "观看人数：" + number(submission.get("watchDelta")) + "\n"
                + "互动人数：" + number(submission.get("interactionDelta")) + "\n"
                + "新增粉丝：" + number(submission.get("newFanDelta")) + "\n"
                + "成交人数：" + number(submission.get("dealDelta"))
                + (missingBefore ? "\n\n注意：前序班次可能漏报，当前差额将在补报后自动重算。" : "");
    }

    private JSONObject post(String url, Object body, String token) throws Exception
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");
        if (token != null)
        {
            builder.header("x-acs-dingtalk-access-token", token);
        }
        HttpResponse<String> response = http.send(
                builder.POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300)
        {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + response.body());
        }
        return JSON.parseObject(response.body());
    }

    private void ensureSuccess(JSONObject response, String action)
    {
        String code = response.getString("code");
        if (code != null && !code.isBlank() && !"0".equals(code))
        {
            throw new IllegalStateException(action + "：" + response);
        }
    }

    private void writeLog(Map<String, Object> row, String reminderType, String status, String error)
    {
        try
        {
            Map<String, Object> value = new HashMap<>(row);
            value.put("reminderType", reminderType);
            value.put("sendStatus", status);
            value.put("errorMessage", error);
            mapper.insertReminderLog(value);
        }
        catch (Exception e)
        {
            log.error("钉钉通知日志保存失败，shiftId={}, type={}", row.get("shiftId"), reminderType, e);
        }
    }

    private String displayTime(Object value)
    {
        if (value instanceof LocalDateTime dateTime)
        {
            return dateTime.format(DISPLAY_TIME);
        }
        String source = text(value).replace('T', ' ');
        if (source.length() >= 16)
        {
            source = source.substring(0, 16);
            try
            {
                return LocalDateTime.parse(source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .format(DISPLAY_TIME);
            }
            catch (Exception ignored)
            {
                // Keep the database value when a driver returns an unexpected format.
            }
        }
        return source;
    }

    private long number(Object value)
    {
        try
        {
            return Long.parseLong(Objects.toString(value, "0"));
        }
        catch (Exception ignored)
        {
            return 0L;
        }
    }

    private String text(Object value)
    {
        return Objects.toString(value, "").trim();
    }

    private String shortText(String value)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
