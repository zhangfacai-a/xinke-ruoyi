package com.xinke.erp.controller;

import java.util.Map;
import java.util.concurrent.Semaphore;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xinke.common.annotation.Anonymous;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.domain.LiveMetricsSubmissionRequest;
import com.xinke.erp.service.LiveMetricsReportingService;
import com.xinke.erp.service.LiveMetricsService;

@RestController
public class LiveMetricsController
{
    private static final Semaphore SUBMISSION_LIMIT = new Semaphore(4);
    private final LiveMetricsService scheduleService;
    private final LiveMetricsReportingService reportingService;

    @Value("${live.metrics.plugin-key:LIVE_CUSTOMER_V2_LOCAL_KEY}")
    private String pluginKey;

    public LiveMetricsController(LiveMetricsService scheduleService,
            LiveMetricsReportingService reportingService)
    {
        this.scheduleService = scheduleService;
        this.reportingService = reportingService;
    }

    @GetMapping("/live/metrics/shifts")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult shifts(@RequestParam Map<String, Object> query)
    {
        return AjaxResult.success(scheduleService.listShifts(query));
    }

    @GetMapping("/live/metrics/options")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult options()
    {
        return AjaxResult.success(scheduleService.options(SecurityUtils.getUserId()));
    }

    @PostMapping("/live/metrics/staff-visibility")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult saveStaffVisibility(@RequestBody Map<String, Object> value)
    {
        scheduleService.saveStaffVisibility(SecurityUtils.getUserId(), value);
        return AjaxResult.success();
    }

    @PostMapping("/live/metrics/shifts")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult saveShift(@RequestBody Map<String, Object> value)
    {
        scheduleService.saveShift(value, SecurityUtils.getUsername());
        return AjaxResult.success(value);
    }

    @DeleteMapping("/live/metrics/shifts/{id}")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult deleteShift(@PathVariable Long id)
    {
        scheduleService.deleteShift(id);
        return AjaxResult.success();
    }

    @PostMapping("/live/metrics/shifts/week")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult saveWeek(@RequestBody Map<String, Object> value)
    {
        return AjaxResult.success(scheduleService.saveWeek(value, SecurityUtils.getUsername()));
    }

    @GetMapping("/live/metrics/shift-templates")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult templates(@RequestParam Long roomId)
    {
        return AjaxResult.success(scheduleService.templates(roomId));
    }

    @PostMapping("/live/metrics/shift-templates")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult saveTemplate(@RequestBody Map<String, Object> value)
    {
        scheduleService.saveTemplate(value, SecurityUtils.getUsername());
        return AjaxResult.success(value);
    }

    @DeleteMapping("/live/metrics/shift-templates/{id}")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult deleteTemplate(@PathVariable Long id)
    {
        scheduleService.deleteTemplate(id);
        return AjaxResult.success();
    }

    @Anonymous
    @PostMapping("/open-api/live-metrics/v2/submissions")
    public AjaxResult submit(@RequestHeader(value = "X-Live-Metrics-Key", required = false) String key,
            @Valid @RequestBody LiveMetricsSubmissionRequest request)
    {
        if (!pluginKey.equals(key))
        {
            return AjaxResult.error("PLUGIN_VERSION_EXPIRED");
        }
        if (!SUBMISSION_LIMIT.tryAcquire())
        {
            AjaxResult busy = AjaxResult.error("服务器正在处理其他直播数据，请稍后重试");
            busy.put("retryAfter", 15);
            return busy;
        }
        try
        {
            return AjaxResult.success(reportingService.submit(request));
        }
        finally
        {
            SUBMISSION_LIMIT.release();
        }
    }

    @GetMapping("/live/metrics/reporting")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult reporting(@RequestParam Map<String, Object> query)
    {
        return AjaxResult.success(reportingService.list(query));
    }

    @GetMapping("/live/metrics/reporting/{sessionId}/shifts")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult reportingShifts(@PathVariable Long sessionId)
    {
        return AjaxResult.success(reportingService.listSessionShifts(sessionId));
    }

    @PostMapping("/live/metrics/reporting/supplement")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult supplement(@RequestBody Map<String, Object> value)
    {
        return AjaxResult.success(reportingService.supplement(value,
                SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }

    @PostMapping("/live/metrics/reporting/rematch")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult rematch(@RequestBody Map<String, Object> value)
    {
        reportingService.rematch(value, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    @PostMapping("/live/metrics/reporting/bind-room")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult bindRoom(@RequestBody Map<String, Object> value)
    {
        reportingService.bindRoom(value, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    @PostMapping("/live/metrics/reporting/{sessionId}/recalculate")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult recalculate(@PathVariable Long sessionId)
    {
        reportingService.recalculate(sessionId, null);
        return AjaxResult.success();
    }
}
