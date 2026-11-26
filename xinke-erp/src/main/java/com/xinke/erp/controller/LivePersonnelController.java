package com.xinke.erp.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.service.LivePersonnelService;
import com.xinke.erp.service.LiveAutoAccessService;
import com.xinke.erp.service.PersonnelFreightService;
import com.xinke.common.annotation.Log;
import com.xinke.common.enums.BusinessType;

@RestController
@RequestMapping("/live/personnel")
public class LivePersonnelController {
    private final LivePersonnelService personnel;
    private final LiveAutoAccessService access;
    private final PersonnelFreightService freight;
    public LivePersonnelController(LivePersonnelService personnel,LiveAutoAccessService access,PersonnelFreightService freight) { this.personnel=personnel; this.access=access;this.freight=freight; }

    @GetMapping("/{id}/freight")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public AjaxResult freight(@PathVariable Long id) { return AjaxResult.success(freight.detail(id)); }

    @PutMapping("/{id}/freight")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title="人员运费权限配置",businessType=BusinessType.GRANT)
    public AjaxResult saveFreight(@PathVariable Long id,@RequestBody Map<String,Object> body) { freight.save(id,body);return AjaxResult.success(); }

    @PostMapping("/reconcile")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title="核对业务自动权限",businessType=BusinessType.UPDATE)
    public AjaxResult reconcile() { access.reconcileAll(); return AjaxResult.success(); }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult summaries(@RequestParam List<Long> ids) { return AjaxResult.success(personnel.summaries(ids)); }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public AjaxResult detail(@PathVariable Long id) { return AjaxResult.success(personnel.detail(id)); }
}
