package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.poi.ExcelUtil;
import com.xinke.erp.domain.LiveGiftBatchSaveRequest;
import com.xinke.erp.domain.LiveGiftCostImportRow;
import com.xinke.erp.domain.LiveGiftLedgerExport;
import com.xinke.erp.domain.LiveGiftSaveRequest;
import com.xinke.erp.service.ILiveGiftService;

@RestController
@RequestMapping("/live/gift")
public class LiveGiftController extends BaseController
{
    @Autowired private ILiveGiftService service;

    @GetMapping("/staff") @PreAuthorize("@ss.hasAnyPermi('live:gift:room,live:gift:mapping,live:gift:admin,live:gift:template,live:gift:entry')")
    public AjaxResult staff(@RequestParam Map<String,Object> q){return success(service.listLiveUserOptions(q));}
    @PostMapping("/staff/sync") @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title="同步钉钉系统用户",businessType=BusinessType.IMPORT)
    public AjaxResult sync(){return success(service.syncDingTalk(getUsername()));}
    @GetMapping("/shop-options") @PreAuthorize("@ss.hasAnyPermi('live:gift:room,live:gift:mapping,live:gift:entry')")
    public AjaxResult shopOptions(){return success(service.listShopOptions());}

    @GetMapping("/shop") @PreAuthorize("@ss.hasAnyPermi('live:gift:room,live:gift:mapping')")
    public AjaxResult shops(@RequestParam Map<String,Object> q){return success(service.listShops(q));}
    @GetMapping("/shop/{id}") @PreAuthorize("@ss.hasPermi('live:gift:room')")
    public AjaxResult shop(@PathVariable Long id){return success(service.getShop(id));}
    @PostMapping("/shop") @PreAuthorize("@ss.hasPermi('live:gift:room')")
    public AjaxResult shopSave(@RequestBody Map<String,Object> value){service.saveShop(value,getUsername());return success(value);}

    @GetMapping("/room") @PreAuthorize("@ss.hasAnyPermi('live:gift:room,live:gift:mapping')")
    public AjaxResult rooms(@RequestParam Map<String,Object> q){return success(service.listRooms(q));}
    @GetMapping("/room/{id}") @PreAuthorize("@ss.hasPermi('live:gift:room')")
    public AjaxResult room(@PathVariable Long id){return success(service.getRoom(id));}
    @PostMapping("/room") @PreAuthorize("@ss.hasPermi('live:gift:room')")
    public AjaxResult roomSave(@RequestBody Map<String,Object> value){service.saveRoom(value,getUsername());return success(value);}

    @GetMapping("/mapping") @PreAuthorize("@ss.hasAnyPermi('live:gift:room,live:gift:mapping,live:gift:template,live:gift:entry')")
    public AjaxResult mappings(@RequestParam Map<String,Object> q){return success(service.listSubjectMappings(q));}
    @PostMapping("/mapping") @PreAuthorize("@ss.hasPermi('live:gift:mapping')")
    public AjaxResult mappingSave(@RequestBody Map<String,Object> value){service.saveSubjectMapping(value,getUsername());return success();}

    @GetMapping("/template") @PreAuthorize("@ss.hasAnyPermi('live:gift:template,live:gift:entry')")
    public AjaxResult templates(){return success(service.listQuickTemplates(getUserId()));}
    @GetMapping("/template/{id}") @PreAuthorize("@ss.hasAnyPermi('live:gift:template,live:gift:entry')")
    public AjaxResult template(@PathVariable Long id){return success(service.getQuickTemplate(id,getUserId()));}
    @PostMapping("/template") @PreAuthorize("@ss.hasPermi('live:gift:template')")
    public AjaxResult templateSave(@RequestBody Map<String,Object> value){service.saveQuickTemplate(value,getUserId(),getUsername());return success();}
    @DeleteMapping("/template/{id}") @PreAuthorize("@ss.hasPermi('live:gift:template')")
    public AjaxResult templateDelete(@PathVariable Long id){service.deleteQuickTemplate(id,getUserId());return success();}

    @GetMapping("/daily") @PreAuthorize("@ss.hasAnyPermi('live:gift:daily,live:gift:entry')")
    public AjaxResult daily(@RequestParam Map<String,Object> q){return success(service.listDaily(q));}
    @GetMapping("/daily/{id}") @PreAuthorize("@ss.hasPermi('live:gift:daily')")
    public AjaxResult dailyOne(@PathVariable Long id){return success(service.getDaily(id));}
    @PostMapping("/daily") @PreAuthorize("@ss.hasPermi('live:gift:daily')")
    public AjaxResult dailySave(@RequestBody Map<String,Object> value){service.saveDaily(value,getUsername());return success();}

    @GetMapping("/catalog") @PreAuthorize("@ss.hasAnyPermi('live:gift:catalog,live:gift:entry,live:gift:preference')")
    public AjaxResult gifts(@RequestParam Map<String,Object> q){q.put("userId", getUserId());return success(service.listGifts(q));}
    @GetMapping("/catalog/{id}") @PreAuthorize("@ss.hasPermi('live:gift:catalog')")
    public AjaxResult gift(@PathVariable Long id){return success(service.getGift(id));}
    @PostMapping("/catalog") @PreAuthorize("@ss.hasPermi('live:gift:catalog')")
    public AjaxResult giftSave(@RequestBody Map<String,Object> value){service.saveGift(value,getUsername());return success();}
    @PostMapping("/catalog/preference") @PreAuthorize("@ss.hasAnyPermi('live:gift:catalog,live:gift:entry,live:gift:preference')")
    public AjaxResult giftPreferenceSave(@RequestBody Map<String,Object> value){service.saveGiftPreference(value,getUserId(),getUsername());return success();}
    @PostMapping("/catalog/cost") @PreAuthorize("@ss.hasPermi('live:gift:cost')")
    @Log(title="调整礼品成本",businessType=BusinessType.UPDATE)
    public AjaxResult cost(@RequestBody Map<String,Object> value){service.addCost(value,getUsername());return success();}
    @PostMapping("/catalog/import") @PreAuthorize("@ss.hasPermi('live:gift:catalog')")
    @Log(title="导入礼品成本历史",businessType=BusinessType.IMPORT)
    public AjaxResult importCatalog(MultipartFile file) throws Exception
    {
        List<LiveGiftCostImportRow> rows=new ExcelUtil<>(LiveGiftCostImportRow.class).importExcel(file.getInputStream());
        return success(service.importGiftCosts(rows,getUsername()));
    }

    @GetMapping("/order/{orderNo}") @PreAuthorize("@ss.hasPermi('live:gift:entry')")
    public AjaxResult order(@PathVariable String orderNo){return success(service.getOrder(orderNo));}
    @PostMapping("/order") @PreAuthorize("@ss.hasPermi('live:gift:entry')")
    @Log(title="订单录礼品",businessType=BusinessType.UPDATE)
    public AjaxResult orderSave(@Valid @RequestBody LiveGiftSaveRequest request){service.saveOrderGift(request,getUsername());return success();}
    @GetMapping("/room-preference") @PreAuthorize("@ss.hasPermi('live:gift:entry')")
    public AjaxResult roomPreference(){return success(service.getRoomPreference(getUserId()));}
    @PostMapping("/room-preference") @PreAuthorize("@ss.hasPermi('live:gift:entry')")
    public AjaxResult roomPreferenceSave(@RequestBody Map<String,Object> value){Object id=value.get("roomId");service.saveRoomPreference(getUserId(),id==null?null:Long.valueOf(id.toString()),getUsername());return success();}
    @PostMapping("/order/batch") @PreAuthorize("@ss.hasPermi('live:gift:entry')")
    @Log(title="批量订单录礼品",businessType=BusinessType.UPDATE)
    public AjaxResult orderBatch(@Valid @RequestBody LiveGiftBatchSaveRequest request){return success(service.batchSaveOrderGifts(request,getUsername()));}

    @GetMapping("/ledger") @PreAuthorize("@ss.hasPermi('live:gift:ledger')")
    public AjaxResult ledger(@RequestParam Map<String,Object> q){return success(service.ledger(q));}
    @GetMapping("/inventory") @PreAuthorize("@ss.hasAnyPermi('live:gift:inventory,live:gift:entry')")
    public AjaxResult inventory(@RequestParam Map<String,Object> q){return success(service.inventory(q));}
    @GetMapping("/inventory/movements") @PreAuthorize("@ss.hasPermi('live:gift:inventory')")
    public AjaxResult inventoryMovements(@RequestParam Map<String,Object> q){return success(service.inventoryMovements(q));}
    @GetMapping("/inventory/summary") @PreAuthorize("@ss.hasAnyPermi('live:gift:inventory,live:gift:summary')")
    public AjaxResult inventorySummary(@RequestParam Map<String,Object> q){return success(service.inventorySummary(q));}
    @PostMapping("/inventory/adjust") @PreAuthorize("@ss.hasPermi('live:gift:inventory:adjust')")
    @Log(title="调整礼品库存",businessType=BusinessType.UPDATE)
    public AjaxResult inventoryAdjust(@RequestBody Map<String,Object> value){service.adjustInventory(value,getUsername());return success();}
    @PostMapping("/ledger/export") @PreAuthorize("@ss.hasPermi('live:gift:export')")
    @Log(title="导出礼品记录",businessType=BusinessType.EXPORT)
    public void export(HttpServletResponse response,@RequestParam Map<String,Object> q)
    {
        List<LiveGiftLedgerExport> rows=service.ledger(q).stream().map(LiveGiftLedgerExport::from).toList();
        new ExcelUtil<>(LiveGiftLedgerExport.class).exportExcel(response,rows,"订单礼品记录");
    }
    @GetMapping("/summary") @PreAuthorize("@ss.hasPermi('live:gift:summary')")
    public AjaxResult summary(@RequestParam Map<String,Object> q){return success(service.summary(q));}
}
