package com.xinke.erp.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.LiveMetricsSubmissionRequest;
import com.xinke.erp.event.LiveMetricsSubmittedEvent;
import com.xinke.erp.mapper.LiveMetricsMapper;

@Service
public class LiveMetricsReportingService
{
    private static final List<String> TOTALS=List.of("exposureTotal","watchTotal","dealTotal","newFanTotal","interactionTotal");
    private static final List<String> DELTAS=List.of("exposureDelta","watchDelta","dealDelta","newFanDelta","interactionDelta");
    private final LiveMetricsMapper mapper;
    private final ApplicationEventPublisher events;
    public LiveMetricsReportingService(LiveMetricsMapper mapper,ApplicationEventPublisher events){this.mapper=mapper;this.events=events;}

    public List<Map<String,Object>> list(Map<String,Object> query){return mapper.listReportingRows(query);}

    public List<Map<String,Object>> listSessionShifts(Long sessionId)
    {
        requiredSession(sessionId);
        return mapper.listSessionShifts(sessionId);
    }

    @Transactional
    public Map<String,Object> submit(LiveMetricsSubmissionRequest request)
    {
        Map<String,Object> duplicate=mapper.getSubmissionByKey(request.getIdempotencyKey());
        if(duplicate!=null){Map<String,Object> session=requiredSession(id(duplicate.get("sessionId")));return response(duplicate,session,duplicate);}
        String roomName=normalizeName(request.getRoomName()),roomKey=key(roomName);if(roomName.isBlank())throw new ServiceException("没有识别到直播间名称");
        LocalDateTime liveStart=LiveMetricsService.parse(request.getLiveStartTime()),now=LocalDateTime.now();
        if(liveStart.isAfter(now.plusMinutes(5)))throw new ServiceException("直播开始时间晚于当前时间，请刷新页面后重试");
        LocalDateTime snapshotAt=resolveSnapshotAt(request.getCapturedAt(),liveStart,now);
        Map<String,Object> room=mapper.findRoomByAliasKey(roomKey),session=findOrCreateSession(room,roomName,roomKey,liveStart);
        ensureSubmissionCapacity(id(session.get("sessionId")));
        Long roomId=id(session.get("roomId")),shiftId=null;
        if(roomId!=null&&Duration.between(liveStart,now).toHours()<=36){Map<String,Object> shift=autoMatchShift(id(session.get("sessionId")),roomId,liveStart,snapshotAt);if(shift!=null)shiftId=id(shift.get("shiftId"));}
        Map<String,Object> value=new HashMap<>();value.put("sessionId",session.get("sessionId"));value.put("shiftId",shiftId);value.put("reportedRoomName",roomName);value.put("snapshotAt",LiveMetricsService.db(snapshotAt));value.put("valueSource","PLUGIN");value.put("matchStatus",roomId==null?"PENDING_ROOM":shiftId==null?"PENDING_SHIFT":"MATCHED");value.put("idempotencyKey",request.getIdempotencyKey());value.put("submittedName","plugin");value.put("remark","");copyMetrics(request,value);mapper.insertRawSubmission(value);
        Map<String,Object> calculated=recalculate(id(session.get("sessionId")),id(value.get("submissionId")));
        publishSubmitted(calculated);
        return response(value,session,calculated);
    }

    @Transactional
    public Map<String,Object> supplement(Map<String,Object> request,Long userId,String userName)
    {
        Long sessionId=LiveMetricsService.longValue(request.get("sessionId"),"直播场次"),shiftId=LiveMetricsService.longValue(request.get("shiftId"),"漏报班次");
        Map<String,Object> session=requiredSession(sessionId),shift=mapper.getShift(shiftId);ensureSubmissionCapacity(sessionId);if(shift==null)throw new ServiceException("班次不存在或已删除");if(!Objects.equals(id(session.get("roomId")),id(shift.get("roomId"))))throw new ServiceException("所选班次不属于该直播间");
        String reason=Objects.toString(request.get("reason"),"").trim();if(reason.isBlank())throw new ServiceException("请填写补录原因");
        Map<String,Object> value=new HashMap<>();value.put("sessionId",sessionId);value.put("shiftId",shiftId);value.put("reportedRoomName",session.get("reportedRoomName"));value.put("snapshotAt",Objects.toString(request.get("snapshotAt"),Objects.toString(shift.get("endTime"),"")));value.put("valueSource","MANUAL_CUMULATIVE");value.put("matchStatus","MATCHED");value.put("idempotencyKey","manual-"+UUID.randomUUID());value.put("submittedBy",userId);value.put("submittedName",userName);value.put("remark",reason);copyMetrics(request,value);mapper.insertRawSubmission(value);audit(id(value.get("submissionId")),sessionId,"SUPPLEMENT",null,"shiftId="+shiftId,reason,userId,userName);Map<String,Object> calculated=recalculate(sessionId,id(value.get("submissionId")));publishSubmitted(calculated);return calculated==null?value:calculated;
    }

    @Transactional
    public void rematch(Map<String,Object> request,Long userId,String userName)
    {
        Long submissionId=LiveMetricsService.longValue(request.get("submissionId"),"上报记录"),shiftId=LiveMetricsService.longValue(request.get("shiftId"),"班次");String reason=Objects.toString(request.get("reason"),"").trim();if(reason.isBlank())throw new ServiceException("请填写调整原因");
        Map<String,Object> old=mapper.getSubmission(submissionId),shift=mapper.getShift(shiftId);if(old==null||shift==null)throw new ServiceException("上报记录或班次不存在");Map<String,Object> session=requiredSession(id(old.get("sessionId")));if(!Objects.equals(id(session.get("roomId")),id(shift.get("roomId"))))throw new ServiceException("所选班次不属于该直播间");
        Long oldShiftId=id(old.get("shiftId"));mapper.updateSubmissionShift(Map.of("submissionId",submissionId,"shiftId",shiftId,"remark",reason));audit(submissionId,id(old.get("sessionId")),"REMATCH",Objects.toString(old.get("shiftId"),""),String.valueOf(shiftId),reason,userId,userName);Map<String,Object> calculated=recalculate(id(old.get("sessionId")),submissionId);if(oldShiftId!=null&&!Objects.equals(oldShiftId,shiftId))mapper.refreshShiftStatus(oldShiftId);publishSubmitted(calculated);
    }

    @Transactional
    public void bindRoom(Map<String,Object> request,Long userId,String userName)
    {
        Long sessionId=LiveMetricsService.longValue(request.get("sessionId"),"直播场次"),roomId=LiveMetricsService.longValue(request.get("roomId"),"直播间");Map<String,Object> session=requiredSession(sessionId);String alias=Objects.toString(session.get("reportedRoomName"),""),aliasKey=key(alias);
        try{mapper.insertRoomAlias(Map.of("roomId",roomId,"aliasName",alias,"aliasKey",aliasKey,"isCurrent",0,"createBy",userName));}catch(DuplicateKeyException ex){Map<String,Object> existing=mapper.findRoomByAliasKey(aliasKey);if(existing==null||!Objects.equals(id(existing.get("roomId")),roomId))throw new ServiceException("这个直播间名称已经属于另一个直播间");}
        mapper.bindLiveSessionRoom(Map.of("sessionId",sessionId,"roomId",roomId));
        LocalDateTime liveStart=LiveMetricsService.parse(session.get("liveStartTime"));for(Map<String,Object> raw:mapper.listRawSessionSubmissions(sessionId)){if(raw.get("shiftId")!=null)continue;LocalDateTime snapshotAt=LiveMetricsService.parse(raw.get("snapshotAt"));Map<String,Object> shift=autoMatchShift(sessionId,roomId,liveStart,snapshotAt);if(shift!=null)mapper.updateSubmissionShift(Map.of("submissionId",raw.get("submissionId"),"shiftId",shift.get("shiftId"),"remark","认领直播间后自动匹配"));}
        audit(null,sessionId,"BIND_ROOM",Objects.toString(session.get("roomId"),""),String.valueOf(roomId),"认领直播间名称",userId,userName);recalculate(sessionId,null);
    }

    @Transactional
    public Map<String,Object> recalculate(Long sessionId,Long focusSubmissionId)
    {
        mapper.resetShiftSubmissionStatus(sessionId);mapper.resetSessionCalculation(sessionId);
        List<Map<String,Object>> all=mapper.listSessionSubmissions(sessionId),shifts=mapper.listSessionShifts(sessionId);Map<Long,Map<String,Object>> latest=new LinkedHashMap<>();
        for(Map<String,Object> row:all)latest.put(id(row.get("shiftId")),row);
        List<Map<String,Object>> selected=new ArrayList<>(latest.values());selected.sort(Comparator.comparing(row->LiveMetricsService.parse(row.get("startTime"))));Set<Long> selectedIds=new LinkedHashSet<>(latest.keySet());Set<Long> missingBefore=new LinkedHashSet<>();long[] base={0,0,0,0,0};Long baseline=null;Map<String,Object> focus=null;
        for(Map<String,Object> current:selected)
        {
            Long shiftId=id(current.get("shiftId"));for(Map<String,Object> shift:shifts){Long candidate=id(shift.get("shiftId"));if(Objects.equals(candidate,shiftId))break;if(!selectedIds.contains(candidate))missingBefore.add(candidate);}
            long[] values=metrics(current);boolean anomaly=false;for(int i=0;i<values.length;i++)if(values[i]<base[i])anomaly=true;
            String status=anomaly?"ANOMALY":missingBefore.isEmpty()?"MATCHED":"SUSPECT_MISSING";Map<String,Object> calc=new HashMap<>();calc.put("submissionId",current.get("submissionId"));calc.put("matchStatus",status);calc.put("baselineSubmissionId",baseline);
            for(int i=0;i<values.length;i++)calc.put(DELTAS.get(i),anomaly?null:values[i]-base[i]);mapper.updateSubmissionCalculation(calc);if(!anomaly){base=values;baseline=id(current.get("submissionId"));mapper.markShiftSubmitted(shiftId);}current.putAll(calc);if(Objects.equals(id(current.get("submissionId")),focusSubmissionId))focus=current;
        }
        return focus;
    }

    private Map<String,Object> findOrCreateSession(Map<String,Object> room,String roomName,String roomKey,LocalDateTime liveStart)
    {
        String start=LiveMetricsService.db(liveStart);Long roomId=room==null?null:id(room.get("roomId"));Map<String,Object> session=findSession(roomId,roomKey,start);if(session!=null)return session;Map<String,Object> value=new HashMap<>();value.put("roomId",roomId);value.put("reportedRoomName",roomName);value.put("roomNameKey",roomKey);value.put("liveStartTime",start);value.put("matchStatus",room==null?"PENDING_ROOM":"MATCHED");
        try{mapper.insertLiveSession(value);return value;}catch(DuplicateKeyException ex){session=findSession(roomId,roomKey,start);if(session!=null)return session;throw ex;}
    }
    private Map<String,Object> findSession(Long roomId,String roomKey,String start){if(roomId!=null){Map<String,Object> byRoom=mapper.findLiveSessionByRoom(roomId,start);if(byRoom!=null)return byRoom;}return mapper.findLiveSession(roomKey,start);}
    private Map<String,Object> requiredSession(Long id){Map<String,Object> session=mapper.getLiveSession(id);if(session==null)throw new ServiceException("直播场次不存在");return session;}
    private Map<String,Object> autoMatchShift(Long sessionId,Long roomId,LocalDateTime liveStart,LocalDateTime snapshotAt)
    {
        List<Map<String,Object>> candidates=mapper.findAutoMatchShifts(roomId,sessionId,LiveMetricsService.db(liveStart),LiveMetricsService.db(snapshotAt));
        List<Map<String,Object>> current=candidates.stream().filter(row->LiveMetricsService.number(row.get("currentShift"))==1).toList();
        if(current.size()==1)return current.get(0);if(current.size()>1)return null;
        List<Map<String,Object>> unreported=candidates.stream().filter(row->LiveMetricsService.number(row.get("hasSubmission"))==0).toList();
        if(unreported.size()==1)return unreported.get(0);if(unreported.size()>1)return null;
        return candidates.isEmpty()?null:candidates.get(0);
    }
    private LocalDateTime resolveSnapshotAt(Long capturedAt,LocalDateTime liveStart,LocalDateTime now)
    {
        if(capturedAt==null)return now;
        LocalDateTime value;
        try{value=LocalDateTime.ofInstant(Instant.ofEpochMilli(capturedAt),ZoneId.systemDefault());}
        catch(Exception ex){throw new ServiceException("采集时间格式不正确");}
        if(value.isAfter(now.plusMinutes(5)))throw new ServiceException("采集时间晚于当前时间，请重新读取数据");
        if(value.isBefore(liveStart.minusMinutes(5)))throw new ServiceException("采集时间早于直播开始时间，请重新读取数据");
        return value;
    }
    private void ensureSubmissionCapacity(Long sessionId){if(sessionId==null||mapper.countSessionSubmissions(sessionId)>=500)throw new ServiceException("本场直播上报记录过多，请联系管理员检查异常重试");}
    private void copyMetrics(Object source,Map<String,Object> target)
    {
        for(String field:TOTALS){Object raw=source instanceof LiveMetricsSubmissionRequest r?switch(field){case "exposureTotal"->r.getExposureTotal();case "watchTotal"->r.getWatchTotal();case "dealTotal"->r.getDealTotal();case "newFanTotal"->r.getNewFanTotal();default->r.getInteractionTotal();}:((Map<?,?>)source).get(field);long value=LiveMetricsService.number(raw);if(value<0)throw new ServiceException("经营数据不能小于0");target.put(field,value);}
    }
    private static long[] metrics(Map<String,Object> row){long[] result=new long[TOTALS.size()];for(int i=0;i<result.length;i++)result[i]=LiveMetricsService.number(row.get(TOTALS.get(i)));return result;}
    private Map<String,Object> response(Map<String,Object> value,Map<String,Object> session,Map<String,Object> calculated){Map<String,Object> result=new LinkedHashMap<>();result.put("submissionId",value.get("submissionId"));result.put("sessionId",session.get("sessionId"));result.put("roomMatched",session.get("roomId")!=null);result.put("shiftMatched",value.get("shiftId")!=null);result.put("matchStatus",calculated==null?value.get("matchStatus"):calculated.get("matchStatus"));if(value.get("shiftId")!=null){Map<String,Object> shift=mapper.getShift(id(value.get("shiftId")));result.put("shift",shift);}return result;}
    private void publishSubmitted(Map<String,Object> calculated)
    {
        if(calculated==null||calculated.get("exposureDelta")==null)return;
        String status=Objects.toString(calculated.get("matchStatus"),"");
        if(!Set.of("MATCHED","SUSPECT_MISSING").contains(status))return;
        Long shiftId=id(calculated.get("shiftId"));if(shiftId==null)return;
        calculated.put("revisionCount",mapper.countShiftSubmissions(shiftId));
        Map<String,Object> shift=mapper.getShift(shiftId);
        if(shift!=null)events.publishEvent(new LiveMetricsSubmittedEvent(shift,calculated));
    }
    private void audit(Long submissionId,Long sessionId,String action,String oldValue,String newValue,String reason,Long userId,String userName){Map<String,Object> v=new HashMap<>();v.put("submissionId",submissionId);v.put("sessionId",sessionId);v.put("actionType",action);v.put("oldValue",oldValue);v.put("newValue",newValue);v.put("reason",reason);v.put("correctedBy",userId);v.put("correctedName",userName);mapper.insertCorrection(v);}
    private static String normalizeName(Object value){return Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFKC).replaceAll("\\s+"," ").replaceFirst("\\s*(?:直播开始时间|开播时间)\\s*[:：]?\\s*20\\d{2}[/.-]\\d{1,2}[/.-]\\d{1,2}[\\s\\S]*$","").replaceFirst("\\s*已直播\\s*\\d+\\s*(?:天|小时|时|分钟|分|秒)[\\s\\S]*$","").trim();}
    private static String key(String value){return normalizeName(value).toLowerCase(Locale.ROOT);}
    private static Long id(Object value){if(value==null)return null;try{return Long.valueOf(value.toString());}catch(Exception ex){return null;}}
}
