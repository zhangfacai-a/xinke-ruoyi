package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.annotation.Log;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.poi.ExcelUtil;
import com.xinke.common.constant.HttpStatus;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.audience.AudienceRankBatch;
import com.xinke.erp.domain.audience.AudienceRankBatchQuery;
import com.xinke.erp.domain.audience.AudienceRankQuery;
import com.xinke.erp.domain.audience.AudienceRankSnapshot;
import com.xinke.erp.domain.audience.AudienceFollowup;
import com.xinke.erp.domain.audience.AudienceFollowupLog;
import com.xinke.erp.domain.audience.AudienceFollowupQuery;
import com.xinke.erp.domain.audience.AudienceFollowupExport;
import com.xinke.erp.domain.audience.AudienceVisitRecord;
import com.xinke.erp.domain.audience.AudienceAssignmentRule;
import com.xinke.erp.domain.audience.AudienceCustomerOrder;
import com.xinke.erp.service.IAudienceRankService;

@RestController
@RequestMapping("/live/audience-rank")
public class AudienceRankController extends BaseController
{
    private final IAudienceRankService audienceRankService;

    public AudienceRankController(IAudienceRankService audienceRankService)
    {
        this.audienceRankService = audienceRankService;
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:list')")
    @GetMapping("/list")
    public TableDataInfo list(AudienceRankQuery query)
    {
        startPage();
        List<AudienceRankSnapshot> rows = audienceRankService.selectSnapshotList(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:list')")
    @GetMapping("/batch/list")
    public TableDataInfo batchList(AudienceRankBatchQuery query)
    {
        startPage();
        List<AudienceRankBatch> rows = audienceRankService.selectBatchList(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:list,live:audienceRank:query')")
    @GetMapping("/batch/{batchId}")
    public AjaxResult batchDetail(@PathVariable Long batchId)
    {
        return success(audienceRankService.selectBatchDetail(batchId));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:list')")
    @GetMapping("/summary")
    public AjaxResult summary(AudienceRankQuery query)
    {
        return success(audienceRankService.selectSummary(query));
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query')")
    @GetMapping("/followup/list")
    public TableDataInfo followupList(AudienceFollowupQuery query)
    {
        startPage();
        List<AudienceFollowup> rows = audienceRankService.selectFollowupList(query, getUserId());
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query')")
    @GetMapping("/followup/summary")
    public AjaxResult followupSummary(AudienceFollowupQuery query)
    {
        return success(audienceRankService.selectFollowupSummary(query, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:assign')")
    @GetMapping("/followup/dashboard")
    public AjaxResult followupDashboard(AudienceFollowupQuery query)
    {
        return success(audienceRankService.selectTeamDashboard(query));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:export')")
    @PostMapping("/followup/export")
    @Log(title = "导出观众跟单", businessType = BusinessType.EXPORT)
    public void followupExport(AudienceFollowupQuery query, HttpServletResponse response)
    {
        List<AudienceFollowupExport> rows = audienceRankService.selectFollowupList(query, getUserId())
                .stream().map(AudienceFollowupExport::from).toList();
        new ExcelUtil<>(AudienceFollowupExport.class).exportExcel(response, rows, "观众跟单");
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query')")
    @GetMapping("/followup/{followupId}")
    public AjaxResult followup(@PathVariable Long followupId)
    {
        return success(audienceRankService.selectFollowup(followupId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:history')")
    @GetMapping("/followup/{followupId}/logs")
    public AjaxResult followupLogs(@PathVariable Long followupId)
    {
        List<AudienceFollowupLog> logs = audienceRankService.selectFollowupLogs(followupId, getUserId());
        return success(logs);
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:history')")
    @GetMapping("/followup/{followupId}/visits")
    public AjaxResult followupVisits(@PathVariable Long followupId)
    {
        List<AudienceVisitRecord> visits = audienceRankService.selectFollowupVisits(followupId, getUserId());
        return success(visits);
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query')")
    @GetMapping("/followup/{followupId}/orders")
    public AjaxResult followupOrders(@PathVariable Long followupId)
    {
        return success(audienceRankService.selectCustomerOrders(followupId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:edit')")
    @PostMapping("/followup/{followupId}/reactivate")
    @Log(title = "重新激活观众客户", businessType = BusinessType.UPDATE)
    public AjaxResult followupReactivate(@PathVariable Long followupId)
    {
        return success(audienceRankService.reactivateFollowup(followupId, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:edit')")
    @PostMapping("/followup/{followupId}/orders")
    @Log(title = "保存观众客户订单", businessType = BusinessType.UPDATE)
    public AjaxResult followupOrderSave(@PathVariable Long followupId,
                                        @RequestBody AudienceCustomerOrder order)
    {
        return success(audienceRankService.saveCustomerOrder(followupId, order, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query,live:audienceRank:followup:assign')")
    @GetMapping("/followup/rooms")
    public AjaxResult followupRooms()
    {
        return success(audienceRankService.selectFollowupRooms());
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:list,live:audienceRank:followup:query,live:audienceRank:followup:assign')")
    @GetMapping("/followup/assignees")
    public AjaxResult followupAssignees(@RequestParam(required = false) Long roomId,
                                        @RequestParam(required = false) String roleCode)
    {
        return success(audienceRankService.selectFollowupAssignees(roomId, roleCode));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:assign')")
    @GetMapping("/followup/assignment-rules")
    public AjaxResult assignmentRules()
    {
        return success(audienceRankService.selectAssignmentRules());
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:assign')")
    @PutMapping("/followup/assignment-rules/{roomId}")
    @Log(title = "保存观众客户分配规则", businessType = BusinessType.UPDATE)
    public AjaxResult assignmentRuleSave(@PathVariable Long roomId,
                                         @RequestBody AudienceAssignmentRule value)
    {
        value.setRoomId(roomId);
        return success(audienceRankService.saveAssignmentRule(value, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:assign')")
    @PostMapping("/followup/auto-assign")
    @Log(title = "智能分配观众客户", businessType = BusinessType.UPDATE)
    public AjaxResult autoAssign(@RequestBody Map<String, Object> value)
    {
        Long roomId = value == null || value.get("roomId") == null
                ? null : Long.valueOf(value.get("roomId").toString());
        return success(audienceRankService.autoAssignFollowups(roomId, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:edit')")
    @PostMapping("/followup/{followupId}")
    public AjaxResult followupSave(@PathVariable Long followupId, @RequestBody AudienceFollowup value)
    {
        value.setFollowupId(followupId);
        audienceRankService.updateFollowup(value, getUserId(), getUsername());
        return success(audienceRankService.selectFollowup(followupId, getUserId()));
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:assign,live:audienceRank:followup:edit')")
    @PostMapping("/followup/{followupId}/claim")
    public AjaxResult followupClaim(@PathVariable Long followupId)
    {
        audienceRankService.claimFollowup(followupId, getUserId(), getUsername());
        return success(audienceRankService.selectFollowup(followupId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('live:audienceRank:followup:edit')")
    @PostMapping("/followup/{followupId}/status")
    public AjaxResult followupStatus(@PathVariable Long followupId,
                                     @RequestBody Map<String, Object> value)
    {
        Date nextFollowAt = null;
        Object next = value == null ? null : value.get("nextFollowAt");
        if (next instanceof Date date) nextFollowAt = date;
        else if (next != null && !next.toString().isBlank())
        {
            try { nextFollowAt = Date.from(java.time.Instant.parse(next.toString())); }
            catch (DateTimeParseException ex)
            {
                try { nextFollowAt = Date.from(LocalDateTime.parse(next.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault()).toInstant()); }
                catch (DateTimeParseException ignored) { throw new ServiceException("跟进时间格式不正确", HttpStatus.BAD_REQUEST); }
            }
        }
        audienceRankService.updateFollowupStatus(followupId,
                value == null ? null : Objects.toString(value.get("status"), null),
                value == null ? null : Objects.toString(value.get("content"), null),
                nextFollowAt, getUserId(), getUsername());
        return success(audienceRankService.selectFollowup(followupId, getUserId()));
    }

    @PreAuthorize("@ss.hasAnyPermi('live:audienceRank:followup:edit,live:audienceRank:followup:assign')")
    @PutMapping("/followup/batch")
    public AjaxResult followupBatch(@RequestBody Map<String, Object> value)
    {
        List<Long> ids = new java.util.ArrayList<>();
        Object rawIds = value == null ? null : value.get("followupIds");
        if (rawIds instanceof List<?> list) for (Object id : list) ids.add(Long.valueOf(id.toString()));
        Map<String, Object> changes = null;
        if (value != null && value.get("changes") instanceof Map<?, ?> rawChanges)
        {
            Map<String, Object> normalizedChanges = new java.util.LinkedHashMap<>();
            rawChanges.forEach((key, item) -> normalizedChanges.put(String.valueOf(key), item));
            changes = normalizedChanges;
        }
        audienceRankService.batchUpdateFollowups(ids, changes, getUserId(), getUsername());
        return success();
    }
}
