package com.xinke.erp.service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.core.redis.RedisCache;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.mapper.LiveMetricsMapper;

@Service
public class LiveDesktopService {
    private final LiveHandoverService reports;
    private final LiveMetricsMapper mapper;
    private final RedisCache cache;
    private final LiveAutoAccessService access;
    public LiveDesktopService(LiveHandoverService reports,LiveMetricsMapper mapper,RedisCache cache,LiveAutoAccessService access){this.reports=reports;this.mapper=mapper;this.cache=cache;this.access=access;}
    public boolean manager(){return SecurityUtils.isAdmin()||SecurityUtils.hasPermi("live:metrics:manage");}
    public void requireRoom(Long roomId){if(roomId==null||(!manager()&&mapper.countRoomAnchor(roomId,SecurityUtils.getUserId())==0))throw new ServiceException("没有该直播间的访问权限");}
    public List<Map<String,Object>> rooms(){return reports.rooms().stream().filter(r->manager()||mapper.countRoomAnchor(number(r.get("roomId")),SecurityUtils.getUserId())>0).toList();}
    public Map<String,Object> room(Long id,Map<String,Object> query){requireRoom(id);Map<String,Object> data=reports.roomDetail(id,query);if(!manager())data.put("staff",List.of());return data;}
    public Map<String,Object> session(Long id){Map<String,Object> data=mapper.getHandoverSession(id);if(data==null)throw new ServiceException("场次不存在");requireRoom(number(data.get("roomId")));return reports.sessionDetail(id);}
    public Map<String,Object> context(String platformId,String roomName){Map<String,Object> value=reports.appContext(platformId,roomName);if(value.get("roomId")!=null)requireRoom(number(value.get("roomId")));return value;}
    public Map<String,Object> preview(LiveHandoverRequest request,String version){
        validateFresh(request);
        Map<String,Object> result=reports.previewApp(request);requireRoom(number(result.get("roomId")));
        if(!manager()&&!Objects.equals(request.getAnchorUserId(),SecurityUtils.getUserId()))throw new ServiceException("只能上报自己的主播数据");
        if(version==null||!version.matches("\\d+\\.\\d+\\.\\d+"))throw new ServiceException("客户端版本无效，请更新App");
        String ticket=UUID.randomUUID().toString();
        Map<String,Object> saved=new HashMap<>();saved.put("userId",SecurityUtils.getUserId());saved.put("request",JSON.toJSONString(request));saved.put("version",version);saved.put("preview",JSON.toJSONString(result));
        cache.setCacheObject("anchor:preview:"+ticket,JSON.toJSONString(saved),30,TimeUnit.MINUTES);
        result.put("ticket",ticket);result.put("expiresAt",request.getCapturedAt()+5*60*1000);
        return result;
    }
    @Transactional
    public Map<String,Object> commit(String ticket){
        Map<String,Object> saved=ticket(ticket);
        LiveHandoverRequest request=JSON.parseObject(String.valueOf(saved.get("request")),LiveHandoverRequest.class);
        Map<String,Object> ctx=context(request.getPlatformSessionId(),request.getRoomName());
        if(ctx.get("roomId")==null)throw new ServiceException("直播间尚未绑定");
        if(!manager()&&!Objects.equals(request.getAnchorUserId(),SecurityUtils.getUserId()))throw new ServiceException("只能上报自己的主播数据");
        if(mapper.getHandoverSnapshotByKey(request.getIdempotencyKey())==null){
            validateFresh(request);
            @SuppressWarnings("unchecked") Map<String,Object> session=(Map<String,Object>)ctx.get("session");
            if(session!=null)mapper.lockHandoverSession(number(session.get("sessionId")));
            Map<String,Object> refreshed=reports.previewApp(request);
            if(!JSON.parseObject(String.valueOf(saved.get("preview"))).equals(JSON.parseObject(JSON.toJSONString(refreshed))))throw new ServiceException("预览后记录已调整，请重新采集并预览");
        }
        return reports.transitionApp(request,SecurityUtils.getUserId(),SecurityUtils.getUsername(),String.valueOf(saved.get("version")));
    }
    private Map<String,Object> ticket(String ticket){
        if(ticket==null||!ticket.matches("[a-f0-9-]{36}"))throw new ServiceException("上报预览无效，请重新预览");
        String raw=cache.getCacheObject("anchor:preview:"+ticket);
        Map<String,Object> saved=raw==null?null:JSON.parseObject(raw);
        if(saved==null||!Objects.equals(number(saved.get("userId")),SecurityUtils.getUserId()))throw new ServiceException("预览已过期或不属于当前账号");
        return saved;
    }
    public static void validateFresh(LiveHandoverRequest request){
        if(request.getCapturedAt()==null||Math.abs(System.currentTimeMillis()-request.getCapturedAt())>5*60*1000)throw new ServiceException("采集数据已过期，请重新采集");
        if(request.getInteractionTotal()==null||request.getWatchTotal()==null)throw new ServiceException("采集字段不完整");
        for(java.math.BigDecimal rate:List.of(request.getExposureWatchRate(),request.getWatchFollowRate(),request.getWatchInteractionRate(),request.getWatchDealRate()))
            if(rate.signum()<0||rate.compareTo(java.math.BigDecimal.valueOf(100))>0)throw new ServiceException("比例必须在0到100之间");
    }
    @Transactional
    public void assign(Long roomId,Set<Long> expected,Set<Long> selected){
        if(!manager())throw new ServiceException("需要主播管理权限");
        if(mapper.lockRoom(roomId)==null)throw new ServiceException("直播间不存在");
        Set<Long> current=new HashSet<>();for(Map<String,Object> row:mapper.listRoomAnchors(roomId))current.add(number(row.get("userId")));
        if(!current.equals(expected))throw new ServiceException("主播名单已被修改，请刷新后重试");
        if(selected.size()>200)throw new ServiceException("单次最多分配200名主播");
        Set<Long> affected=new HashSet<>(current);affected.addAll(selected);
        for(Long user:selected){if(user==null||mapper.countActiveUser(user)==0)throw new ServiceException("名单包含无效用户");if(!current.contains(user))mapper.insertRoomAnchor(roomId,user,SecurityUtils.getUsername());}
        for(Long user:current)if(!selected.contains(user))mapper.deleteRoomAnchor(roomId,user);
        refreshAccess(affected);
    }
    @Transactional
    public void add(Long roomId,Long user){reports.addRoomAnchor(roomId,user,SecurityUtils.getUsername());refreshAccess(Set.of(user));}
    @Transactional
    public void remove(Long roomId,Long user){reports.removeRoomAnchor(roomId,user);refreshAccess(Set.of(user));}
    private void refreshAccess(Set<Long> users){access.reconcile(users);}
    private static Long number(Object value){return value==null?null:Long.valueOf(String.valueOf(value));}
}
