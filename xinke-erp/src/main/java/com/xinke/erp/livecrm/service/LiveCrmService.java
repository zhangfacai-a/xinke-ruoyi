package com.xinke.erp.livecrm.service;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.livecrm.domain.LiveCrmBatchRequest;
import com.xinke.erp.livecrm.domain.LiveCrmRegistrationRequest;
import com.xinke.erp.livecrm.domain.LiveCrmSessionRequest;
import com.xinke.erp.livecrm.domain.LiveCrmTaskActionRequest;
import com.xinke.erp.livecrm.domain.LiveCrmOrderUpdateRequest;
import com.xinke.erp.livecrm.mapper.LiveCrmMapper;

@Service
public class LiveCrmService {
    private static final Logger log = LoggerFactory.getLogger(LiveCrmService.class);
    private static final long INBOX_SUMMARY_CACHE_MILLIS = 15_000L;
    private static final Set<String> REGISTRATION_STAGES = Set.of("NOT_CONSULTED", "CONTACTED", "CONSULTING", "HIGH_INTENT", "ORDERED", "INVALID");
    private static final Set<String> INTERACTION_TYPES = Set.of("COMMENT", "BUY_INTENT", "CONSULT");
    private static final Set<String> FAN_STATUSES = Set.of("UNKNOWN", "UNFOLLOWED", "FOLLOWED");
    @org.springframework.beans.factory.annotation.Value("${live.audience-v2.enabled:true}")
    private boolean audienceV2Enabled;
    @org.springframework.beans.factory.annotation.Value("${live.audience-v2.write-mode:active}")
    private String audienceV2WriteMode;
    // Keep release controls under one prefix; retain the old fallback for local configs.
    @org.springframework.beans.factory.annotation.Value("${live.crm.v2.plugin-min-version:${live.audience-v2.plugin-min-version:2.4.0}}")
    private String minimumPluginVersion;
    @org.springframework.beans.factory.annotation.Value("${live.crm.v2.latest-version:3.0.0}")
    private String latestPluginVersion;
    @org.springframework.beans.factory.annotation.Value("${live.crm.v2.update-url:}")
    private String pluginUpdateUrl;
    @org.springframework.beans.factory.annotation.Value("${live.crm.v2.update-sha256:}")
    private String pluginUpdateSha256;
    private final LiveCrmMapper mapper;
    private final Map<InboxSummaryKey, InboxSummaryCacheEntry> inboxSummaryCache = new ConcurrentHashMap<>();
    public LiveCrmService(LiveCrmMapper mapper){this.mapper=mapper;}

    private record InboxSummaryKey(Long sessionId, String interactionType, String beginDate, String endDate) {}
    private record InboxSummaryCacheEntry(long expiresAt, Map<String,Object> value) {}

    @Transactional
    public Map<String,Object> ensureSession(LiveCrmSessionRequest request, Long userId){
        String room = request.getRoomName() == null ? "" : request.getRoomName().trim();
        if(room.isEmpty()) throw new ServiceException("直播间名称不能为空");
        String key = normalize(room);
        String date = normalizeLiveDate(request.getLiveDate());
        ensureRoom(room, key, request.getPlatformRoomId());
        Map<String,Object> current=mapper.findOpenSession(key,date);
        if(current!=null) return current;
        Map<String,Object> value=new HashMap<>(); value.put("roomName",room); value.put("roomNameKey",key);
        value.put("platformRoomId",request.getPlatformRoomId()); value.put("anchorId",request.getAnchorId()); value.put("liveDate",date); value.put("createdBy",userId);
        try {
            mapper.insertSession(value);
        } catch (RuntimeException ex) {
            // A concurrent plugin tab may create the same room/day between
            // the lookup and insert. Reuse that session instead of returning
            // a transient 500 to the operator.
            if (isDuplicateKey(ex)) {
                Map<String,Object> concurrent = mapper.findOpenSession(key, date);
                if (concurrent != null) return concurrent;
            }
            throw ex;
        }
        value.put("sessionId", value.get("id")); return value;
    }

    private void ensureRoom(String roomName, String roomNameKey, String platformRoomId) {
        if (mapper.findRoomByNameKey(roomNameKey) != null) return;
        Map<String,Object> room = new HashMap<>();
        room.put("roomName", roomName);
        room.put("roomNameKey", roomNameKey);
        room.put("platformRoomId", platformRoomId);
        try {
            mapper.insertDiscoveredRoom(room);
        } catch (RuntimeException ex) {
            // Multiple plugin tabs can discover the same new room together.
            // If another request created it first, treat this request as successful.
            if (isDuplicateKey(ex) && mapper.findRoomByNameKey(roomNameKey) != null) return;
            throw ex;
        }
    }

    @Transactional
    public Map<String,Object> options(){
        mapper.syncDiscoveredRooms();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("rooms",mapper.listRoomOptions());
        result.put("sessions",mapper.listSessions(100));
        result.put("users",mapper.listEnabledUsers());
        return result;
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Map<String,Object> room = mapper.findRoomById(roomId);
        if (room == null) throw new ServiceException("直播间不存在或已删除");
        String key = normalize(String.valueOf(room.get("roomName")));
        mapper.deleteRoomAttachments(key); mapper.deleteRoomOrders(key); mapper.deleteRoomTasks(key);
        mapper.deleteRoomOpportunities(key); mapper.deleteRoomRegistrations(key); mapper.deleteRoomInteractions(key);
        mapper.deleteRoomPresence(key); mapper.deleteRoomBatches(key); mapper.deleteRoomSessions(key);
        mapper.deleteRoomStaff(roomId); mapper.deleteRoom(roomId);
    }

    @Transactional
    public Map<String,Object> ingest(LiveCrmBatchRequest request){
        if(!audienceV2Enabled) return Map.of("accepted",0,"duplicate",0,"rejected",request.getEvents()==null?0:request.getEvents().size(),"errorCode","AUDIENCE_V2_DISABLED");
        if(!isPluginVersionSupported(request.getPluginVersion())) return Map.of("accepted",0,"duplicate",0,"rejected",request.getEvents()==null?0:request.getEvents().size(),"errorCode","PLUGIN_VERSION_EXPIRED");
        if(request.getEvents()==null) request.setEvents(List.of());
        if (mapper.getSession(request.getSessionId()) == null) {
            throw new ServiceException("直播场次不存在，请先建立直播场次");
        }
        Map<String,Object> batch=new HashMap<>(); batch.put("batchId",request.getBatchId()); batch.put("sessionId",request.getSessionId()); batch.put("installationId",request.getInstallationId()); batch.put("pluginVersion",request.getPluginVersion()); batch.put("eventCount",request.getEvents().size());
        try {
            mapper.insertBatch(batch);
        } catch (RuntimeException ex) {
            // A retry of an already committed batch is expected.  Do not
            // classify a missing table, connection failure, or other
            // database error as a duplicate: that would make the plugin
            // discard its local queue while nothing was persisted.
            if (isDuplicateKey(ex)) {
                return Map.of("batchId", request.getBatchId(), "accepted", 0,
                        "duplicate", request.getEvents().size(), "rejected", 0);
            }
            throw ex;
        }
        if("shadow".equalsIgnoreCase(audienceV2WriteMode)){
            batch.put("acceptedCount",0);batch.put("duplicateCount",0);batch.put("rejectedCount",0);batch.put("status","SHADOW");mapper.updateBatch(batch);
            return Map.of("batchId",request.getBatchId(),"accepted",0,"duplicate",0,"rejected",0,"shadowed",request.getEvents().size());
        }
        int accepted=0, duplicate=0, rejected=0, newCustomers=0, updatedCustomers=0;
        for(Map<String,Object> event:request.getEvents()) {
            try {
                String eventId=string(event,"eventId"); String nickname=string(event,"nickname");
                if(eventId==null || nickname==null) { rejected++; continue; }
                String type="VIEWER".equalsIgnoreCase(string(event,"eventType"))||"VIEWER_PRESENCE".equalsIgnoreCase(string(event,"eventType"))?"VIEWER":"COMMENT";
                Long watchSeconds=optionalNumber(event,"watchSeconds");
                if("VIEWER".equals(type) && (watchSeconds==null || watchSeconds<300L)) { rejected++; continue; }
                String secUid=firstString(event,"secUid","sec_uid");
                Map<String,Object> customer=resolveCustomer(nickname,secUid,"PLUGIN_REALTIME");
                if(Boolean.TRUE.equals(customer.remove("_created"))) newCustomers++; else updatedCustomers++;
                Date occurredAt = new Date(number(event,"occurredAt"));
                String dedupKey = string(event,"dedupKey");
                if (dedupKey == null && "COMMENT".equals(type)) {
                    String normalizedContent = normalize(string(event,"content"));
                    dedupKey = request.getSessionId() + "|" + normalize(nickname) + "|" + normalizedContent;
                }
                Map<String,Object> row=new HashMap<>(); row.put("eventId",eventId); row.put("dedupKey",dedupKey); row.put("ingestBatchId",batch.get("id")); row.put("sessionId",request.getSessionId()); row.put("customerId",customer.get("id")); row.put("eventType",type); row.put("nickname",nickname); row.put("secUid",secUid); row.put("content",string(event,"content")); row.put("isFollower",bool(event,"isFollower")); row.put("isFollowing",bool(event,"isFollowing")); row.put("occurredAt",occurredAt); row.put("sourceChannel","PLUGIN_REALTIME"); row.put("payloadJson",JSON.toJSONString(event));
                int n=mapper.insertInteraction(row);
                // Presence is a time-series heartbeat.  It must be touched
                // even when the interaction row is a duplicate (same
                // 3-minute bucket), otherwise last_seen_at stays at the
                // first upload and every customer appears as 0 minutes.
                if("VIEWER".equals(type) && n>0){
                    long lastSeenAt=occurredAt.getTime();
                    Long suppliedFirstSeen=optionalNumber(event,"firstSeenAt");
                    Long suppliedLastSeen=optionalNumber(event,"lastSeenAt");
                    if(suppliedLastSeen!=null) lastSeenAt=suppliedLastSeen;
                    long firstSeenAt=suppliedFirstSeen==null?lastSeenAt-watchSeconds*1000L:suppliedFirstSeen;
                    // Never accept a client interval wider than the declared
                    // qualified viewing duration or longer than six hours.
                    long safeDuration=Math.min(Math.max(watchSeconds,300L),21600L)*1000L;
                    firstSeenAt=Math.max(firstSeenAt,lastSeenAt-safeDuration);
                    row.put("firstSeenAt",new Date(firstSeenAt));row.put("lastSeenAt",new Date(lastSeenAt));
                    mapper.insertPresence(row);
                }
                if(n==0){duplicate++; continue;} accepted++;
                Map<String,Object> customerUpdate=new HashMap<>(); customerUpdate.put("id",customer.get("id")); customerUpdate.put("displayName",nickname); customerUpdate.put("stage",null); customerUpdate.put("interactionStatus", "COMMENT".equals(type) ? "HAS_COMMENT" : "HAS_INTERACTION"); customerUpdate.put("fanStatus", bool(event,"isFollower")!=null && bool(event,"isFollower") ? "FOLLOWED" : null); mapper.updateCustomer(customerUpdate);
            } catch(Exception ex){rejected++;}
        }
        batch.put("acceptedCount",accepted);batch.put("newCustomerCount",newCustomers);batch.put("updatedCustomerCount",updatedCustomers);batch.put("duplicateCount",duplicate);batch.put("rejectedCount",rejected);batch.put("status","ACCEPTED");mapper.updateBatch(batch);
        return Map.of("batchId",request.getBatchId(),"accepted",accepted,"duplicate",duplicate,"rejected",rejected);
    }

    @Transactional
    public Map<String,Object> register(LiveCrmRegistrationRequest req, Long userId, String username){
        String stage = normalizeStage(req.getStage());
        String interactionType = normalizeInteractionType(req.getInteractionType());
        String addFanStatus = normalizeFanStatus(req.getAddFanStatus());
        req.setStage(stage);
        req.setInteractionType(interactionType);
        req.setAddFanStatus(addFanStatus);
        // A room selected in the center is only a query context.  Create the
        // day's session lazily when the operator actually saves a registration
        // (and keep accepting the existing sessionId-only payloads).
        if(req.getSessionId()==null && blankToNull(req.getRoomName())!=null){
            LiveCrmSessionRequest sessionRequest=new LiveCrmSessionRequest();
            sessionRequest.setRoomName(req.getRoomName());
            sessionRequest.setPlatformRoomId(req.getPlatformRoomId());
            sessionRequest.setLiveDate(req.getLiveDate());
            Map<String,Object> session=ensureSession(sessionRequest,userId);
            Object sessionValue=session.get("sessionId");
            if(sessionValue==null) sessionValue=session.get("id");
            if(sessionValue!=null) req.setSessionId(((Number)sessionValue).longValue());
        }
        Map<String,Object> customer=resolveCustomer(req.getNickname(),req.getSecUid(),"MANUAL_ROOM");
        Long customerId=((Number)customer.get("id")).longValue();
        Map<String,Object> owner=req.getOwnerUserId()==null?null:mapper.findEnabledUser(req.getOwnerUserId());
        Long ownerUserId=owner==null?userId:((Number)owner.get("userId")).longValue();
        String ownerName=owner==null?username:String.valueOf(owner.get("userName"));
        Map<String,Object> staff=resolveStaff(req.getSessionId(),req.getAnchorUserId(),req.getControllerUserId());
        Map<String,Object> row=new HashMap<>(); row.put("sessionId",req.getSessionId());row.put("customerId",customerId);row.put("interactionType",interactionType);row.put("stage",stage);row.put("consultModel",blankToNull(req.getConsultModel()));row.put("concern",blankToNull(req.getConcern()));row.put("specialMark",blankToNull(req.getSpecialMark()));row.put("addFanStatus",addFanStatus);row.put("note",blankToNull(req.getNote()));row.put("orderNo",blankToNull(req.getOrderNo()));row.put("registeredBy",userId);row.put("registeredName",username);mapper.insertRegistration(row);
        Map<String,Object> customerUpdate=new HashMap<>();customerUpdate.put("id",customerId);customerUpdate.put("douyinAccount",blankToNull(req.getDouyinAccount()));customerUpdate.put("phone",blankToNull(req.getPhone()));customerUpdate.put("wechat",blankToNull(req.getWechat()));customerUpdate.put("stage",stage);customerUpdate.put("interactionStatus","BUY_INTENT".equals(interactionType)?"BUY_INTENT":"HAS_COMMENT");customerUpdate.put("fanStatus","UNKNOWN".equals(addFanStatus)?null:addFanStatus);customerUpdate.put("ownerUserId",ownerUserId);customerUpdate.put("ownerName",ownerName);mapper.updateCustomer(customerUpdate);
        Map<String,Object> opportunity=mapper.findOpenOpportunity(customerId,req.getSessionId());
        Map<String,Object> opportunityValue=new HashMap<>();opportunityValue.put("customerId",customerId);opportunityValue.put("sessionId",req.getSessionId());opportunityValue.put("interactionStatus",customerUpdate.get("interactionStatus"));opportunityValue.put("fanStatus",addFanStatus);opportunityValue.put("stage",stage);opportunityValue.put("consultModel",blankToNull(req.getConsultModel()));opportunityValue.put("concern",blankToNull(req.getConcern()));opportunityValue.put("specialMark",blankToNull(req.getSpecialMark()));opportunityValue.put("ownerUserId",ownerUserId);opportunityValue.put("ownerName",ownerName);opportunityValue.putAll(staff);
        // The first follow-up belongs to the account that actually saved it.
        // Room staffing remains separate attribution data.
        opportunityValue.put("controllerFirstFollowupUserId",userId);
        opportunityValue.put("controllerFirstFollowupName",username);
        if(opportunity==null){
            mapper.insertOpportunity(opportunityValue);
            opportunity=opportunityValue;
            if("INVALID".equals(stage)) {
                opportunityValue.put("id",opportunity.get("id"));
                opportunityValue.put("status","COMPLETED");
                mapper.updateOpportunity(opportunityValue);
            }
        }else{
            opportunityValue.put("id",opportunity.get("id"));
            opportunityValue.put("status","INVALID".equals(stage) ? "COMPLETED" : ("ORDERED".equals(stage) ? "IN_PROGRESS" : null));
            mapper.updateOpportunity(opportunityValue);
            opportunity=opportunityValue;
        }
        Long opportunityId=((Number)opportunity.get("id")).longValue();
        boolean terminalRegistration="ORDERED".equalsIgnoreCase(stage)
                || "INVALID".equalsIgnoreCase(stage)
                || blankToNull(req.getOrderNo())!=null;
        Map<String,Object> openTask=mapper.findOpenTask(opportunityId);
        if(terminalRegistration){
            // A customer marked ordered/invalid no longer belongs in the
            // live follow-up queue.  Keep the task history but close the open
            // task so it does not reappear in Today's work.
            if(openTask!=null && !"PHONE_SERVICE".equals(openTask.get("taskType"))){
                Map<String,Object> closeTask=new HashMap<>();
                closeTask.put("id",openTask.get("id")); closeTask.put("status","DONE");
                 closeTask.put("result","ORDERED".equalsIgnoreCase(stage) || blankToNull(req.getOrderNo())!=null ? "已下单" : "已标记无效");
                closeTask.put("completedAt",new Date()); closeTask.put("completedBy",userId); closeTask.put("completedName",username);
                mapper.updateTask(closeTask);
            }
        } else {
            String requestedTaskDate=blankToNull(req.getNextTaskDate());
            String taskDate=requestedTaskDate==null?LocalDate.now().toString():requestedTaskDate;
            try { LocalDate.parse(taskDate); } catch(Exception ex) { throw new ServiceException("下次跟进日期格式不正确"); }
            if(openTask==null){
                Map<String,Object> task=new HashMap<>();task.put("opportunityId",opportunityId);task.put("taskDate",taskDate);mapper.insertTask(task);
            } else if(requestedTaskDate!=null) {
                mapper.updateTaskDate(((Number)openTask.get("id")).longValue(),taskDate);
            }
        }
        if("ORDERED".equals(stage) || blankToNull(req.getOrderNo())!=null){
            Map<String,Object> order=mapper.findOrderByOpportunity(opportunityId);
            String orderNo=blankToNull(req.getOrderNo());
            String orderStatus=orderNo==null?"ORDERED_PENDING_NO":"ENTERED_PENDING_SYNC";
            // Do not downgrade an order that already has a number/status when
            // a later registration only updates the customer stage.
            if(order!=null && orderNo==null && order.get("orderNo")!=null){
                orderStatus=String.valueOf(order.get("orderStatus"));
            }
            Map<String,Object> orderValue=new HashMap<>();
            orderValue.put("opportunityId",opportunityId);orderValue.put("customerId",customerId);
            orderValue.put("orderNo",orderNo);orderValue.put("orderStatus",orderStatus);
            orderValue.put("productModel",blankToNull(req.getConsultModel()));
            if(order==null) mapper.insertOrder(orderValue);
            else {orderValue.put("id",order.get("id"));mapper.updateOrder(orderValue);}
        }
        Map<String,Object> result=new HashMap<>(customer);result.put("registrationId",row.get("id"));result.put("opportunityId",opportunityId); return result;
    }

    @Transactional
    public Map<String,Object> actOnTask(Long id, LiveCrmTaskActionRequest req, Long userId, String username){
        Map<String,Object> task=mapper.getTask(id); if(task==null) throw new ServiceException("跟进任务不存在");
        String action=req.getAction().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("CLAIM","COMPLETE","NEXT","RELEASE").contains(action)) throw new ServiceException("不支持的任务操作");
        Map<String,Object> update=new HashMap<>();update.put("id",id);update.put("result",blankToNull(req.getResult()));
        update.put("status","CLAIM".equals(action)?"IN_PROGRESS":"RELEASE".equals(action)?"RELEASED":"DONE");
        if(!"CLAIM".equals(action)){update.put("completedAt",new Date());update.put("completedBy",userId);update.put("completedName",username);}
        mapper.updateTask(update);
        Long opportunityId=((Number)task.get("opportunityId")).longValue();
        boolean phoneTask="PHONE_SERVICE".equals(task.get("taskType"));
        Map<String,Object> opportunity=new HashMap<>();opportunity.put("id",opportunityId);
        if("CLAIM".equals(action)){
            if(phoneTask){ update.put("assigneeUserId",userId); update.put("assigneeName",username); mapper.updateTask(update); }
            else { opportunity.put("ownerUserId",userId);opportunity.put("ownerName",username);opportunity.put("status","IN_PROGRESS");mapper.updateOpportunity(opportunity);mapper.updateCustomerOwnerByOpportunity(opportunityId,userId,username); }
        }
        if("RELEASE".equals(action) && !phoneTask){opportunity.put("status","RELEASED");mapper.updateOpportunity(opportunity);}
        if("NEXT".equals(action)){
            String date=blankToNull(req.getNextTaskDate()); if(date==null) throw new ServiceException("请选择下次跟进日期");
            try{LocalDate.parse(date);}catch(Exception ex){throw new ServiceException("下次跟进日期格式不正确");}
            Map<String,Object> next=new HashMap<>();next.put("opportunityId",opportunityId);next.put("taskDate",date);
            if(phoneTask){ next.put("handoffReason",task.get("handoffReason")); mapper.insertPhoneTask(next); }
            else mapper.insertTask(next);
            task.put("nextTaskId",next.get("id"));
        }
        task.put("action",action);return task;
    }

    @Transactional
    public Map<String,Object> updateOrder(Long id, LiveCrmOrderUpdateRequest req){
        Map<String,Object> order=mapper.getOrder(id); if(order==null) throw new ServiceException("订单记录不存在");
        String status=req.getOrderStatus().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("ORDERED_PENDING_NO","ENTERED_PENDING_SYNC","PROCESSING","SHIPPED","COMPLETED","REFUNDED","CLOSED").contains(status)) throw new ServiceException("订单状态不正确");
        order.put("orderStatus",status);order.put("orderNo",blankToNull(req.getOrderNo()));order.put("productModel",blankToNull(req.getProductModel()));order.put("payAmount",req.getPayAmount());mapper.updateOrder(order);
        if("COMPLETED".equals(status)||"CLOSED".equals(status)||"REFUNDED".equals(status)){Map<String,Object> opportunity=new HashMap<>();opportunity.put("id",order.get("opportunityId"));opportunity.put("status","COMPLETED");mapper.updateOpportunity(opportunity);}
        return mapper.getOrder(id);
    }

    public Map<String,Object> customerDetail(Long id){
        Map<String,Object> customer=mapper.getCustomer(id);if(customer==null)throw new ServiceException("客户不存在");
        customer.put("identities",mapper.listCustomerIdentities(id));customer.put("interactions",mapper.listCustomerInteractions(id));customer.put("registrations",mapper.listCustomerRegistrations(id));customer.put("opportunities",mapper.listCustomerOpportunities(id));customer.put("orders",mapper.listCustomerOrders(id));customer.put("attachments",mapper.listCustomerAttachments(id));customer.put("tasks",mapper.listCustomerTasks(id));return customer;
    }

    public Map<String,Object> batchDetail(Long id){Map<String,Object> batch=mapper.getBatch(id);if(batch==null)throw new ServiceException("上传批次不存在");batch.put("events",mapper.listBatchEvents(id));return batch;}

    private Map<String,Object> resolveStaff(Long sessionId,Long anchorUserId,Long controllerUserId){
        Map<String,Object> result=new HashMap<>();if(sessionId==null)return result;
        Map<String,Object> session=mapper.getSession(sessionId);if(session==null)throw new ServiceException("直播场次不存在");
        List<Map<String,Object>> mappings=mapper.listRoomStaff(String.valueOf(session.get("roomNameKey")));
        putStaff(result,mappings,"anchor",anchorUserId,"anchorUserId","anchorName","主播");
        putStaff(result,mappings,"controller",controllerUserId,"controllerUserId","controllerName","场控");
        return result;
    }

    private void putStaff(Map<String,Object> result,List<Map<String,Object>> mappings,String role,Long requested,String idKey,String nameKey,String label){
        List<Map<String,Object>> candidates=mappings.stream().filter(item->role.equals(item.get("roleCode"))).toList();
        Map<String,Object> selected=requested==null?null:candidates.stream().filter(item->requested.equals(((Number)item.get("userId")).longValue())).findFirst().orElse(null);
        if(requested!=null&&selected==null)throw new ServiceException("所选"+label+"不属于当前直播间");
        if(selected==null&&!candidates.isEmpty())selected=candidates.get(0);
        if(selected!=null){
            result.put(idKey,selected.get("userId"));
            if(requested==null&&candidates.size()>1){
                StringJoiner names=new StringJoiner("、");
                for(Map<String,Object> candidate:candidates){if(candidate.get("userName")!=null)names.add(String.valueOf(candidate.get("userName")));}
                result.put(nameKey,names.toString());
            }else result.put(nameKey,selected.get("userName"));
        }
    }

    private Map<String,Object> resolveCustomer(String nickname,String secUid,String source){
        String name=nickname.trim(), key=normalize(name);
        // A platform UID is authoritative. Do not merge different accounts
        // merely because their display names are identical.
        boolean hasPlatformUid=secUid!=null&&!secUid.isBlank();
        Map<String,Object> c=hasPlatformUid?mapper.findCustomerByUid(secUid):mapper.findCustomerByNickname(key);
        if(c==null){c=new HashMap<>();c.put("displayName",name);c.put("nicknameKey",key);mapper.insertCustomer(c);c.put("_created",true);}
        Long customerId=((Number)c.get("id")).longValue();
        String identityUid=secUid==null||secUid.isBlank()?null:secUid.trim();
        Map<String,Object> existingIdentity=mapper.findIdentity(customerId,identityUid,key);
        if(existingIdentity==null){Map<String,Object> identity=new HashMap<>();identity.put("customerId",customerId);identity.put("secUid",identityUid);identity.put("nickname",name);identity.put("nicknameKey",key);identity.put("sourceChannel",source);identity.put("sameNameMultiIdentity",0);mapper.insertIdentity(identity);}else{existingIdentity.put("nickname",name);mapper.touchIdentity(existingIdentity);}
        if(mapper.countDistinctIdentityUids(customerId)>1) mapper.markSameNameMultiIdentity(customerId);
        return c;
    }
    public List<Map<String,Object>> listInbox(String keyword,String stage,Long sessionId,String filter,String interactionType,String beginDate,String endDate,int page,int size){
        int safePage=Math.max(page,1), safeSize=Math.min(Math.max(size,1),200);
        String safeFilter=blankToNull(filter)==null?null:filter.trim().toLowerCase(Locale.ROOT);
        String safeInteraction=blankToNull(interactionType)==null?null:interactionType.trim().toUpperCase(Locale.ROOT);
        if (!"VIEWER".equals(safeInteraction) && !"COMMENT".equals(safeInteraction)) safeInteraction=null;
        String[] dates=inboxDateWindow(beginDate,endDate);
        return mapper.listInbox(blankToNull(keyword),blankToNull(stage),sessionId,safeFilter,safeInteraction,dates[0],dates[1],(safePage-1)*safeSize,safeSize);
    }
    public long countInbox(String keyword,String stage,Long sessionId,String filter,String interactionType,String beginDate,String endDate){
        String safeFilter=blankToNull(filter)==null?null:filter.trim().toLowerCase(Locale.ROOT);
        String safeInteraction=blankToNull(interactionType)==null?null:interactionType.trim().toUpperCase(Locale.ROOT);
        if (!"VIEWER".equals(safeInteraction) && !"COMMENT".equals(safeInteraction)) safeInteraction=null;
        String[] dates=inboxDateWindow(beginDate,endDate);
        return mapper.countInbox(blankToNull(keyword),blankToNull(stage),sessionId,safeFilter,safeInteraction,dates[0],dates[1]);
    }
    public Map<String,Object> inboxSummary(Long sessionId, String interactionType, String beginDate, String endDate){
        String safeInteraction=blankToNull(interactionType)==null?null:interactionType.trim().toUpperCase(Locale.ROOT);
        if (!"VIEWER".equals(safeInteraction) && !"COMMENT".equals(safeInteraction)) safeInteraction=null;
        String[] dates=inboxDateWindow(beginDate,endDate);
        String safeBeginDate=dates[0], safeEndDate=dates[1];
        InboxSummaryKey cacheKey=new InboxSummaryKey(sessionId,safeInteraction,safeBeginDate,safeEndDate);
        long now=System.currentTimeMillis();
        InboxSummaryCacheEntry cached=inboxSummaryCache.get(cacheKey);
        if(cached!=null && cached.expiresAt()>now) return new LinkedHashMap<>(cached.value());
        synchronized(inboxSummaryCache) {
            now=System.currentTimeMillis();
            cached=inboxSummaryCache.get(cacheKey);
            if(cached!=null && cached.expiresAt()>now) return new LinkedHashMap<>(cached.value());
            Map<String,Object> value=new LinkedHashMap<>();
            try {
                value.put("pending", mapper.countInbox(null,null,sessionId,null,safeInteraction,safeBeginDate,safeEndDate));
            } catch (RuntimeException ex) {
                log.warn("Inbox pending summary query failed; returning a partial summary", ex);
                value.put("pending",0);
            }
            try {
                Map<String,Object> workflowSummary=mapper.inboxSummary(sessionId, safeInteraction, safeBeginDate, safeEndDate);
                if(workflowSummary!=null) value.putAll(workflowSummary);
            } catch (RuntimeException ex) {
                log.warn("Inbox workflow summary query failed; returning available counters", ex);
            }
            value.putIfAbsent("pending",0); value.putIfAbsent("today",0); value.putIfAbsent("overdue",0);
            value.putIfAbsent("consulting",0); value.putIfAbsent("orderPending",0); value.putIfAbsent("phoneHandoff",0);
            Map<String,Object> cachedValue=new LinkedHashMap<>(value);
            if(inboxSummaryCache.size()>=128) inboxSummaryCache.clear();
            inboxSummaryCache.put(cacheKey,new InboxSummaryCacheEntry(now+INBOX_SUMMARY_CACHE_MILLIS,cachedValue));
            return new LinkedHashMap<>(cachedValue);
        }
    }
    public Map<String,Object> workbenchSummary(Long sessionId){Map<String,Object> value=mapper.workbenchSummary(sessionId); return value==null?Map.of("interactions",0,"unregistered",0,"followed",0,"consulting",0):value;}
    /** Move eligible, non-converted opportunities to a separate phone-service task. */
    @Transactional
    public int processPhoneHandoffs(){
        int count=0;
        for(Map<String,Object> candidate: mapper.listPhoneHandoffCandidates()){
            Long opportunityId=((Number)candidate.get("opportunityId")).longValue();
            if(mapper.findOpenPhoneTask(opportunityId)!=null) continue;
            Map<String,Object> handoff=new HashMap<>();
            handoff.put("id",opportunityId);
            handoff.put("reason",candidate.get("handoffReason"));
            // The status update is the concurrency gate.  A scheduled run
            // and a manual run may inspect the same candidate at the same
            // time; only the run that changes the opportunity may create the
            // phone-service task.
            if (mapper.markOpportunityHandoff(handoff) == 0) continue;
            Map<String,Object> task=new HashMap<>();
            task.put("opportunityId",opportunityId);
            task.put("taskDate",LocalDate.now().toString());
            task.put("handoffReason",candidate.get("handoffReason"));
            mapper.insertPhoneTask(task);
            count++;
        }
        return count;
    }

    public List<Map<String,Object>> listOrders(int page,int size){int safePage=Math.max(page,1),safeSize=Math.min(Math.max(size,1),200);return mapper.listOrders((safePage-1)*safeSize,safeSize);}
    public long countOrders(){return mapper.countOrders();}
    public Map<String,Object> handshake(String version){return Map.of("edition","LIVE_CUSTOMER_V2","protocolVersion",2,"pluginVersion",version==null?"":version,"minPluginVersion",minimumPluginVersion,"supported",isPluginVersionSupported(version));}
    public Map<String,Object> versionInfo(){Map<String,Object> value=new LinkedHashMap<>();value.put("edition","LIVE_CUSTOMER_V2");value.put("protocolVersion",2);value.put("latestVersion",latestPluginVersion);value.put("minVersion",minimumPluginVersion);value.put("updateUrl",pluginUpdateUrl==null?"":pluginUpdateUrl);value.put("sha256",pluginUpdateSha256==null?"":pluginUpdateSha256);return value;}
    public boolean isPluginVersionSupported(String version){return compareVersions(version,minimumPluginVersion)>=0;}
    public List<Map<String,Object>> listWorkbench(String keyword,String stage,Boolean claimedOnly,Long ownerUserId,Long sessionId,String beginDate,String endDate,int page,int size){int safePage=Math.max(page,1),safeSize=Math.min(Math.max(size,1),200);return mapper.listWorkbench(blankToNull(keyword),blankToNull(stage),claimedOnly,ownerUserId,sessionId,validDate(beginDate),validDate(endDate),(safePage-1)*safeSize,safeSize);}
    public long countWorkbench(String keyword,String stage,Boolean claimedOnly,Long ownerUserId,Long sessionId,String beginDate,String endDate){return mapper.countWorkbench(blankToNull(keyword),blankToNull(stage),claimedOnly,ownerUserId,sessionId,validDate(beginDate),validDate(endDate));}
    private static String validDate(String value){if(value==null||value.isBlank())return null;try{return LocalDate.parse(value.trim()).toString();}catch(Exception ignored){return null;}}
    private static String[] inboxDateWindow(String beginDate,String endDate){
        LocalDate today=LocalDate.now();
        LocalDate oldest=today.minusDays(1);
        LocalDate begin=parseDate(beginDate,today);
        LocalDate end=parseDate(endDate,today);
        if(begin.isBefore(oldest)) begin=oldest;
        if(begin.isAfter(today)) begin=today;
        if(end.isBefore(oldest)) end=oldest;
        if(end.isAfter(today)) end=today;
        if(begin.isAfter(end)) begin=end;
        return new String[]{begin.toString(),end.toString()};
    }
    private static LocalDate parseDate(String value,LocalDate fallback){
        String valid=validDate(value);
        return valid==null?fallback:LocalDate.parse(valid);
    }
    public List<Map<String,Object>> listBatches(int page,int size){int safePage=Math.max(page,1),safeSize=Math.min(Math.max(size,1),200);return mapper.listBatches((safePage-1)*safeSize,safeSize);}
    public long countBatches(){return mapper.countBatches();}
    public List<Map<String,Object>> listTasks(int page,int size){int safePage=Math.max(page,1),safeSize=Math.min(Math.max(size,1),200);return mapper.listTasks((safePage-1)*safeSize,safeSize);}
    public long countTasks(){return mapper.countTasks();}
    private static String normalize(String value){
        if(value==null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
    }
    private static String string(Map<String,Object> m,String k){Object v=m.get(k);return v==null||String.valueOf(v).isBlank()?null:String.valueOf(v);}
    private static String firstString(Map<String,Object> m,String... keys){for(String key:keys){String value=string(m,key);if(value!=null)return value;}return null;}
    private static long number(Map<String,Object> m,String k){
        long value;
        Object v=m.get(k);
        if(v instanceof Number) value=((Number)v).longValue();
        else { try { value=Long.parseLong(String.valueOf(v)); } catch(Exception e) { value=System.currentTimeMillis(); } }
        // Browser timestamps are normally milliseconds. Accept Unix seconds
        // as well so a malformed client event cannot be placed in 1970.
        return value > 0 && value < 100_000_000_000L ? value * 1000L : value;
    }
    private static Long optionalNumber(Map<String,Object> m,String k){
        Object value=m.get(k); if(value==null)return null;
        try{return value instanceof Number?((Number)value).longValue():Long.parseLong(String.valueOf(value));}
        catch(Exception ignored){return null;}
    }
    private static Boolean bool(Map<String,Object> m,String k){Object v=m.get(k);return v==null?null:Boolean.valueOf(String.valueOf(v));}
    private static String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String normalizeStage(String value){
        String stage=blankToNull(value);
        if (stage == null) throw new ServiceException("请选择客户阶段");
        stage=stage.toUpperCase(Locale.ROOT);
        if ("UNCONTACTED".equals(stage)) stage="NOT_CONSULTED";
        if (!REGISTRATION_STAGES.contains(stage)) throw new ServiceException("客户阶段不正确");
        return stage;
    }
    private static String normalizeInteractionType(String value){
        String type=blankToNull(value);
        if (type == null) type="COMMENT";
        type=type.toUpperCase(Locale.ROOT);
        if (!INTERACTION_TYPES.contains(type)) throw new ServiceException("互动类型不正确");
        return type;
    }
    private static String normalizeFanStatus(String value){
        String status=blankToNull(value);
        if (status == null) return "UNKNOWN";
        status=status.toUpperCase(Locale.ROOT);
        if (!FAN_STATUSES.contains(status)) throw new ServiceException("加粉关系不正确");
        return status;
    }
    private static String normalizeLiveDate(String value){
        if(value==null || value.isBlank()) return LocalDate.now().toString();
        String normalized=value.trim().replace('.','-').replace('/','-');
        try{return LocalDate.parse(normalized).toString();}
        catch(Exception ex){throw new ServiceException("直播日期格式不正确，应为 YYYY-MM-DD");}
    }
    private static int compareVersions(String left,String right){String[] a=(left==null?"":left).split("\\.");String[] b=(right==null?"":right).split("\\.");for(int i=0;i<Math.max(a.length,b.length);i++){int x=versionPart(a,i),y=versionPart(b,i);if(x!=y)return Integer.compare(x,y);}return 0;}
    private static int versionPart(String[] value,int index){if(index>=value.length)return 0;try{return Integer.parseInt(value[index].replaceAll("[^0-9].*$",""));}catch(Exception ignored){return 0;}}

    private static boolean isDuplicateKey(Throwable error){
        for(Throwable current=error; current!=null; current=current.getCause()){
            if(current instanceof java.sql.SQLException sql && (sql.getErrorCode()==1062 || "23000".equals(sql.getSQLState()))) return true;
            String name=current.getClass().getName();
            if(name.endsWith("DuplicateKeyException") || name.endsWith("DuplicateEntryException")) return true;
        }
        return false;
    }
}
