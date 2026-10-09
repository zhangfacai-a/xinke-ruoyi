package com.xinke.erp.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.LiveMetricsMapper;

@Service
public class LiveMetricsService
{
    private static final DateTimeFormatter DB=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_DAILY_SHIFTS=4;
    private final LiveMetricsMapper mapper;
    public LiveMetricsService(LiveMetricsMapper mapper){this.mapper=mapper;}

    public List<Map<String,Object>> listShifts(Map<String,Object> query){return mapper.listShifts(query);}
    public Map<String,Object> options(Long userId){Map<String,Object> result=new LinkedHashMap<>();result.put("rooms",mapper.listRoomOptionsForAnchor(userId));result.put("allRooms",mapper.listRoomOptions());result.put("staff",mapper.listStaffOptions());result.put("hiddenUserIds",mapper.listHiddenStaffUserIds(userId));result.put("currentUserId",userId);return result;}

    @Transactional
    public void saveStaffVisibility(Long viewerUserId,Map<String,Object> request)
    {
        if(viewerUserId==null)throw new ServiceException("当前账号未登录");
        Object raw=request.get("hiddenUserIds");
        if(!(raw instanceof List<?> values))throw new ServiceException("人员显示设置格式不正确");
        if(values.size()>1000)throw new ServiceException("隐藏人员数量过多");
        List<Long> hiddenUserIds=new ArrayList<>();
        for(Object value:values)
        {
            Long userId=longValue(value,"人员");
            if(Objects.equals(userId,viewerUserId)||hiddenUserIds.contains(userId))continue;
            if(mapper.countActiveUser(userId)==0)throw new ServiceException("人员账号不存在或已停用");
            hiddenUserIds.add(userId);
        }
        mapper.deleteHiddenStaff(viewerUserId);
        if(!hiddenUserIds.isEmpty())mapper.insertHiddenStaff(viewerUserId,hiddenUserIds);
    }
    public List<Map<String,Object>> templates(Long roomId){if(roomId==null)throw new ServiceException("请先选择直播间");return mapper.listShiftTemplates(roomId);}

    public void saveTemplate(Map<String,Object> value,String user)
    {
        Long roomId=longValue(value.get("roomId"),"直播间"),templateId=value.get("templateId")==null?null:longValue(value.get("templateId"),"模板");
        String name=Objects.toString(value.get("templateName"),"").trim(),start=clock(value.get("startClock")),end=clock(value.get("endClock"));
        if(name.isBlank())throw new ServiceException("请输入模板名称");if(name.length()>50)throw new ServiceException("模板名称不能超过50个字");
        if(mapper.countShiftTemplateName(roomId,name,templateId)>0)throw new ServiceException("当前直播间已经有同名模板");
        value.put("roomId",roomId);value.put("templateId",templateId);value.put("templateName",name);value.put("startClock",start);value.put("endClock",end);value.put("createBy",user);
        if(templateId==null)mapper.insertShiftTemplate(value);else if(mapper.updateShiftTemplate(value)==0)throw new ServiceException("模板不存在或已删除");
    }
    public void deleteTemplate(Long id){if(id==null||mapper.deleteShiftTemplate(id)==0)throw new ServiceException("模板不存在或已删除");}

    public void saveShift(Map<String,Object> value,String user)
    {
        normalizeShift(value);validateShift(value);value.put(value.get("shiftId")==null?"createBy":"updateBy",user);
        if(value.get("shiftId")==null)mapper.insertShift(value);else if(mapper.updateShift(value)==0)throw new ServiceException("班次不存在或已删除");
    }

    @Transactional
    public void deleteShift(Long shiftId)
    {
        if(shiftId==null||mapper.getShift(shiftId)==null)throw new ServiceException("班次不存在或已删除");
        if(mapper.countShiftSubmissions(shiftId)>0)throw new ServiceException("该班次已有上报记录，不能删除；如排班有误，请修改班次或重新匹配上报记录");
        if(mapper.deleteShift(shiftId)==0)throw new ServiceException("班次已被上报或已经删除，请刷新后重试");
    }

    @Transactional
    public Map<String,Object> saveWeek(Map<String,Object> request,String user)
    {
        Long roomId=longValue(request.get("roomId"),"直播间");
        if(!(request.get("items") instanceof List<?> items)||items.isEmpty())throw new ServiceException("请至少选择一天排班");
        if(items.size()>100)throw new ServiceException("一次最多生成100个班次");
        List<Map<String,Object>> conflicts=new ArrayList<>();int created=0;
        for(Object raw:items)
        {
            if(!(raw instanceof Map<?,?> source))throw new ServiceException("排班内容格式不正确");
            Map<String,Object> shift=new HashMap<>();source.forEach((k,v)->shift.put(Objects.toString(k),v));shift.put("roomId",roomId);
            try{normalizeShift(shift);validateShift(shift);shift.put("createBy",user);mapper.insertShift(shift);created++;}
            catch(ServiceException ex){conflicts.add(Map.of("date",Objects.toString(shift.get("liveDate"),""),"reason",ex.getMessage()));}
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("created",created);result.put("skipped",conflicts.size());result.put("conflicts",conflicts);return result;
    }

    private void normalizeShift(Map<String,Object> value)
    {
        Long roomId=longValue(value.get("roomId"),"直播间"),anchorId=longValue(value.get("anchorUserId"),"主播");
        if(mapper.countActiveUser(anchorId)==0)throw new ServiceException("主播账号不存在或已停用");if(mapper.countRoomAnchor(roomId,anchorId)==0)throw new ServiceException("所选主播不属于该直播间");
        LocalDateTime start=parse(value.get("startTime")),end=parse(value.get("endTime"));if(!end.isAfter(start))throw new ServiceException("结束时间必须晚于开始时间");if(Duration.between(start,end).toHours()>24)throw new ServiceException("单个班次不能超过24小时");
        value.put("roomId",roomId);value.put("anchorUserId",anchorId);value.put("liveDate",start.toLocalDate().toString());value.put("startTime",start.format(DB));value.put("endTime",end.format(DB));value.put("remindAt",end.plusMinutes(5).format(DB));value.put("dingUserId",mapper.getDingUserId(anchorId));value.put("remark",Objects.toString(value.get("remark"),"").trim());
    }
    private void validateShift(Map<String,Object> value)
    {
        if(mapper.countAnchorShiftsOnDate(value)>=MAX_DAILY_SHIFTS)throw new ServiceException("该主播当天已有4个班次，不能继续添加");
        if(mapper.countOverlappingShifts(value)>0)throw new ServiceException("该直播间在所选时间已有班次");if(mapper.countOverlappingAnchorShifts(value)>0)throw new ServiceException("该主播同时段已有其他班次");
    }

    static LocalDateTime parse(Object value)
    {
        if(value instanceof LocalDateTime v)return v;if(value instanceof java.sql.Timestamp v)return v.toLocalDateTime();if(value instanceof Date v)return LocalDateTime.ofInstant(v.toInstant(),ZoneId.systemDefault());
        String text=Objects.toString(value,"").trim().replace('T',' ');if(text.length()==16)text+=":00";int dot=text.indexOf('.',19);if(dot>0)text=text.substring(0,dot);
        try{return LocalDateTime.parse(text,DB);}catch(Exception ex){throw new ServiceException("时间格式不正确");}
    }
    static String db(LocalDateTime value){return value.format(DB);} static long number(Object value){try{return Long.parseLong(Objects.toString(value,"0"));}catch(Exception ex){return 0;}}
    static Long longValue(Object value,String label){try{return Long.valueOf(Objects.toString(value,""));}catch(Exception ex){throw new ServiceException("请选择"+label);}}
    private static String clock(Object value){String text=Objects.toString(value,"").trim();if(!text.matches("(?:[01]\\d|2[0-3]):[0-5]\\d|24:00"))throw new ServiceException("模板时间格式不正确");return text;}
}
