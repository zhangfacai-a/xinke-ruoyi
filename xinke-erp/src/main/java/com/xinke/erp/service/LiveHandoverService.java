package com.xinke.erp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.alibaba.fastjson2.JSON;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.mapper.LiveMetricsMapper;

@Service
public class LiveHandoverService
{
    private static final DateTimeFormatter DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal MONEY_MAX = new BigDecimal("99999999999999.9999");
    private final LiveMetricsMapper mapper;
    private final ApplicationEventPublisher events;

    public LiveHandoverService(LiveMetricsMapper mapper,ApplicationEventPublisher events){this.mapper=mapper;this.events=events;}

    public Map<String,Object> context(String platformSessionId,String roomName)
    {
        Map<String,Object> session=mapper.findHandoverSessionByPlatformId(platformSessionId);
        Long roomId=session==null?resolveRoomId(roomName):id(session.get("roomId"));
        Map<String,Object> value=new LinkedHashMap<>();
        value.put("session",session);value.put("roomMatched",roomId!=null);value.put("roomId",roomId);
        value.put("anchors",roomId==null?List.of():mapper.listRoomAnchors(roomId));
        Map<String,Object> previous=session==null?null:mapper.getLatestHandoverSnapshot(id(session.get("sessionId")));
        value.put("previousSnapshot",previous);
        List<Map<String,Object>> segments=session==null?List.of():mapper.listHandoverSegments(id(session.get("sessionId")));
        value.put("lastReport",segments.isEmpty()?null:segments.get(segments.size()-1));
        value.put("segments",segments);
        return value;
    }

    @Transactional
    public Map<String,Object> transition(LiveHandoverRequest request)
    {
        return transitionInternal(request,"PLUGIN","plugin",null,null);
    }

    @Transactional
    public Map<String,Object> transitionApp(LiveHandoverRequest request,Long operatorId,String operator,String clientVersion)
    {
        return transitionInternal(request,"APP",operator,operatorId,clientVersion);
    }

    private Map<String,Object> transitionInternal(LiveHandoverRequest request,String source,String operator,Long operatorId,String clientVersion)
    {
        validateMetrics(request);
        Map<String,Object> duplicate=mapper.getHandoverSnapshotByKey(request.getIdempotencyKey());
        if(duplicate!=null)return state(id(duplicate.get("sessionId")),true);
        Map<String,Object> session=ensureSession(request);
        Long sessionId=id(session.get("sessionId")),roomId=id(session.get("roomId"));
        if(roomId==null)throw new ServiceException("没有匹配到系统直播间，请先在网站绑定直播间名称");
        session=mapper.lockHandoverSession(sessionId);
        duplicate=mapper.getHandoverSnapshotByKey(request.getIdempotencyKey());
        if(duplicate!=null)return state(id(duplicate.get("sessionId")),true);
        Map<String,Object> previous=mapper.getLatestHandoverSnapshot(sessionId);
        Long actualPrevious=previous==null?null:id(previous.get("snapshotId"));
        if(!Objects.equals(actualPrevious,request.getPreviousSnapshotId()))throw new ServiceException("本场刚刚产生了新的上报，请重新读取数据后再提交");
        validateAnchor(roomId,request.getAnchorUserId());
        Map<String,Object> baseline=previous;
        if(baseline==null)
        {
            baseline=zeroSnapshot(session,request);
            mapper.insertHandoverSnapshot(baseline);
        }
        if("APP".equals(source))
        {
            baseline=appBaseline(session,baseline);
            if(Boolean.TRUE.equals(baseline.get("estimatedBaseline")))
            {
                baseline.put("snapshotKind","MANUAL_BASELINE");baseline.put("sessionId",sessionId);baseline.put("source","SYSTEM");baseline.put("idempotencyKey","baseline-"+request.getIdempotencyKey());
                mapper.insertHandoverSnapshot(baseline);
            }
        }
        Map<String,Object> current=snapshot(request,sessionId,"REPORT");
        current.put("source",source);
        if("APP".equals(source))validateAppDelta(baseline,current);
        mapper.insertHandoverSnapshot(current);
        mapper.updateHandoverSessionSnapshot(sessionId,String.valueOf(current.get("capturedAt")));
        Map<String,Object> segment=calculation(baseline,current);
        segment.put("sessionId",sessionId);segment.put("roomId",roomId);segment.put("anchorUserId",request.getAnchorUserId());segment.put("startSnapshotId",id(baseline.get("snapshotId")));segment.put("endSnapshotId",id(current.get("snapshotId")));segment.put("startTime",baseline.get("capturedAt"));segment.put("endTime",current.get("capturedAt"));segment.put("precisionStatus","EXACT");segment.put("source","PLUGIN");segment.put("remark","");segment.put("createBy","plugin");
        segment.put("source",source);segment.put("createBy",operator);segment.put("operatorUserId",operatorId);segment.put("clientVersion",clientVersion);
        if("APP".equals(source))
        {
            segment.put("remark",Boolean.TRUE.equals(baseline.get("estimatedBaseline"))?"由已确认人工区间接续；互动人数为估算":"互动人数由累计观看人数和互动率估算");
            if(Boolean.TRUE.equals(baseline.get("estimatedBaseline")))segment.put("precisionStatus","MANUAL");
        }
        mapper.insertHandoverSegment(segment);
        correction(sessionId,id(segment.get("segmentId")),"REPORT",null,segment,"APP".equals(source)?"App上报":"插件上报",operatorId,operator);
        changed(id(segment.get("segmentId")),false);
        return state(sessionId,false);
    }

    public List<Map<String,Object>> rooms(){return mapper.listHandoverRooms();}

    public Map<String,Object> previewApp(LiveHandoverRequest request)
    {
        validateMetrics(request);
        Map<String,Object> context=context(request.getPlatformSessionId(),request.getRoomName());
        if(context.get("roomId")==null)throw new ServiceException("没有匹配到系统直播间，请先绑定名称");
        validateAnchor(id(context.get("roomId")),request.getAnchorUserId());
        @SuppressWarnings("unchecked") Map<String,Object> session=(Map<String,Object>)context.get("session");
        @SuppressWarnings("unchecked") Map<String,Object> previous=(Map<String,Object>)context.get("previousSnapshot");
        if(!Objects.equals(previous==null?null:id(previous.get("snapshotId")),request.getPreviousSnapshotId()))throw new ServiceException("已有新的上报，请重新采集并预览");
        if(session==null)session=new HashMap<>(Map.of("liveStartTime",db(parse(request.getLiveStartTime()))));
        Map<String,Object> baseline=appBaseline(session,previous==null?zeroSnapshot(session,request):previous);
        Map<String,Object> current=snapshot(request,null,"REPORT");
        validateAppDelta(baseline,current);
        Map<String,Object> result=new LinkedHashMap<>(calculation(baseline,current));
        result.put("startTime",baseline.get("capturedAt"));result.put("endTime",current.get("capturedAt"));
        result.put("firstReport",previous==null&&!Boolean.TRUE.equals(baseline.get("estimatedBaseline")));result.put("roomId",context.get("roomId"));
        result.put("baselineEstimated",Boolean.TRUE.equals(baseline.get("estimatedBaseline")));
        result.put("interactionEstimated",true);
        return result;
    }

    public Map<String,Object> appContext(String platformId,String roomName)
    {
        Map<String,Object> value=context(platformId,roomName);
        @SuppressWarnings("unchecked") Map<String,Object> session=(Map<String,Object>)value.get("session");
        @SuppressWarnings("unchecked") Map<String,Object> previous=(Map<String,Object>)value.get("previousSnapshot");
        if(session!=null){
            Map<String,Object> baseline=previous;
            if(baseline==null){baseline=new HashMap<>();baseline.put("capturedAt",session.get("liveStartTime"));for(String key:List.of("watchTotal","interactionTotal","newFanTotal","dealTotal","presaleOrderTotal","gmvTotal","adCostTotal","exposureWatchRate"))baseline.put(key,BigDecimal.ZERO);}
            value.put("effectiveBaseline",appBaseline(session,baseline));
        }
        return value;
    }

    private Map<String,Object> appBaseline(Map<String,Object> session,Map<String,Object> original)
    {
        if(session.get("sessionId")==null)return original;
        List<Map<String,Object>> segments=mapper.listHandoverSegments(id(session.get("sessionId")));
        if(segments==null||segments.isEmpty())return original;
        Map<String,Object> base=new HashMap<>(original);
        BigDecimal exposure=exposureTotal(base);
        boolean estimated=false;
        for(Map<String,Object> row:segments){
            if(!parse(row.get("endTime")).isAfter(parse(base.get("capturedAt"))))continue;
            if(!sameMinute(parse(row.get("startTime")),parse(base.get("capturedAt"))))throw new ServiceException("人工记录存在时间间隔，请管理员核对后再上报");
            if(row.get("endSnapshotId")!=null){Map<String,Object> exact=mapper.getHandoverSnapshot(id(row.get("endSnapshotId")));if(exact!=null&&parse(exact.get("capturedAt")).equals(parse(row.get("endTime")))){base=new HashMap<>(exact);exposure=exposureTotal(base);estimated=false;continue;}}
            for(String[] pair:List.of(new String[]{"watchTotal","watchDelta"},new String[]{"interactionTotal","interactionDelta"},new String[]{"newFanTotal","newFanDelta"},new String[]{"dealTotal","dealDelta"},new String[]{"presaleOrderTotal","presaleOrderDelta"},new String[]{"gmvTotal","gmvDelta"},new String[]{"adCostTotal","adCostDelta"}))base.put(pair[0],decimal(base.get(pair[0])).add(decimal(row.get(pair[1]))));
            BigDecimal rowRate=decimal(row.get("exposureWatchRate")),watch=decimal(row.get("watchDelta"));
            exposure=exposure==null||rowRate.signum()<=0?null:exposure.add(watch.multiply(BigDecimal.valueOf(100)).divide(rowRate,8,RoundingMode.HALF_UP));
            base.put("capturedAt",row.get("endTime"));estimated=true;
        }
        if(estimated){
            base.remove("snapshotId");base.put("estimatedBaseline",true);
            base.put("exposureWatchRate",exposure==null||exposure.signum()<=0?BigDecimal.ZERO:decimal(base.get("watchTotal")).multiply(BigDecimal.valueOf(100)).divide(exposure,4,RoundingMode.HALF_UP));
            long watch=number(base.get("watchTotal"));base.put("watchFollowRate",rate(number(base.get("newFanTotal")),watch));base.put("watchInteractionRate",rate(number(base.get("interactionTotal")),watch));base.put("watchDealRate",rate(number(base.get("dealTotal")),watch));
        }
        return base;
    }

    private void validateAppDelta(Map<String,Object> baseline,Map<String,Object> current)
    {
        if(!parse(current.get("capturedAt")).isAfter(parse(baseline.get("capturedAt"))))throw new ServiceException("本次采集时间必须晚于上次上报，请重新采集");
        for(String key:List.of("watchTotal","interactionTotal","newFanTotal","dealTotal","presaleOrderTotal","gmvTotal","adCostTotal"))
            if(decimal(current.get(key)).compareTo(decimal(baseline.get(key)))<0)throw new ServiceException("累计数据发生回退，请核对场次或由管理员调整后再报");
    }

    @Transactional
    public void addRoomAnchor(Long roomId,Long userId,String username)
    {
        if(mapper.getHandoverRoom(roomId)==null)throw new ServiceException("直播间不存在或已停用");
        if(mapper.countActiveUser(userId)==0)throw new ServiceException("主播账号不存在或已停用");
        if(mapper.countRoomAnchor(roomId,userId)>0)throw new ServiceException("该主播已经在当前直播间");
        mapper.insertRoomAnchor(roomId,userId,username);
    }

    @Transactional
    public void removeRoomAnchor(Long roomId,Long userId)
    {
        if(mapper.countRoomAnchor(roomId,userId)==0)throw new ServiceException("该主播不在当前直播间");
        mapper.deleteRoomAnchor(roomId,userId);
    }

    public Map<String,Object> roomDetail(Long roomId,Map<String,Object> query)
    {
        Map<String,Object> room=mapper.getHandoverRoom(roomId);if(room==null)throw new ServiceException("直播间不存在");
        Map<String,Object> params=new HashMap<>(query);params.put("roomId",roomId);
        room.put("anchors",mapper.listRoomAnchors(roomId));room.put("staff",mapper.listStaffOptions());
        List<Map<String,Object>> sessions=mapper.listHandoverSessions(params);
        for(Map<String,Object> session:sessions)enrichSession(session,id(session.get("sessionId")),false);
        room.put("sessions",sessions);return room;
    }

    public Map<String,Object> sessionDetail(Long sessionId)
    {
        Map<String,Object> session=mapper.getHandoverSession(sessionId);
        if(session==null)throw new ServiceException("直播场次不存在");
        return enrichSession(new LinkedHashMap<>(session),sessionId,true);
    }

    private Map<String,Object> enrichSession(Map<String,Object> value,Long sessionId,boolean includeAnchors)
    {
        value.put("segments",mapper.listHandoverSegments(sessionId));
        value.put("snapshots",mapper.listHandoverSnapshots(sessionId));
        value.put("corrections",mapper.listHandoverCorrections(sessionId));
        if(includeAnchors)value.put("anchors",value.get("roomId")==null?List.of():mapper.listRoomAnchors(id(value.get("roomId"))));
        return value;
    }

    @Transactional
    public Map<String,Object> createSegment(Long sessionId,Map<String,Object> input,Long userId,String userName)
    {
        Map<String,Object> session=mapper.lockHandoverSession(sessionId);
        if(session==null)throw new ServiceException("直播场次不存在");
        Long roomId=id(session.get("roomId"));
        if(roomId==null)throw new ServiceException("该场次没有匹配直播间");
        String reason=text(input.get("reason"));
        if(reason.isBlank())reason="人工补录";
        Long anchorId=longValue(input.get("anchorUserId"),"主播");
        validateAnchor(roomId,anchorId);
        Map<String,Object> value=manualValue(session,input);
        Long adjustedSegmentId=rebalanceForSupplement(session,value,reason,userId,userName);
        value.put("sessionId",sessionId);value.put("roomId",roomId);value.put("anchorUserId",anchorId);
        value.put("precisionStatus","MANUAL");value.put("source","MANUAL");value.put("remark",reason);value.put("createBy",userName);
        mapper.insertHandoverSegment(value);
        correction(sessionId,id(value.get("segmentId")),"CREATE",null,value,reason,userId,userName);
        if(adjustedSegmentId!=null)changed(adjustedSegmentId,false);
        changed(id(value.get("segmentId")),false);
        return mapper.getHandoverSegment(id(value.get("segmentId")));
    }

    private Long rebalanceForSupplement(Map<String,Object> session,Map<String,Object> supplement,String reason,Long userId,String userName)
    {
        Long sessionId=id(session.get("sessionId"));
        List<Map<String,Object>> segments=mapper.listHandoverSegments(sessionId);
        LocalDateTime start=snapToBoundary(parse(supplement.get("startTime")),session,segments),end=snapToBoundary(parse(supplement.get("endTime")),session,segments);
        if(!end.isAfter(start))throw new ServiceException("结束时间必须晚于开始时间");
        supplement.put("startTime",db(start));supplement.put("endTime",db(end));
        List<Map<String,Object>> overlaps=segments.stream().filter(item ->
            parse(item.get("startTime")).isBefore(end)&&parse(item.get("endTime")).isAfter(start)).toList();
        if(overlaps.isEmpty())
        {
            boolean connected=start.equals(parse(session.get("liveStartTime")))||segments.stream().anyMatch(item -> start.equals(parse(item.get("endTime")))||end.equals(parse(item.get("startTime"))));
            if(!connected)throw new ServiceException("补录的开始时间或结束时间至少要与已有记录无缝衔接；若连续漏报两场，请按时间先后分两次补录");
            return null;
        }
        if(overlaps.size()>1)throw new ServiceException("补录时间跨越了多条主播记录，请缩小时间范围后再补录");
        Map<String,Object> old=overlaps.get(0);
        LocalDateTime oldStart=parse(old.get("startTime")),oldEnd=parse(old.get("endTime"));
        boolean trimStart=start.equals(oldStart)&&end.isBefore(oldEnd);
        boolean trimEnd=end.equals(oldEnd)&&start.isAfter(oldStart);
        if(!trimStart&&!trimEnd)
            throw new ServiceException("补录时间需从现有记录的开始时间起，或到现有记录的结束时间止，系统才能准确重算相邻数据");

        Map<String,Object> adjusted=new HashMap<>(old);
        subtractMetrics(adjusted,supplement);
        if(trimStart)
        {
            adjusted.put("startTime",supplement.get("endTime"));
            adjusted.put("startSnapshotId",null);
        }
        else
        {
            adjusted.put("endTime",supplement.get("startTime"));
            adjusted.put("endSnapshotId",null);
        }
        adjusted.put("precisionStatus","MANUAL");adjusted.put("source","ADJUSTED");adjusted.put("remark","因补录自动重算："+reason);
        adjusted.put("updateBy",userName);adjusted.put("version",Integer.valueOf(String.valueOf(old.get("version"))));
        if(mapper.updateHandoverSegment(adjusted)!=1)throw new ServiceException("相邻记录刚刚被修改，请刷新后重试");
        correction(sessionId,id(old.get("segmentId")),"RECALCULATE",old,adjusted,"补录后自动重算相邻记录："+reason,userId,userName);
        return id(old.get("segmentId"));
    }

    private static LocalDateTime snapToBoundary(LocalDateTime value,Map<String,Object> session,List<Map<String,Object>> segments)
    {
        LocalDateTime liveStart=parse(session.get("liveStartTime"));
        if(sameMinute(value,liveStart))return liveStart;
        for(Map<String,Object> segment:segments)
        {
            LocalDateTime start=parse(segment.get("startTime")),end=parse(segment.get("endTime"));
            if(sameMinute(value,start))return start;
            if(sameMinute(value,end))return end;
        }
        return value;
    }

    private static boolean sameMinute(LocalDateTime left,LocalDateTime right)
    {return left.truncatedTo(ChronoUnit.MINUTES).equals(right.truncatedTo(ChronoUnit.MINUTES));}

    private static void subtractMetrics(Map<String,Object> target,Map<String,Object> supplement)
    {
        long originalWatch=number(target.get("watchDelta"));
        long watch=subtractLong(target,supplement,"watchDelta","观看人数"),interaction=subtractLong(target,supplement,"interactionDelta","互动人数"),fans=subtractLong(target,supplement,"newFanDelta","新增粉丝"),deal=subtractLong(target,supplement,"dealDelta","成交人数"),presale=subtractLong(target,supplement,"presaleOrderDelta","预售订单");
        BigDecimal exposureRate=remainingExposureRate(target,supplement,originalWatch,watch);
        target.put("watchDelta",watch);target.put("interactionDelta",interaction);target.put("newFanDelta",fans);target.put("dealDelta",deal);target.put("presaleOrderDelta",presale);
        target.put("gmvDelta",subtractDecimal(target,supplement,"gmvDelta","成交金额"));
        target.put("adCostDelta",subtractDecimal(target,supplement,"adCostDelta","投流消耗"));
        target.put("watchFollowRate",rate(fans,watch));target.put("watchInteractionRate",rate(interaction,watch));target.put("watchDealRate",rate(deal,watch));
        target.put("exposureWatchRate",exposureRate);
    }

    private static long subtractLong(Map<String,Object> target,Map<String,Object> supplement,String key,String label)
    {
        long result=number(target.get(key))-number(supplement.get(key));
        if(result<0)throw new ServiceException("补录的"+label+"不能大于相邻记录，请核对后再保存");
        return result;
    }

    private static BigDecimal subtractDecimal(Map<String,Object> target,Map<String,Object> supplement,String key,String label)
    {
        BigDecimal result=decimal(target.get(key)).subtract(decimal(supplement.get(key)));
        if(result.signum()<0)throw new ServiceException("补录的"+label+"不能大于相邻记录，请核对后再保存");
        return result.setScale(4,RoundingMode.HALF_UP);
    }

    private static BigDecimal remainingExposureRate(Map<String,Object> original,Map<String,Object> supplement,long originalWatch,long remainingWatch)
    {
        if(remainingWatch<=0)return BigDecimal.ZERO;
        BigDecimal originalRate=decimal(original.get("exposureWatchRate")),supplementRate=decimal(supplement.get("exposureWatchRate"));
        if(originalRate.signum()<=0||supplementRate.signum()<=0)return BigDecimal.ZERO;
        BigDecimal originalExposure=BigDecimal.valueOf(originalWatch).multiply(BigDecimal.valueOf(100)).divide(originalRate,8,RoundingMode.HALF_UP);
        BigDecimal supplementExposure=decimal(supplement.get("watchDelta")).multiply(BigDecimal.valueOf(100)).divide(supplementRate,8,RoundingMode.HALF_UP);
        BigDecimal remainingExposure=originalExposure.subtract(supplementExposure);
        if(remainingExposure.signum()<=0)throw new ServiceException("补录的进房率与观看人数不匹配，请核对后再保存");
        return BigDecimal.valueOf(remainingWatch).multiply(BigDecimal.valueOf(100)).divide(remainingExposure,4,RoundingMode.HALF_UP);
    }

    @Transactional
    public Map<String,Object> updateSegment(Long segmentId,Map<String,Object> input,Long userId,String userName)
    {
        Map<String,Object> old=mapper.getHandoverSegment(segmentId);if(old==null)throw new ServiceException("主播片段不存在");
        String reason=text(input.get("reason"));if(reason.isBlank())throw new ServiceException("请填写调整原因");
        Long anchorId=longValue(input.get("anchorUserId"),"主播");
        Integer version=Integer.valueOf(String.valueOf(input.get("version")));
        validateAnchor(id(old.get("roomId")),anchorId);
        Map<String,Object> session=mapper.getHandoverSession(id(old.get("sessionId")));
        boolean manual=isManualMode(input);
        Map<String,Object> value=manual?manualValue(session,input):automaticValue(id(old.get("sessionId")),input);
        if(mapper.countHandoverSegmentOverlap(id(old.get("sessionId")),value.get("startTime"),value.get("endTime"),segmentId)>0)throw new ServiceException("调整后的时间段与其他主播记录重叠");
        value.put("segmentId",segmentId);value.put("anchorUserId",anchorId);value.put("precisionStatus",manual?"MANUAL":"EXACT");value.put("source",manual?"MANUAL":"AUTO");value.put("remark",reason);value.put("updateBy",userName);value.put("version",version);
        if(mapper.updateHandoverSegment(value)!=1)throw new ServiceException("该片段已被其他人修改，请刷新后重试");
        correction(id(old.get("sessionId")),segmentId,"UPDATE",old,value,reason,userId,userName);
        changed(segmentId,false);
        return mapper.getHandoverSegment(segmentId);
    }

    @Transactional
    public Map<String,Object> voidSegment(Long segmentId,Map<String,Object> input,Long userId,String userName)
    {
        Map<String,Object> initial=mapper.getHandoverSegment(segmentId);if(initial==null)throw new ServiceException("主播记录不存在");
        Long sessionId=id(initial.get("sessionId"));
        if(mapper.lockHandoverSession(sessionId)==null)throw new ServiceException("直播场次不存在");
        Map<String,Object> old=mapper.getHandoverSegment(segmentId);
        if(old==null||!"CLOSED".equals(Objects.toString(old.get("status"),"")))throw new ServiceException("该上报已经作废，请刷新后查看");
        String reason=text(input.get("reason"));
        if(reason.isBlank())throw new ServiceException("请填写作废原因");
        if(reason.length()>500)throw new ServiceException("作废原因不能超过500个字符");
        Integer version;
        try{version=Integer.valueOf(Objects.toString(input.get("version"),""));}
        catch(Exception exception){throw new ServiceException("记录版本不正确，请刷新后重试");}

        List<Map<String,Object>> segments=mapper.listHandoverSegments(sessionId);
        int index=-1;
        for(int i=0;i<segments.size();i++)if(Objects.equals(segmentId,id(segments.get(i).get("segmentId")))){index=i;break;}
        if(index<0)throw new ServiceException("该上报已经作废，请刷新后查看");
        Map<String,Object> next=index+1<segments.size()?segments.get(index+1):null;
        Long startSnapshotId=id(old.get("startSnapshotId")),endSnapshotId=id(old.get("endSnapshotId"));
        if(startSnapshotId==null||endSnapshotId==null)throw new ServiceException("该记录缺少原始快照，不能自动作废，请使用调整功能修正");

        Map<String,Object> adjusted=null;
        if(next!=null)
        {
            Long nextStart=id(next.get("startSnapshotId")),nextEnd=id(next.get("endSnapshotId"));
            if(!Objects.equals(nextStart,endSnapshotId)||nextEnd==null)
                throw new ServiceException("下一条记录与本条快照未连续，不能自动重算，请先修复相邻记录");
            Map<String,Object> start=mapper.getHandoverSnapshot(startSnapshotId),end=mapper.getHandoverSnapshot(nextEnd);
            if(start==null||end==null)throw new ServiceException("相邻记录的原始快照缺失，不能自动重算");
            adjusted=new HashMap<>(next);
            adjusted.putAll(calculation(start,end));
            adjusted.put("startSnapshotId",startSnapshotId);
            adjusted.put("startTime",old.get("startTime"));
            adjusted.put("precisionStatus","EXACT");
            adjusted.put("source","AUTO");
            adjusted.put("remark","因上一条上报作废自动重算："+reason);
            adjusted.put("updateBy",userName);
        }

        Map<String,Object> voidValue=new HashMap<>();voidValue.put("segmentId",segmentId);voidValue.put("version",version);voidValue.put("reason",reason);voidValue.put("userId",userId);voidValue.put("userName",userName);
        if(mapper.voidHandoverSegment(voidValue)!=1)throw new ServiceException("记录已被其他人修改，请刷新后重试");
        correction(sessionId,segmentId,"VOID",old,null,reason,userId,userName);
        if(adjusted!=null)
        {
            Map<String,Object> nextOld=new HashMap<>(next);
            if(mapper.updateHandoverSegment(adjusted)!=1)throw new ServiceException("下一条记录已被其他人修改，请刷新后重试");
            correction(sessionId,id(next.get("segmentId")),"RECALCULATE",nextOld,adjusted,"上一条上报作废后自动重算："+reason,userId,userName);
            changed(id(next.get("segmentId")),false);
        }
        else
        {
            Map<String,Object> previous=mapper.getHandoverSnapshot(startSnapshotId);
            mapper.resetHandoverSessionSnapshot(sessionId,previous==null?null:Objects.toString(previous.get("capturedAt"),null));
        }
        changed(segmentId,true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("voidedSegmentId",segmentId);result.put("recalculatedSegmentId",next==null?null:next.get("segmentId"));result.put("recalculatedAnchorName",next==null?null:next.get("anchorName"));return result;
    }

    private Map<String,Object> calculation(Map<String,Object> start,Map<String,Object> end)
    {
        Map<String,Object> value=new HashMap<>();
        long watch=delta(start,end,"watchTotal"),interaction=delta(start,end,"interactionTotal"),fans=delta(start,end,"newFanTotal"),deal=delta(start,end,"dealTotal"),presale=delta(start,end,"presaleOrderTotal");
        value.put("watchDelta",watch);value.put("interactionDelta",interaction);value.put("newFanDelta",fans);value.put("dealDelta",deal);value.put("presaleOrderDelta",presale);
        value.put("gmvDelta",decimalDelta(start,end,"gmvTotal"));value.put("adCostDelta",decimalDelta(start,end,"adCostTotal"));
        value.put("exposureWatchRate",exposureRate(start,end,watch));value.put("watchFollowRate",rate(fans,watch));value.put("watchInteractionRate",rate(interaction,watch));value.put("watchDealRate",rate(deal,watch));return value;
    }

    private Map<String,Object> automaticValue(Long sessionId,Map<String,Object> input)
    {
        Long startId=longValue(input.get("startSnapshotId"),"开始时间"),endId=longValue(input.get("endSnapshotId"),"结束时间");
        Map<String,Object> start=mapper.getHandoverSnapshot(startId),end=mapper.getHandoverSnapshot(endId);
        validateBoundary(sessionId,start,end);
        Map<String,Object> value=calculation(start,end);
        value.put("startSnapshotId",startId);value.put("endSnapshotId",endId);value.put("startTime",start.get("capturedAt"));value.put("endTime",end.get("capturedAt"));
        return value;
    }

    private Map<String,Object> manualValue(Map<String,Object> session,Map<String,Object> input)
    {
        LocalDateTime start=parseRequired(input.get("startTime"),"开始时间"),end=parseRequired(input.get("endTime"),"结束时间"),liveStart=parse(session.get("liveStartTime"));
        if(start.isBefore(liveStart))throw new ServiceException("开始时间不能早于本场开播时间");
        if(!end.isAfter(start))throw new ServiceException("结束时间必须晚于开始时间");
        if(end.isAfter(LocalDateTime.now(ZONE).plusMinutes(1)))throw new ServiceException("结束时间不能晚于当前时间");
        if(end.isAfter(liveStart.plusDays(2)))throw new ServiceException("结束时间不能超过本场开播后两天");
        long watch=nonNegativeLong(input,"watchDelta","观看人数"),fans=nonNegativeLong(input,"newFanDelta","新增粉丝"),deal=nonNegativeLong(input,"dealDelta","成交人数"),presale=nonNegativeLong(input,"presaleOrderDelta","预售订单");
        if(fans>watch)throw new ServiceException("新增粉丝不能大于观看人数");
        if(deal>watch)throw new ServiceException("成交人数不能大于观看人数");
        BigDecimal interactionRate=rateValue(input,"watchInteractionRate","互动率");
        Map<String,Object> value=new HashMap<>();
        value.put("startSnapshotId",null);value.put("endSnapshotId",null);value.put("startTime",db(start));value.put("endTime",db(end));
        value.put("watchDelta",watch);value.put("newFanDelta",fans);value.put("dealDelta",deal);value.put("presaleOrderDelta",presale);
        value.put("interactionDelta",BigDecimal.valueOf(watch).multiply(interactionRate).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP).longValue());
        value.put("gmvDelta",nonNegativeDecimal(input,"gmvDelta","成交金额"));value.put("adCostDelta",nonNegativeDecimal(input,"adCostDelta","投流消耗"));
        value.put("exposureWatchRate",rateValue(input,"exposureWatchRate","进房率"));value.put("watchFollowRate",rateValue(input,"watchFollowRate","转粉率"));value.put("watchInteractionRate",interactionRate);value.put("watchDealRate",rateValue(input,"watchDealRate","成交率"));
        return value;
    }

    private Map<String,Object> insertSnapshot(LiveHandoverRequest request,Long sessionId,String kind)
    {
        Map<String,Object> value=snapshot(request,sessionId,kind);mapper.insertHandoverSnapshot(value);mapper.updateHandoverSessionSnapshot(sessionId,String.valueOf(value.get("capturedAt")));return value;
    }

    private Map<String,Object> snapshot(LiveHandoverRequest r,Long sessionId,String kind)
    {
        Map<String,Object> v=new HashMap<>();v.put("sessionId",sessionId);v.put("snapshotKind",kind);v.put("capturedAt",captured(r));v.put("watchTotal",r.getWatchTotal());v.put("interactionTotal",r.getInteractionTotal());v.put("newFanTotal",r.getNewFanTotal());v.put("dealTotal",r.getDealTotal());v.put("gmvTotal",r.getGmvTotal());v.put("adCostTotal",r.getAdCostTotal());v.put("presaleOrderTotal",r.getPresaleOrderTotal());v.put("exposureWatchRate",r.getExposureWatchRate());v.put("watchFollowRate",r.getWatchFollowRate());v.put("watchInteractionRate",r.getWatchInteractionRate());v.put("watchDealRate",r.getWatchDealRate());v.put("idempotencyKey",r.getIdempotencyKey());v.put("source","PLUGIN");return v;
    }

    private Map<String,Object> zeroSnapshot(Map<String,Object> session,LiveHandoverRequest r)
    {
        Map<String,Object> v=new HashMap<>();v.put("sessionId",session.get("sessionId"));v.put("snapshotKind","ZERO");v.put("capturedAt",session.get("liveStartTime"));for(String key:List.of("watchTotal","interactionTotal","newFanTotal","dealTotal","presaleOrderTotal"))v.put(key,0L);for(String key:List.of("gmvTotal","adCostTotal","exposureWatchRate","watchFollowRate","watchInteractionRate","watchDealRate"))v.put(key,BigDecimal.ZERO);v.put("idempotencyKey","zero-"+r.getPlatformSessionId());v.put("source","SYSTEM");return v;
    }

    private Map<String,Object> ensureSession(LiveHandoverRequest r)
    {
        Map<String,Object> existing=mapper.findHandoverSessionByPlatformId(r.getPlatformSessionId());if(existing!=null)return existing;
        Long roomId=resolveRoomId(r.getRoomName());Map<String,Object> value=new HashMap<>();value.put("roomId",roomId);value.put("platformSessionId",r.getPlatformSessionId());value.put("reportedRoomName",r.getRoomName().trim());value.put("liveStartTime",db(parse(r.getLiveStartTime())));mapper.insertHandoverSession(value);return value;
    }

    private Map<String,Object> state(Long sessionId,boolean duplicate)
    {
        Map<String,Object> session=mapper.lockHandoverSession(sessionId);Map<String,Object> v=new LinkedHashMap<>();v.put("duplicate",duplicate);v.put("session",session);v.put("previousSnapshot",mapper.getLatestHandoverSnapshot(sessionId));v.put("segments",mapper.listHandoverSegments(sessionId));v.put("anchors",session.get("roomId")==null?List.of():mapper.listRoomAnchors(id(session.get("roomId"))));return v;
    }

    private Long resolveRoomId(String roomName)
    {
        String key=normalize(roomName);Map<String,Object> alias=mapper.findRoomByAliasKey(key);if(alias!=null)return id(alias.get("roomId"));
        Long match=null;
        for(Map<String,Object> room:mapper.listRoomOptions())if(normalize(room.get("roomName")).equals(key))
        {if(match!=null)throw new ServiceException("存在同名直播间，请管理员绑定明确的直播间别名");match=id(room.get("roomId"));}
        return match;
    }

    private void validateAnchor(Long roomId,Long anchorId){if(anchorId==null)throw new ServiceException("请选择主播");if(mapper.countRoomAnchor(roomId,anchorId)==0)throw new ServiceException("该主播不属于当前直播间，请先在网站添加");}
    private static boolean isManualMode(Map<String,Object> input)
    {
        String mode=text(input.get("dataMode")).toUpperCase();
        if(!"AUTO".equals(mode)&&!"MANUAL".equals(mode))throw new ServiceException("请选择数据填写方式");
        return "MANUAL".equals(mode);
    }
    private void validateBoundary(Long sessionId,Map<String,Object> start,Map<String,Object> end)
    {
        if(start==null||end==null||!Objects.equals(id(start.get("sessionId")),sessionId)||!Objects.equals(id(end.get("sessionId")),sessionId))throw new ServiceException("所选时间不属于当前直播场次");
        if(!parse(end.get("capturedAt")).isAfter(parse(start.get("capturedAt"))))throw new ServiceException("结束时间必须晚于开始时间");
    }
    private void validateMetrics(LiveHandoverRequest r){if(r.getDealTotal()>r.getWatchTotal())throw new ServiceException("成交人数不能大于观看人数");if(r.getNewFanTotal()>r.getWatchTotal())throw new ServiceException("新增粉丝不能大于观看人数");if(r.getInteractionTotal()>r.getWatchTotal())throw new ServiceException("互动人数不能大于观看人数");if(r.getGmvTotal().compareTo(MONEY_MAX)>0)throw new ServiceException("成交金额超出有效范围，请重新读取页面数据");if(r.getAdCostTotal().compareTo(MONEY_MAX)>0)throw new ServiceException("千川消耗超出有效范围，请重新读取页面数据");}
    private String captured(LiveHandoverRequest r){long now=System.currentTimeMillis(),raw=r.getCapturedAt()==null?now:r.getCapturedAt();if(Math.abs(now-raw)>10*60*1000)raw=now;return db(LocalDateTime.ofInstant(Instant.ofEpochMilli(raw),ZONE));}
    private static long delta(Map<String,Object> a,Map<String,Object> b,String key){return number(b.get(key))-number(a.get(key));}
    private static BigDecimal decimalDelta(Map<String,Object> a,Map<String,Object> b,String key){return decimal(b.get(key)).subtract(decimal(a.get(key)));}
    private static BigDecimal rate(long numerator,long denominator){return numerator<=0||denominator<=0?BigDecimal.ZERO:BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator),4,RoundingMode.HALF_UP);}
    private static BigDecimal exposureRate(Map<String,Object> start,Map<String,Object> end,long watchDelta)
    {
        BigDecimal startExposure=exposureTotal(start),endExposure=exposureTotal(end);
        if(startExposure==null||endExposure==null)return BigDecimal.ZERO;
        BigDecimal exposureDelta=endExposure.subtract(startExposure);
        if(watchDelta<=0||exposureDelta.signum()<=0)return BigDecimal.ZERO;
        return BigDecimal.valueOf(watchDelta).multiply(BigDecimal.valueOf(100)).divide(exposureDelta,4,RoundingMode.HALF_UP);
    }
    private static BigDecimal exposureTotal(Map<String,Object> snapshot)
    {
        BigDecimal watch=decimal(snapshot.get("watchTotal")),rate=decimal(snapshot.get("exposureWatchRate"));
        if(watch.signum()==0)return BigDecimal.ZERO;
        return rate.signum()<=0?null:watch.multiply(BigDecimal.valueOf(100)).divide(rate,8,RoundingMode.HALF_UP);
    }
    private void correction(Long sessionId,Long segmentId,String action,Object old,Object value,String reason,Long userId,String userName){Map<String,Object> row=new HashMap<>();row.put("sessionId",sessionId);row.put("segmentId",segmentId);row.put("actionType",action);row.put("oldValue",old==null?null:JSON.toJSONString(old));row.put("newValue",value==null?null:JSON.toJSONString(value));row.put("reason",reason);row.put("correctedBy",userId);row.put("correctedName",userName);mapper.insertHandoverCorrection(row);}
    private void changed(Long segmentId,boolean deleted){events.publishEvent(new BiLiveAnchorChangedEvent(segmentId,deleted));}
    private static Long longValue(Object v,String label){try{return Long.valueOf(String.valueOf(v));}catch(Exception e){throw new ServiceException("请选择"+label);}}
    private static long nonNegativeLong(Map<String,Object> input,String key,String label)
    {
        try{long value=new BigDecimal(text(input.get(key))).longValueExact();if(value<0)throw new Exception();return value;}
        catch(Exception exception){throw new ServiceException(label+"必须是大于或等于 0 的整数");}
    }
    private static BigDecimal nonNegativeDecimal(Map<String,Object> input,String key,String label)
    {
        try{BigDecimal value=new BigDecimal(text(input.get(key)));if(value.signum()<0)throw new Exception();return value.setScale(4,RoundingMode.HALF_UP);}
        catch(Exception exception){throw new ServiceException(label+"必须是大于或等于 0 的数字");}
    }
    private static BigDecimal rateValue(Map<String,Object> input,String key,String label)
    {
        BigDecimal value=nonNegativeDecimal(input,key,label);if(value.compareTo(BigDecimal.valueOf(100))>0)throw new ServiceException(label+"不能超过 100%");return value;
    }
    private static LocalDateTime parseRequired(Object value,String label)
    {
        if(text(value).isBlank())throw new ServiceException("请选择"+label);
        try{return parse(value);}catch(ServiceException exception){throw new ServiceException(label+"格式不正确");}
    }
    private static Long id(Object v){return v==null?null:Long.valueOf(String.valueOf(v));}
    private static long number(Object v){if(v==null)return 0;return new BigDecimal(String.valueOf(v)).longValue();}
    private static BigDecimal decimal(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(String.valueOf(v));}
    public static String normalize(Object v){return Normalizer.normalize(text(v),Normalizer.Form.NFKC).replaceAll("\\s+","").toLowerCase(java.util.Locale.ROOT);}
    private static String text(Object v){return Objects.toString(v,"").trim();}
    private static String db(LocalDateTime v){return v.format(DB);}
    private static LocalDateTime parse(Object value)
    {
        String input=text(value).replace('T',' ');if(input.length()==16)input+=":00";int dot=input.indexOf('.',19);if(dot>0)input=input.substring(0,dot);
        try{return LocalDateTime.parse(input,DB);}catch(Exception exception){throw new ServiceException("开播时间格式不正确");}
    }
}
