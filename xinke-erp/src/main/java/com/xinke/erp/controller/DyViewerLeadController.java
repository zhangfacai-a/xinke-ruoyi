package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Anonymous;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.poi.ExcelUtil;
import com.xinke.erp.domain.DyCaptureReport;
import com.xinke.erp.domain.DyViewerLeadExport;
import com.xinke.erp.service.IDyViewerLeadService;

@RestController
public class DyViewerLeadController extends BaseController
{
    @Autowired
    private IDyViewerLeadService dyViewerLeadService;

    @Anonymous
    @PostMapping({ "/live/viewer/report", "/live/userRoom/report" })
    public AjaxResult report(@RequestBody DyCaptureReport report)
    {
        return success(dyViewerLeadService.report(report));
    }

    @PreAuthorize("@ss.hasPermi('live:room:list')")
    @GetMapping("/live/room/list")
    public TableDataInfo roomList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = dyViewerLeadService.listRooms(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('live:room:query')")
    @GetMapping("/live/room/{roomKey}")
    public AjaxResult getRoom(@PathVariable String roomKey)
    {
        return success(dyViewerLeadService.getRoom(roomKey));
    }

    @PreAuthorize("@ss.hasPermi('live:room:add')")
    @Log(title = "Douyin live room", businessType = BusinessType.INSERT)
    @PostMapping("/live/room")
    public AjaxResult addRoom(@RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.addRoom(form));
    }

    @PreAuthorize("@ss.hasPermi('live:room:edit')")
    @Log(title = "Douyin live room", businessType = BusinessType.UPDATE)
    @PutMapping("/live/room/{roomKey}")
    public AjaxResult editRoom(@PathVariable String roomKey, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.updateRoom(roomKey, form));
    }

    @PreAuthorize("@ss.hasPermi('live:room:remove')")
    @Log(title = "Douyin live room", businessType = BusinessType.DELETE)
    @DeleteMapping("/live/room/{roomKey}")
    public AjaxResult removeRoom(@PathVariable String roomKey)
    {
        return toAjax(dyViewerLeadService.deleteRoom(roomKey));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/lead/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = dyViewerLeadService.listLeads(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @Log(title = "Douyin viewer lead", businessType = BusinessType.EXPORT)
    @PostMapping("/live/viewer/lead/export")
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> query)
    {
        List<DyViewerLeadExport> list = dyViewerLeadService.exportLeads(query);
        ExcelUtil<DyViewerLeadExport> util = new ExcelUtil<>(DyViewerLeadExport.class);
        util.exportExcel(response, list, "直播追单明细");
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/room/suggestions")
    public AjaxResult roomSuggestions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(dyViewerLeadService.listRoomSuggestions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/owner/suggestions")
    public AjaxResult ownerSuggestions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(dyViewerLeadService.listOwnerSuggestions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:query')")
    @GetMapping("/live/viewer/lead/{leadId}")
    public AjaxResult getInfo(@PathVariable Long leadId)
    {
        return success(dyViewerLeadService.getLeadDetail(leadId));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "Douyin viewer lead", businessType = BusinessType.UPDATE)
    @PutMapping("/live/viewer/lead/{leadId}")
    public AjaxResult edit(@PathVariable Long leadId, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.updateLead(leadId, form));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:follow')")
    @Log(title = "Douyin viewer follow", businessType = BusinessType.INSERT)
    @PostMapping("/live/viewer/lead/{leadId}/follow")
    public AjaxResult addFollow(@PathVariable Long leadId, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.addFollowRecord(leadId, form, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/summary")
    public AjaxResult summary(@RequestParam Map<String, Object> query)
    {
        return success(dyViewerLeadService.summary(query));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/bi")
    public AjaxResult bi(@RequestParam Map<String, Object> query)
    {
        return success(dyViewerLeadService.biDashboard(query));
    }
}
