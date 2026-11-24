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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xinke.common.annotation.Anonymous;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.service.LiveHandoverService;
import com.xinke.erp.service.LiveDesktopService;

@RestController
public class LiveMetricsController
{
    private static final Semaphore PLUGIN_LIMIT = new Semaphore(8);
    private final LiveHandoverService service;
    private final LiveDesktopService desktop;

    @Value("${live.metrics.plugin-key:LIVE_CUSTOMER_V2_LOCAL_KEY}")
    private String pluginKey;

    @Value("${live.metrics.minimum-plugin-version:4.0.0}")
    private String minimumPluginVersion;

    public LiveMetricsController(LiveHandoverService service,LiveDesktopService desktop){this.service=service;this.desktop=desktop;}

    @Anonymous
    @GetMapping("/open-api/live-metrics/v3/context")
    public AjaxResult context(@RequestHeader(value="X-Live-Metrics-Key",required=false) String key,
            @RequestHeader(value="X-Plugin-Version",required=false) String version,
            @RequestParam String platformSessionId,@RequestParam String roomName)
    {
        AjaxResult rejected=rejectPlugin(key,version);
        return rejected==null?AjaxResult.success(service.context(platformSessionId,roomName)):rejected;
    }

    @Anonymous
    @PostMapping("/open-api/live-metrics/v3/transition")
    public AjaxResult transition(@RequestHeader(value="X-Live-Metrics-Key",required=false) String key,
            @RequestHeader(value="X-Plugin-Version",required=false) String version,
            @Valid @RequestBody LiveHandoverRequest request)
    {
        AjaxResult rejected=rejectPlugin(key,version);
        if(rejected!=null)return rejected;
        if(!PLUGIN_LIMIT.tryAcquire())
        {
            AjaxResult busy=AjaxResult.error("服务器正在处理其他直播数据，请稍后重试");
            busy.put("retryAfter",5);
            return busy;
        }
        try{return AjaxResult.success(service.transition(request));}
        finally{PLUGIN_LIMIT.release();}
    }

    @GetMapping("/live/metrics/rooms")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult rooms(){return AjaxResult.success(desktop.rooms());}

    @GetMapping("/live/metrics/rooms/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult room(@PathVariable Long roomId,@RequestParam Map<String,Object> query)
    {return AjaxResult.success(desktop.room(roomId,query));}

    @PostMapping("/live/metrics/rooms/{roomId}/anchors")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult addAnchor(@PathVariable Long roomId,@RequestBody Map<String,Object> value)
    {
        desktop.add(roomId,Long.valueOf(String.valueOf(value.get("userId"))));
        return AjaxResult.success();
    }

    @DeleteMapping("/live/metrics/rooms/{roomId}/anchors/{userId}")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult removeAnchor(@PathVariable Long roomId,@PathVariable Long userId)
    {desktop.remove(roomId,userId);return AjaxResult.success();}

    @GetMapping("/live/metrics/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult session(@PathVariable Long sessionId){return AjaxResult.success(desktop.session(sessionId));}

    @PostMapping("/live/metrics/sessions/{sessionId}/segments")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult createSegment(@PathVariable Long sessionId,@RequestBody Map<String,Object> value)
    {return AjaxResult.success(service.createSegment(sessionId,value,SecurityUtils.getUserId(),displayName()));}

    @PutMapping("/live/metrics/segments/{segmentId}")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult updateSegment(@PathVariable Long segmentId,@RequestBody Map<String,Object> value)
    {return AjaxResult.success(service.updateSegment(segmentId,value,SecurityUtils.getUserId(),displayName()));}

    @PostMapping("/live/metrics/segments/{segmentId}/void")
    @PreAuthorize("@ss.hasPermi('live:metrics:manage')")
    public AjaxResult voidSegment(@PathVariable Long segmentId,@RequestBody Map<String,Object> value)
    {return AjaxResult.success(service.voidSegment(segmentId,value,SecurityUtils.getUserId(),displayName()));}

    private AjaxResult rejectPlugin(String key,String version)
    {
        if(!pluginKey.equals(key))return AjaxResult.error("PLUGIN_AUTH_FAILED");
        if(isVersionOlder(version,minimumPluginVersion))return AjaxResult.error("PLUGIN_VERSION_EXPIRED");
        return null;
    }

    private String displayName()
    {
        String nickname=SecurityUtils.getLoginUser().getUser().getNickName();
        return nickname==null||nickname.isBlank()?SecurityUtils.getUsername():nickname;
    }

    private static boolean isVersionOlder(String current,String minimum)
    {
        if(minimum==null||minimum.isBlank())return false;
        if(current==null||current.isBlank())return true;
        String[] left=current.trim().split("[.-]"),right=minimum.trim().split("[.-]");
        int length=Math.max(left.length,right.length);
        try
        {
            for(int i=0;i<length;i++)
            {
                int a=i<left.length?Integer.parseInt(left[i]):0,b=i<right.length?Integer.parseInt(right[i]):0;
                if(a!=b)return a<b;
            }
            return false;
        }
        catch(NumberFormatException exception){return true;}
    }
}
