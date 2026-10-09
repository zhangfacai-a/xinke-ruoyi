package com.xinke.erp.livecrm.controller;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.livecrm.service.LiveCrmCenterService;

@RestController
@RequestMapping("/live/v3/customer-center")
@PreAuthorize("@ss.hasAnyPermi('live:customer:list,live:customerCenter:list,live:audienceV2:list')")
public class LiveCrmCenterController extends BaseController {
    private final LiveCrmCenterService service;

    public LiveCrmCenterController(LiveCrmCenterService service) {
        this.service = service;
    }

    @GetMapping("/customers")
    public AjaxResult customers(@RequestParam(required = false) String day,
                                @RequestParam(required = false) String beginDate,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String roomName,
                                @RequestParam(defaultValue = "INBOX") String queue,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String stage,
                                @RequestParam(defaultValue = "false") boolean commentedOnly,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "20") int pageSize) {
        return success(service.list(day, beginDate, endDate, roomName, queue, keyword, stage,
                commentedOnly, pageNum, pageSize));
    }

    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam(required = false) String day,
                                @RequestParam(required = false) String beginDate,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String roomName) {
        return success(service.dashboard(day, beginDate, endDate, roomName));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String beginDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String roomName,
                       @RequestParam(defaultValue = "ALL") String queue,
                       @RequestParam(required = false) String stage,
                       @RequestParam(defaultValue = "false") boolean commentedOnly,
                       HttpServletResponse response) throws IOException {
        service.export(beginDate, endDate, roomName, queue, stage, commentedOnly, response);
    }
}
