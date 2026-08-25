package com.xinke.erp.controller;

import java.util.Date;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Anonymous;
import com.xinke.common.annotation.RateLimiter;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.LimitType;
import com.xinke.common.utils.ip.IpUtils;
import com.xinke.erp.domain.audience.AudienceRankImportRequest;
import com.xinke.erp.domain.audience.AudienceRankImportResult;
import com.xinke.erp.domain.audience.AudienceRankPingResult;
import com.xinke.erp.service.AudienceRankUploadAuthenticator;
import com.xinke.erp.service.IAudienceRankService;

@Anonymous
@RestController
@RequestMapping("/open-api/douyin/audience-rank")
public class AudienceRankOpenApiController extends BaseController
{
    public static final String UPLOAD_KEY_HEADER = "X-Audience-Upload-Key";

    private final AudienceRankUploadAuthenticator authenticator;
    private final IAudienceRankService audienceRankService;

    public AudienceRankOpenApiController(AudienceRankUploadAuthenticator authenticator,
            IAudienceRankService audienceRankService)
    {
        this.authenticator = authenticator;
        this.audienceRankService = audienceRankService;
    }

    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/ping")
    public AjaxResult ping(@RequestHeader(value = UPLOAD_KEY_HEADER, required = false) String uploadKey)
    {
        authenticator.authenticate(uploadKey);
        return success(new AudienceRankPingResult(true, 500, new Date()));
    }

    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/import")
    public AjaxResult importRanks(@RequestHeader(value = UPLOAD_KEY_HEADER, required = false) String uploadKey,
            @Valid @RequestBody AudienceRankImportRequest request, HttpServletRequest servletRequest)
    {
        authenticator.authenticate(uploadKey);
        AudienceRankImportResult result = audienceRankService.importRanks(request, IpUtils.getIpAddr(servletRequest));
        return AjaxResult.success(result.isDuplicate() ? "该榜单已经上传，无需重复操作" : "榜单上传成功", result);
    }
}
