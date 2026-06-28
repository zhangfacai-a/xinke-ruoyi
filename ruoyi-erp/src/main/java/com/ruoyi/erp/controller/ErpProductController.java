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
import com.ruoyi.erp.domain.ErpProduct;
import com.ruoyi.erp.service.IErpProductService;

@RestController
@RequestMapping("/erp/product")
public class ErpProductController extends BaseController
{
    @Autowired
    private IErpProductService productService;

    @PreAuthorize("@ss.hasPermi('erp:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpProduct product)
    {
        startPage();
        List<ErpProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:product:query')")
    @GetMapping("/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        return success(productService.selectProductById(productId));
    }

    @PreAuthorize("@ss.hasPermi('erp:product:add')")
    @Log(title = "ERP Product", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpProduct product)
    {
        product.setCreateBy(getUsername());
        return toAjax(productService.insertProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('erp:product:edit')")
    @Log(title = "ERP Product", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpProduct product)
    {
        product.setUpdateBy(getUsername());
        return toAjax(productService.updateProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('erp:product:remove')")
    @Log(title = "ERP Product", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(productService.deleteProductByIds(productIds));
    }
}
