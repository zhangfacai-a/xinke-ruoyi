package com.ruoyi.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.erp.domain.ErpOrderItem;
import com.ruoyi.erp.service.IErpOrderService;

@RestController
@RequestMapping("/erp/order")
public class ErpOrderController extends BaseController
{
    @Autowired
    private IErpOrderService orderService;

    @PreAuthorize("@ss.hasPermi('erp:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpOrderItem orderItem)
    {
        startPage();
        List<ErpOrderItem> list = orderService.selectOrderItemList(orderItem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:order:query')")
    @GetMapping("/{orderItemId}")
    public AjaxResult getInfo(@PathVariable Long orderItemId)
    {
        return success(orderService.selectOrderItemById(orderItemId));
    }
}
