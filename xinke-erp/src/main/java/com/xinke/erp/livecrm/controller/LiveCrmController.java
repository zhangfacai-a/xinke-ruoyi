package com.xinke.erp.livecrm.controller;

import java.util.*;
import java.util.concurrent.Semaphore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.xinke.common.annotation.Anonymous;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.utils.ip.IpUtils;
import com.xinke.erp.livecrm.domain.*;
import com.xinke.erp.livecrm.service.LiveCrmService;
import com.xinke.erp.livecrm.service.LiveCrmPayloadCrypto;
import com.xinke.erp.livecrm.service.LiveCrmAttachmentService;

@RestController
public class LiveCrmController extends BaseController {
    private static final String UPLOAD_KEY = "LIVE_CUSTOMER_V2_LOCAL_KEY";
    private static final int MAX_BATCH_EVENTS = 500;
    private static final Semaphore INGEST_LIMIT = new Semaphore(2);
    private final LiveCrmService service;
    private final LiveCrmAttachmentService attachmentService;
    private final LiveCrmPayloadCrypto payloadCrypto;
    public LiveCrmController(LiveCrmService service, LiveCrmAttachmentService attachmentService, LiveCrmPayloadCrypto payloadCrypto){this.service=service;this.attachmentService=attachmentService;this.payloadCrypto=payloadCrypto;}
    @Anonymous @PostMapping({"/open-api/live-crm/v2/plugin/handshake","/open-api/v2/live/plugin/handshake"}) public AjaxResult handshake(@RequestHeader(value="X-Live-Crm-Key",required=false) String key,@RequestHeader(value="X-Plugin-Version",required=false) String version){if(!UPLOAD_KEY.equals(key)||!service.isPluginVersionSupported(version))return error("PLUGIN_VERSION_EXPIRED");return success(service.handshake(version));}
    @Anonymous @GetMapping({"/open-api/live-crm/v2/plugin/version","/open-api/v2/live/plugin/version"}) public AjaxResult version(@RequestHeader(value="X-Live-Crm-Key",required=false) String key){if(!UPLOAD_KEY.equals(key))return error("PLUGIN_VERSION_EXPIRED");return success(service.versionInfo());}
    @Anonymous @PostMapping({"/open-api/live-crm/v2/sessions/ensure","/open-api/v2/live/sessions/ensure"}) public AjaxResult ensure(@RequestHeader(value="X-Live-Crm-Key",required=false) String key,@Valid @RequestBody LiveCrmSessionRequest req){if(!UPLOAD_KEY.equals(key))return error("PLUGIN_VERSION_EXPIRED");return success(service.ensureSession(req,null));}
    /** Web users may select a room before today's session exists. Create/reuse it without exposing the plugin key. */
    @PostMapping("/live/v2/sessions/ensure") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customerCenter:edit,live:customer:list,live:audienceV2:list,live:audienceV2:edit')")
    public AjaxResult ensureForUser(@Valid @RequestBody LiveCrmSessionRequest req){return success(service.ensureSession(req,getUserId()));}
    @Anonymous @PostMapping({"/open-api/live-crm/v2/sessions/{id}/events:batch","/open-api/v2/live/sessions/{id}/events:batch"}) public AjaxResult ingest(@RequestHeader(value="X-Live-Crm-Key",required=false) String key,@PathVariable Long id,@Valid @RequestBody LiveCrmBatchRequest req){if(!UPLOAD_KEY.equals(key))return error("PLUGIN_VERSION_EXPIRED");if(!Objects.equals(id,req.getSessionId()))return error("sessionId不匹配");try{payloadCrypto.decryptInto(req);}catch(IllegalArgumentException ex){return error("PAYLOAD_DECRYPT_FAILED");}int eventCount=req.getEvents()==null?0:req.getEvents().size();if(eventCount>MAX_BATCH_EVENTS){AjaxResult tooLarge=error("单次同步数据过多，请等待插件自动分批重试");tooLarge.put("errorCode","BATCH_TOO_LARGE");tooLarge.put("maxBatchEvents",MAX_BATCH_EVENTS);return tooLarge;}if(!INGEST_LIMIT.tryAcquire()) { AjaxResult busy=error("服务器正在处理其他同步，请稍后重试"); busy.put("retryAfter",30); return busy; } try { return success(service.ingest(req)); } finally { INGEST_LIMIT.release(); }}
    @PostMapping("/live/v2/registrations") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:list,live:audienceV2:edit')") public AjaxResult register(@Valid @RequestBody LiveCrmRegistrationRequest req){return success(service.register(req,getUserId(),getUsername()));}
    @GetMapping("/live/v2/options") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customerCenter:edit,live:customer:list,live:audienceV2:list')") public AjaxResult options(){return success(service.options());}
    @DeleteMapping("/live/v2/rooms/{roomId}") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:edit,live:audienceV2:edit')") public AjaxResult deleteRoom(@PathVariable Long roomId){service.deleteRoom(roomId);return success();}
    @GetMapping("/live/v2/customers/{id}") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')") public AjaxResult customer(@PathVariable Long id){return success(service.customerDetail(id));}
    @GetMapping({"/live-crm/v2/workbench","/live/v2/workbench","/live/v2/customers"}) @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')") public TableDataInfo workbench(@RequestParam(required=false) String keyword,@RequestParam(required=false) String stage,@RequestParam(required=false) Boolean claimedOnly,@RequestParam(required=false) Long ownerUserId,@RequestParam(required=false) Long sessionId,@RequestParam(required=false) String beginDate,@RequestParam(required=false) String endDate,@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){List<Map<String,Object>> rows=service.listWorkbench(keyword,stage,claimedOnly,ownerUserId,sessionId,beginDate,endDate,pageNum,pageSize);return new TableDataInfo(rows,service.countWorkbench(keyword,stage,claimedOnly,ownerUserId,sessionId,beginDate,endDate));}
    /**
     * New customer-center inbox.  It is deliberately separate from the
     * frozen workbench endpoint so the old page keeps its historical shape.
     */
    @GetMapping({"/live/v2/inbox","/live-crm/v2/inbox"})
    @PreAuthorize("@ss.hasAnyPermi('live:customer:list,live:customerCenter:list,live:audienceV2:list')")
    public TableDataInfo inbox(@RequestParam(required=false) String keyword,
                               @RequestParam(required=false) String stage,
                               @RequestParam(required=false) Long sessionId,
                               @RequestParam(required=false) String filter,
                               @RequestParam(required=false) String interactionType,
                               @RequestParam(required=false) String beginDate,
                               @RequestParam(required=false) String endDate,
                               @RequestParam(defaultValue="1") int pageNum,
                               @RequestParam(defaultValue="20") int pageSize){
        List<Map<String,Object>> rows=service.listInbox(keyword,stage,sessionId,filter,interactionType,beginDate,endDate,pageNum,pageSize);
        return new TableDataInfo(rows,service.countInbox(keyword,stage,sessionId,filter,interactionType,beginDate,endDate));
    }
    @GetMapping({"/live/v2/inbox/summary","/live-crm/v2/inbox/summary"})
    @PreAuthorize("@ss.hasAnyPermi('live:customer:list,live:customerCenter:list,live:audienceV2:list')")
    public AjaxResult inboxSummary(@RequestParam(required=false) Long sessionId,
                                   @RequestParam(required=false) String interactionType,
                                   @RequestParam(required=false) String beginDate,
                                   @RequestParam(required=false) String endDate){return success(service.inboxSummary(sessionId, interactionType, beginDate, endDate));}
    @GetMapping("/live/v2/workbench/summary") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')") public AjaxResult workbenchSummary(@RequestParam(required=false) Long sessionId){return success(service.workbenchSummary(sessionId));}
    @GetMapping("/live/v2/tasks") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')") public TableDataInfo tasks(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){return new TableDataInfo(service.listTasks(pageNum,pageSize),service.countTasks());}
    @PostMapping("/live/v2/tasks/phone-handoff/run") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:edit,live:audienceV2:edit')") public AjaxResult runPhoneHandoff(){return success(Map.of("created",service.processPhoneHandoffs()));}
    @PostMapping("/live/v2/tasks/{id}/action") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:list,live:audienceV2:edit')") public AjaxResult taskAction(@PathVariable Long id,@Valid @RequestBody LiveCrmTaskActionRequest req){return success(service.actOnTask(id,req,getUserId(),getUsername()));}
    @GetMapping("/live/v2/orders") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')") public TableDataInfo orders(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){return new TableDataInfo(service.listOrders(pageNum,pageSize),service.countOrders());}
    @PutMapping("/live/v2/orders/{id}") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:list,live:audienceV2:edit')") public AjaxResult orderUpdate(@PathVariable Long id,@Valid @RequestBody LiveCrmOrderUpdateRequest req){return success(service.updateOrder(id,req));}
    @GetMapping({"/live-crm/v2/upload-batches","/live/v2/upload-batches"}) @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:records,live:customer:upload:list,live:audienceV2:list,live:audienceV2:records')") public TableDataInfo batches(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){List<Map<String,Object>> rows=service.listBatches(pageNum,pageSize);return new TableDataInfo(rows,service.countBatches());}
    @GetMapping({"/live-crm/v2/upload-batches/{id}","/live/v2/upload-batches/{id}"}) @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:records,live:customer:upload:list,live:audienceV2:list,live:audienceV2:records')") public AjaxResult batch(@PathVariable Long id){return success(service.batchDetail(id));}
    @PostMapping("/live/v2/attachments/presign") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:edit,live:audienceV2:edit')")
    public AjaxResult attachmentPresign(@Valid @RequestBody LiveCrmAttachmentPresignRequest req){return success(attachmentService.presign(req,getUserId()));}
    @PostMapping(value="/live/v2/attachments/upload", consumes="multipart/form-data") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:edit,live:audienceV2:edit')")
    public AjaxResult attachmentUpload(@RequestPart("file") MultipartFile file, @RequestParam Long customerId,
                                       @RequestParam(required=false) Long opportunityId, @RequestParam(required=false) Long orderId){
        return success(attachmentService.upload(file, customerId, opportunityId, orderId, getUserId()));
    }
    @PostMapping("/live/v2/attachments/{id}/complete") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:edit,live:customer:edit,live:audienceV2:edit')")
    public AjaxResult attachmentComplete(@PathVariable Long id,@Valid @RequestBody(required=false) LiveCrmAttachmentCompleteRequest req){return success(attachmentService.complete(id,req));}
    @GetMapping("/live/v2/attachments/{id}/url") @PreAuthorize("@ss.hasAnyPermi('live:customerCenter:list,live:customer:list,live:audienceV2:list')")
    public AjaxResult attachmentUrl(@PathVariable Long id){return success(attachmentService.viewUrl(id));}
}
