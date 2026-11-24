package com.xinke.erp.controller;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.service.LiveDesktopService;
import com.xinke.erp.service.LiveHandoverService;
import com.xinke.erp.mapper.LiveMetricsMapper;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.common.exception.ServiceException;

@RestController
@RequestMapping("/live/desktop")
public class LiveDesktopController {
    private final LiveDesktopService service;
    private final LiveMetricsMapper mapper;
    public LiveDesktopController(LiveDesktopService service,LiveMetricsMapper mapper){this.service=service;this.mapper=mapper;}
    @GetMapping("/context")
    public AjaxResult context(@RequestParam String platformSessionId,@RequestParam String roomName){return AjaxResult.success(service.context(platformSessionId,roomName));}
    @PostMapping("/preview")
    public AjaxResult preview(@Valid @RequestBody LiveHandoverRequest request,@RequestHeader("X-App-Version") String version){return AjaxResult.success(service.preview(request,version));}
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String,String> value){return AjaxResult.success(service.commit(value.get("ticket")));}
    @PutMapping("/rooms/{roomId}/anchors")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult assign(@PathVariable Long roomId,@RequestBody Assignment value){service.assign(roomId,new HashSet<>(value.expected()),new HashSet<>(value.selected()));return AjaxResult.success();}
    public record Assignment(List<Long> expected,List<Long> selected){}
    @PostMapping("/rooms/{roomId}/binding")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult bind(@PathVariable Long roomId,@RequestBody Map<String,String> input){
        String name=input.get("roomName");if(name==null||name.isBlank()||name.length()>160||mapper.getHandoverRoom(roomId)==null)throw new ServiceException("直播间名称无效");
        String key=LiveHandoverService.normalize(name);Map<String,Object> existing=mapper.findRoomByAliasKey(key);
        if(existing!=null){if(!String.valueOf(roomId).equals(String.valueOf(existing.get("roomId"))))throw new ServiceException("此名称已绑定其他直播间");return AjaxResult.success();}
        mapper.bindAlias(roomId,name,key,SecurityUtils.getUsername());return AjaxResult.success();
    }
}
