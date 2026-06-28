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
import com.ruoyi.erp.domain.ErpShop;
import com.ruoyi.erp.service.IErpShopService;

@RestController
@RequestMapping("/erp/shop")
public class ErpShopController extends BaseController
{
    @Autowired
    private IErpShopService shopService;

    @PreAuthorize("@ss.hasPermi('erp:shop:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpShop shop)
    {
        startPage();
        List<ErpShop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:shop:query')")
    @GetMapping("/{shopId}")
    public AjaxResult getInfo(@PathVariable Long shopId)
    {
        return success(shopService.selectShopById(shopId));
    }

    @PreAuthorize("@ss.hasPermi('erp:shop:add')")
    @Log(title = "ERP 店铺", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ErpShop shop)
    {
        shop.setCreateBy(getUsername());
        return toAjax(shopService.insertShop(shop));
    }

    @PreAuthorize("@ss.hasPermi('erp:shop:edit')")
    @Log(title = "ERP 店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ErpShop shop)
    {
        shop.setUpdateBy(getUsername());
        return toAjax(shopService.updateShop(shop));
    }

    @PreAuthorize("@ss.hasPermi('erp:shop:remove')")
    @Log(title = "ERP 店铺", businessType = BusinessType.DELETE)
    @DeleteMapping("/{shopIds}")
    public AjaxResult remove(@PathVariable Long[] shopIds)
    {
        return toAjax(shopService.deleteShopByIds(shopIds));
    }
}
