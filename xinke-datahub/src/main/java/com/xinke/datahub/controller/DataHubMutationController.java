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
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.datahub.domain.dto.DataHubConfirmationRequest;
import com.xinke.datahub.domain.dto.DataHubEditRequest;
import com.xinke.datahub.domain.dto.DataHubMutationConfirmRequest;
import com.xinke.datahub.domain.dto.DataHubSheetRequest;
import com.xinke.datahub.service.DataHubMutationService;

@RestController
@RequestMapping("/datahub/dataset")
public class DataHubMutationController extends BaseController
{
    private final DataHubMutationService mutationService;

    public DataHubMutationController(DataHubMutationService mutationService)
    {
        this.mutationService = mutationService;
    }

    @PreAuthorize("(#operation != null && #operation.equalsIgnoreCase('APPEND')"
            + " && @ss.hasPermi('datahub:dataset:append'))"
            + " || (#operation != null && #operation.equalsIgnoreCase('REPLACE')"
            + " && @ss.hasPermi('datahub:dataset:replace'))")
    @Log(title = "数据表变更预览", businessType = BusinessType.IMPORT)
    @PostMapping("/{datasetId}/import/preview")
    public AjaxResult preview(@PathVariable Long datasetId, @RequestParam MultipartFile file,
            @RequestParam String operation, @RequestParam Long baseVersionId,
            @RequestParam(required = false) String sheetName)
    {
        return success(mutationService.previewFile(datasetId, operation, baseVersionId, file, sheetName,
                getUserId(), getUsername(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyPermi('datahub:dataset:append,datahub:dataset:replace')")
    @PutMapping("/{datasetId}/import/{previewId}/sheet")
    public AjaxResult changeSheet(@PathVariable Long datasetId, @PathVariable String previewId,
            @RequestBody DataHubSheetRequest request)
    {
        requireFilePermission(mutationService.previewOperation(datasetId, previewId,
                getUserId(), SecurityUtils.isAdmin()));
        return success(mutationService.changeSheet(datasetId, previewId, request.getSheetName(),
                getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasAnyPermi('datahub:dataset:append,datahub:dataset:replace')")
    @Log(title = "提交数据表变更", businessType = BusinessType.IMPORT)
    @RepeatSubmit
    @PostMapping("/{datasetId}/import/{previewId}/confirm")
    public AjaxResult confirm(@PathVariable Long datasetId, @PathVariable String previewId,
            @RequestBody DataHubMutationConfirmRequest request)
    {
        requireFilePermission(mutationService.previewOperation(datasetId, previewId,
                getUserId(), SecurityUtils.isAdmin()));
        return success(mutationService.confirmFile(datasetId, previewId, request,
                getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:edit')")
    @Log(title = "编辑数据表数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{datasetId}/edit")
    public AjaxResult edit(@PathVariable Long datasetId, @RequestBody DataHubEditRequest request)
    {
        return success(mutationService.edit(datasetId, request, getUserId(), getUsername(),
                SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:clear')")
    @Log(title = "清空数据表数据", businessType = BusinessType.CLEAN)
    @RepeatSubmit
    @PostMapping("/{datasetId}/clear")
    public AjaxResult clear(@PathVariable Long datasetId, @RequestBody DataHubConfirmationRequest request)
    {
        return success(mutationService.clear(datasetId, request, getUserId(), getUsername(),
                SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:query')")
    @GetMapping("/{datasetId}/versions")
    public AjaxResult versions(@PathVariable Long datasetId)
    {
        return success(mutationService.versions(datasetId, getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:dataset:rollback')")
    @Log(title = "回滚数据表版本", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{datasetId}/versions/{versionId}/rollback")
    public AjaxResult rollback(@PathVariable Long datasetId, @PathVariable Long versionId,
            @RequestBody DataHubConfirmationRequest request)
    {
        return success(mutationService.rollback(datasetId, versionId, request, getUserId(), getUsername(),
                SecurityUtils.isAdmin()));
    }

    private void requireFilePermission(String operation)
    {
        String permission = "REPLACE".equals(operation)
                ? "datahub:dataset:replace" : "datahub:dataset:append";
        if (!SecurityUtils.hasPermi(permission)) throw new ServiceException("没有权限访问该数据变更任务");
    }
}
