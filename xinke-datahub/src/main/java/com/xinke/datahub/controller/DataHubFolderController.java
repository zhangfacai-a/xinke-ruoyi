package com.xinke.datahub.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.datahub.domain.dto.DataHubFolderItemMoveRequest;
import com.xinke.datahub.domain.dto.DataHubFolderRequest;
import com.xinke.datahub.service.DataHubFolderService;

@RestController
@RequestMapping("/datahub/folder")
public class DataHubFolderController extends BaseController
{
    private final DataHubFolderService folderService;

    public DataHubFolderController(DataHubFolderService folderService)
    {
        this.folderService = folderService;
    }

    @PreAuthorize("@ss.hasPermi('datahub:folder:list')")
    @GetMapping("/tree")
    public AjaxResult tree()
    {
        return success(folderService.tree(getUserId(), SecurityUtils.isAdmin()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:folder:add')")
    @Log(title = "DataHub文件夹", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody DataHubFolderRequest request)
    {
        return success(folderService.create(request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:folder:edit')")
    @Log(title = "DataHub文件夹", businessType = BusinessType.UPDATE)
    @PutMapping("/{folderId}")
    public AjaxResult update(@PathVariable Long folderId, @RequestBody DataHubFolderRequest request)
    {
        return success(folderService.update(folderId, request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('datahub:folder:remove')")
    @Log(title = "DataHub文件夹", businessType = BusinessType.DELETE)
    @DeleteMapping("/{folderId}")
    public AjaxResult delete(@PathVariable Long folderId, @RequestParam Integer lockVersion)
    {
        folderService.delete(folderId, lockVersion, getUserId(), getUsername(), SecurityUtils.isAdmin());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('datahub:folder:item:edit')")
    @Log(title = "DataHub数据表整理", businessType = BusinessType.UPDATE)
    @PutMapping("/item/{datasetId}")
    public AjaxResult moveItem(@PathVariable Long datasetId, @RequestBody DataHubFolderItemMoveRequest request)
    {
        return success(folderService.moveItem(datasetId, request, getUserId(), getUsername(), SecurityUtils.isAdmin()));
    }
}
