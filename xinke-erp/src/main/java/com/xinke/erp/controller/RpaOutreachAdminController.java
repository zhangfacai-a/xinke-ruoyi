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
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.domain.RpaRoomBindingRequest;
import com.xinke.erp.domain.RpaShopConfigRequest;
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

    @PreAuthorize("@ss.hasPermi('live:rpa:config')")
    @GetMapping("/room/unmapped")
    public AjaxResult unmappedRooms()
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.listUnmappedRooms());
    }

    @PreAuthorize("@ss.hasPermi('live:rpa:config')")
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

    @PreAuthorize("@ss.hasPermi('live:rpa:config')")
    @Log(title = "RPA直播间绑定", businessType = BusinessType.UPDATE)
    @PutMapping("/shop/{shopConfigId}/rooms")
    public AjaxResult bindRooms(@PathVariable Long shopConfigId, @Valid @RequestBody RpaRoomBindingRequest request)
    {
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.bindRooms(shopConfigId, request));
    }
}
