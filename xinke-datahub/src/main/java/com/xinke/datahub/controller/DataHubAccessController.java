package com.xinke.datahub.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.datahub.service.DataHubDatasetService;

@RestController
@RequestMapping("/datahub/access")
public class DataHubAccessController extends BaseController
{
    private final DataHubDatasetService datasetService;

    public DataHubAccessController(DataHubDatasetService datasetService)
    {
        this.datasetService = datasetService;
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:grant')")
    @GetMapping("/users")
    public AjaxResult users(@RequestParam(required = false) String keyword)
    {
        return success(datasetService.userOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:grant')")
    @GetMapping("/roles")
    public AjaxResult roles(@RequestParam(required = false) String keyword)
    {
        return success(datasetService.roleOptions(keyword));
    }
}
