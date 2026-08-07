package com.xinke.erp.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Anonymous;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.domain.RpaBatchRequest;
import com.xinke.erp.domain.RpaTaskClaimRequest;
import com.xinke.erp.domain.RpaTaskResultRequest;
import com.xinke.erp.service.IRpaOutreachService;

@RestController
@RequestMapping("/open-api/rpa/outreach")
public class RpaOutreachController extends BaseController
{
    private static final int RPA_NOT_CONFIGURED_CODE = 46010;
    private static final int RPA_AUTH_FAILED_CODE = 46011;

    @Autowired
    private IRpaOutreachService rpaOutreachService;

    @Value("${live.rpa.api-key:}")
    private String rpaApiKey;

    @GetMapping("/health")
    @Anonymous
    public AjaxResult health(@RequestHeader(value = "X-RPA-Key", required = false) String requestKey)
    {
        AjaxResult denied = authenticate(requestKey);
        if (denied != null)
        {
            return denied;
        }
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.health());
    }

    @PostMapping("/task/claim")
    @Anonymous
    public AjaxResult claim(@RequestHeader(value = "X-RPA-Key", required = false) String requestKey,
            @Valid @RequestBody RpaTaskClaimRequest request)
    {
        AjaxResult denied = authenticate(requestKey);
        if (denied != null)
        {
            return denied;
        }
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.claim(request));
    }

    @PostMapping("/batch/heartbeat")
    @Anonymous
    public AjaxResult heartbeat(@RequestHeader(value = "X-RPA-Key", required = false) String requestKey,
            @Valid @RequestBody RpaBatchRequest request)
    {
        AjaxResult denied = authenticate(requestKey);
        if (denied != null)
        {
            return denied;
        }
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.heartbeat(request));
    }

    @PostMapping("/task/result")
    @Anonymous
    public AjaxResult result(@RequestHeader(value = "X-RPA-Key", required = false) String requestKey,
            @Valid @RequestBody RpaTaskResultRequest request)
    {
        AjaxResult denied = authenticate(requestKey);
        if (denied != null)
        {
            return denied;
        }
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.submitResult(request));
    }

    @PostMapping("/batch/release")
    @Anonymous
    public AjaxResult release(@RequestHeader(value = "X-RPA-Key", required = false) String requestKey,
            @Valid @RequestBody RpaBatchRequest request)
    {
        AjaxResult denied = authenticate(requestKey);
        if (denied != null)
        {
            return denied;
        }
        rpaOutreachService.ensureSchema();
        return success(rpaOutreachService.release(request));
    }

    private AjaxResult authenticate(String requestKey)
    {
        String expected = rpaApiKey == null ? "" : rpaApiKey.trim();
        if (expected.isEmpty())
        {
            return AjaxResult.error(RPA_NOT_CONFIGURED_CODE, "RPA接口密钥尚未配置");
        }
        String actual = requestKey == null ? "" : requestKey.trim();
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)))
        {
            return AjaxResult.error(RPA_AUTH_FAILED_CODE, "RPA接口认证失败");
        }
        return null;
    }
}
