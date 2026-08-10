package com.xinke.erp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.Map;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.domain.RpaRoomBindingRequest;
import com.xinke.erp.domain.RpaShopConfigRequest;
import com.xinke.erp.domain.RpaTrackingConfigRequest;
import com.xinke.erp.domain.RpaViewerTrackingRequest;
import com.xinke.erp.domain.RpaViewerIdsRequest;
import com.xinke.erp.domain.RpaBlacklistRequest;
import com.xinke.erp.service.IRpaOutreachService;

@RestController
@RequestMapping("/live/rpa")
public class RpaOutreachAdminController extends BaseController
{
    @Autowired
    private IRpaOutreachService rpaOutreachService;

    @PreAuthorize("@ss.hasPermi('live:rpa:config')")
    @GetMapping("/shop/list")
    public AjaxResult shops()
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.listShopConfigs());
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/room/unmapped")
    public AjaxResult unmappedRooms()
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.listUnmappedRooms());
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA店铺配置", businessType = BusinessType.INSERT)
    @PostMapping("/shop")
    public AjaxResult addShop(@Valid @RequestBody RpaShopConfigRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.saveShopConfig(null, request));
    }

    @PreAuthorize("@ss.hasPermi('live:rpa:config')")
    @Log(title = "RPA店铺配置", businessType = BusinessType.UPDATE)
    @PutMapping("/shop/{shopConfigId}")
    public AjaxResult updateShop(@PathVariable Long shopConfigId, @Valid @RequestBody RpaShopConfigRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.saveShopConfig(shopConfigId, request));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA直播间绑定", businessType = BusinessType.UPDATE)
    @PutMapping("/shop/{shopConfigId}/rooms")
    public AjaxResult bindRooms(@PathVariable Long shopConfigId, @Valid @RequestBody RpaRoomBindingRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.bindRooms(shopConfigId, request));
    }

    @PreAuthorize("@ss.hasPermi('live:room:edit')")
    @Log(title = "直播间店铺映射", businessType = BusinessType.UPDATE)
    @PutMapping("/room/{roomKey}/shop/{shopConfigId}")
    public AjaxResult mapRoom(@PathVariable String roomKey, @PathVariable Long shopConfigId)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.mapRoomToShop(roomKey, shopConfigId));
    }

    @PreAuthorize("@ss.hasPermi('live:room:edit')")
    @Log(title = "解除直播间店铺映射", businessType = BusinessType.UPDATE)
    @DeleteMapping("/room/{roomKey}/shop")
    public AjaxResult unmapRoom(@PathVariable String roomKey)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.unmapRoom(roomKey));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/tracking/config")
    public AjaxResult trackingConfig()
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.getTrackingConfig());
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA自动追踪规则", businessType = BusinessType.UPDATE)
    @PutMapping("/tracking/config")
    public AjaxResult updateTrackingConfig(@Valid @RequestBody RpaTrackingConfigRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.updateTrackingConfig(request));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA用户追踪规则", businessType = BusinessType.UPDATE)
    @PutMapping("/tracking/viewers")
    public AjaxResult updateViewerTracking(@Valid @RequestBody RpaViewerTrackingRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.updateViewerTracking(request));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/workbench/list")
    public TableDataInfo workbench(@RequestParam Map<String, Object> query)
    {
        rpaOutreachService.ensureSchema();
        startPage();
        return getDataTable(rpaOutreachService.listWorkbench(query));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/workbench/stats")
    public AjaxResult workbenchStats(@RequestParam Map<String, Object> query)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.workbenchStats(query));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/workbench/shops")
    public AjaxResult workbenchShops()
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.listShopConfigs());
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA用户加入待领取", businessType = BusinessType.UPDATE)
    @PostMapping("/workbench/enqueue")
    public AjaxResult enqueue(@Valid @RequestBody RpaViewerIdsRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.enqueueViewers(request.getViewerIds()));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA用户黑名单", businessType = BusinessType.UPDATE)
    @PostMapping("/workbench/blacklist")
    public AjaxResult blacklist(@Valid @RequestBody RpaBlacklistRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.blacklistViewers(request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "RPA用户移出黑名单", businessType = BusinessType.UPDATE)
    @DeleteMapping("/workbench/blacklist/{ids}")
    public AjaxResult restoreBlacklist(@PathVariable Long[] ids)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.restoreBlacklist(java.util.Arrays.asList(ids)));
    }
}
