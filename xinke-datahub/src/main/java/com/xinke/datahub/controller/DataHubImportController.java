package com.xinke.datahub.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.annotation.Log;
import com.xinke.common.annotation.RepeatSubmit;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.domain.dto.DataHubSheetRequest;
import com.xinke.datahub.service.DataHubImportService;

@RestController
@RequestMapping("/datahub/import")
public class DataHubImportController extends BaseController
{
    private final DataHubImportService importService;

    public DataHubImportController(DataHubImportService importService)
    {
        this.importService = importService;
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:add')")
    @Log(title = "数据表上传预览", businessType = BusinessType.IMPORT)
    @PostMapping("/preview")
    public AjaxResult preview(@RequestParam MultipartFile file,
            @RequestParam(required = false) String sheetName)
    {
        return success(importService.preview(file, sheetName, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:add')")
    @PutMapping("/{previewId}/sheet")
    public AjaxResult changeSheet(@PathVariable String previewId, @RequestBody DataHubSheetRequest request)
    {
        return success(importService.changeSheet(previewId, request.getSheetName(), getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:add')")
    @Log(title = "创建动态数据表", businessType = BusinessType.IMPORT)
    @RepeatSubmit
    @PostMapping("/{previewId}/confirm")
    public AjaxResult confirm(@PathVariable String previewId, @RequestBody DataHubCreateRequest request)
    {
        return success(importService.confirmCreate(previewId, request, getUserId()));
    }

    @PreAuthorize("@ss.hasAnyPermi('datahub:dataset:add,datahub:dataset:append,datahub:dataset:replace,"
            + "datahub:dataset:edit,datahub:dataset:clear,datahub:dataset:rollback,datahub:job:query')")
    @GetMapping("/{previewId}")
    public AjaxResult job(@PathVariable String previewId)
    {
        return success(importService.getJob(previewId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyPermi('datahub:dataset:add,datahub:dataset:append,datahub:dataset:replace,"
            + "datahub:dataset:edit,datahub:dataset:clear,datahub:dataset:rollback,datahub:job:query')")
    @GetMapping("/{previewId}/errors")
    public AjaxResult errors(@PathVariable String previewId)
    {
        return success(importService.getErrors(previewId, getUserId(), SecurityUtils.isAdmin()));
    }
}
