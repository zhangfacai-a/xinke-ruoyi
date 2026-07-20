package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.domain.ErpAfterSaleCreateRequest;
import com.xinke.erp.domain.ErpOrderItem;
import com.xinke.erp.domain.ErpShipmentCreateRequest;
import com.xinke.erp.service.IErpOrderService;
import jakarta.validation.Valid;

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

    @PreAuthorize("@ss.hasPermi('erp:order:list')")
    @GetMapping("/fulfillment/list")
    public TableDataInfo fulfillmentList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(orderService.selectFulfillmentOrderList(query));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:list')")
    @GetMapping("/fulfillment/summary")
    public AjaxResult fulfillmentSummary(@RequestParam Map<String, Object> query)
    {
        return success(orderService.selectFulfillmentSummary(query));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:query')")
    @GetMapping("/fulfillment/{salesId}")
    public AjaxResult fulfillmentDetail(@PathVariable Long salesId)
    {
        return success(orderService.selectFulfillmentDetail(salesId));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:sync')")
    @Log(title = "同步销售履约订单", businessType = BusinessType.IMPORT)
    @PostMapping("/fulfillment/sync")
    public AjaxResult syncFulfillment()
    {
        return success(orderService.syncFulfillmentOrders(getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:shipment')")
    @Log(title = "创建销售发货单", businessType = BusinessType.INSERT)
    @PostMapping("/shipment")
    public AjaxResult createShipment(@Valid @RequestBody ErpShipmentCreateRequest request)
    {
        return success((Object) orderService.createShipment(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:shipment')")
    @Log(title = "确认销售发货", businessType = BusinessType.UPDATE)
    @PostMapping("/shipment/{shipmentNo}/ship")
    public AjaxResult shipShipment(@PathVariable String shipmentNo)
    {
        return toAjax(orderService.shipShipment(shipmentNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:shipment')")
    @Log(title = "销售包裹签收", businessType = BusinessType.UPDATE)
    @PostMapping("/shipment/{shipmentNo}/sign")
    public AjaxResult signShipment(@PathVariable String shipmentNo)
    {
        return toAjax(orderService.signShipment(shipmentNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:shipment')")
    @Log(title = "取消销售发货单", businessType = BusinessType.UPDATE)
    @PostMapping("/shipment/{shipmentNo}/cancel")
    public AjaxResult cancelShipment(@PathVariable String shipmentNo,
                                     @RequestBody(required = false) Map<String, Object> body)
    {
        String reason = body == null ? null : String.valueOf(body.getOrDefault("reason", ""));
        return toAjax(orderService.cancelShipment(shipmentNo, reason, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:after-sale')")
    @Log(title = "创建销售售后单", businessType = BusinessType.INSERT)
    @PostMapping("/after-sale")
    public AjaxResult createAfterSale(@Valid @RequestBody ErpAfterSaleCreateRequest request)
    {
        return success((Object) orderService.createAfterSale(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:after-sale')")
    @Log(title = "审核销售售后单", businessType = BusinessType.UPDATE)
    @PostMapping("/after-sale/{afterSaleNo}/approve")
    public AjaxResult approveAfterSale(@PathVariable String afterSaleNo)
    {
        return toAjax(orderService.approveAfterSale(afterSaleNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:after-sale')")
    @Log(title = "驳回销售售后单", businessType = BusinessType.UPDATE)
    @PostMapping("/after-sale/{afterSaleNo}/reject")
    public AjaxResult rejectAfterSale(@PathVariable String afterSaleNo,
                                      @RequestBody(required = false) Map<String, Object> body)
    {
        String reason = body == null ? null : String.valueOf(body.getOrDefault("reason", ""));
        return toAjax(orderService.rejectAfterSale(afterSaleNo, reason, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:after-sale')")
    @Log(title = "完成销售售后单", businessType = BusinessType.UPDATE)
    @PostMapping("/after-sale/{afterSaleNo}/complete")
    public AjaxResult completeAfterSale(@PathVariable String afterSaleNo)
    {
        return toAjax(orderService.completeAfterSale(afterSaleNo, getUsername()));
    }
}
