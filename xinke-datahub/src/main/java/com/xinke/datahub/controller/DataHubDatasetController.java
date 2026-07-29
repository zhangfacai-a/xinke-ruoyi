package com.xinke.datahub.controller;

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
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.dto.DataHubAclRequest;
import com.xinke.datahub.domain.dto.DataHubDataQuery;
import com.xinke.datahub.service.DataHubDatasetService;

@RestController
@RequestMapping("/datahub/dataset")
public class DataHubDatasetController extends BaseController
{
    private final DataHubDatasetService datasetService;

    public DataHubDatasetController(DataHubDatasetService datasetService)
    {
        this.datasetService = datasetService;
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:list')")
    @GetMapping("/list")
    public TableDataInfo list(DataHubDataset query)
    {
        startPage();
        return getDataTable(datasetService.list(query, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:query')")
    @GetMapping("/{datasetId}")
    public AjaxResult detail(@PathVariable Long datasetId)
    {
        return success(datasetService.detail(datasetId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:query')")
    @PostMapping("/{datasetId}/data/query")
    public AjaxResult queryData(@PathVariable Long datasetId, @RequestBody(required = false) DataHubDataQuery query)
    {
        return success(datasetService.queryData(datasetId, query, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyPermi('datahub:dataset:query,datahub:job:list')")
    @GetMapping("/{datasetId}/jobs")
    public AjaxResult jobs(@PathVariable Long datasetId)
    {
        return success(datasetService.jobs(datasetId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:grant')")
    @GetMapping("/{datasetId}/acl")
    public AjaxResult acl(@PathVariable Long datasetId)
    {
        return success(datasetService.acl(datasetId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:grant')")
    @Log(title = "数据表授权", businessType = BusinessType.GRANT)
    @PutMapping("/{datasetId}/acl")
    public AjaxResult replaceAcl(@PathVariable Long datasetId, @RequestBody DataHubAclRequest request)
    {
        datasetService.replaceAcl(datasetId, request, getUserId(), getUsername(), SecurityUtils.isAdmin());
        return success();
    }
}
