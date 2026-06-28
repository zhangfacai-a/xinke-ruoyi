package com.ruoyi.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.domain.ErpProductSku;
import com.ruoyi.erp.service.IErpProductSkuService;

@RestController
@RequestMapping("/erp/sku")
public class ErpProductSkuController extends BaseController
{
    @Autowired
    private IErpProductSkuService skuService;

    @PreAuthorize("@ss.hasPermi('erp:sku:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpProductSku sku)
    {
        startPage();
        List<ErpProductSku> list = skuService.selectSkuList(sku);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:sku:query')")
    @GetMapping("/{skuId}")
    public AjaxResult getInfo(@PathVariable Long skuId)
    {
        return success(skuService.selectSkuById(skuId));
    }

    @PreAuthorize("@ss.hasPermi('erp:sku:add')")
    @Log(title = "ERP SKU", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpProductSku sku)
    {
        sku.setCreateBy(getUsername());
        return toAjax(skuService.insertSku(sku));
    }

    @PreAuthorize("@ss.hasPermi('erp:sku:edit')")
    @Log(title = "ERP SKU", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpProductSku sku)
    {
        sku.setUpdateBy(getUsername());
        return toAjax(skuService.updateSku(sku));
    }

    @PreAuthorize("@ss.hasPermi('erp:sku:remove')")
    @Log(title = "ERP SKU", businessType = BusinessType.DELETE)
    @DeleteMapping("/{skuIds}")
    public AjaxResult remove(@PathVariable Long[] skuIds)
    {
        return toAjax(skuService.deleteSkuByIds(skuIds));
    }
}
