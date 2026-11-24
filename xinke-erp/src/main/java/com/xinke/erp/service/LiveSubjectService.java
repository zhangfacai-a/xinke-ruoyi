package com.xinke.erp.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.StringUtils;
import com.xinke.erp.mapper.LiveGiftMapper;

/** Shop, room and staffing configuration, independent of gift order operations. */
@Service
public class LiveSubjectService {
    private final LiveGiftMapper mapper;
    private final ShopStaffGroupService shopStaffGroups;
    public LiveSubjectService(LiveGiftMapper mapper,ShopStaffGroupService shopStaffGroups) {
        this.mapper=mapper;this.shopStaffGroups=shopStaffGroups;
    }
    public List<Map<String, Object>> listShops(Map<String, Object> q) { return mapper.selectShops(limitedQuery(q, 200, 500)); }
    public Map<String, Object> getShop(Long id) { return mapper.selectShopById(id); }
    @Transactional public void deleteShop(Long id) {
        if(mapper.selectShopById(id)==null) throw new ServiceException("店铺不存在");
        java.util.Set<Long> affected=shopStaffGroups.affectedUsers(id);
        mapper.deleteShop(id);
        mapper.deleteSubjectMappings("SHOP",id);
        shopStaffGroups.removeShop(id);
        shopStaffGroups.reconcile(affected);
    }

    @Transactional
    public void saveShop(Map<String, Object> value, String username)
    {
        required(value, "shopCode", "店铺编码不能为空");
        required(value, "shopName", "店铺名称不能为空");
        if (value.get("shopId") != null && mapper.selectShopById(Long.valueOf(value.get("shopId").toString())) == null)
            throw new ServiceException("店铺不存在或已删除");
        if (value.get("status") != null && !Set.of("0", "1").contains(value.get("status").toString()))
            throw new ServiceException("店铺状态不正确");
        value.putIfAbsent("status", "0"); value.put("username", username);
        try
        {
            if (value.get("shopId") == null) mapper.insertShop(value); else mapper.updateShop(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("店铺编码已经存在");
        }
        if(value.containsKey("staffGroupId")) {
            Object raw=value.get("staffGroupId");
            Long groupId=raw==null||raw.toString().isBlank()?null:Long.valueOf(raw.toString());
            shopStaffGroups.bind(Long.valueOf(value.get("shopId").toString()),groupId,username);
        }
        shopStaffGroups.reconcile(shopStaffGroups.subjectUsers("SHOP",Long.valueOf(value.get("shopId").toString())));
    }

    public List<Map<String, Object>> listRooms(Map<String, Object> q) { mapper.syncAudienceRooms(); return mapper.selectRooms(limitedQuery(q, 200, 500)); }
    public Map<String, Object> getRoom(Long id) { return mapper.selectRoomById(id); }

    @Transactional
    public void saveRoom(Map<String, Object> value, String username)
    {
        required(value, "roomCode", "直播间编码不能为空");
        required(value, "roomName", "直播间名称不能为空");
        value.put("username", username);
        value.putIfAbsent("status", "0");
        try
        {
            if (value.get("roomId") == null) mapper.insertRoom(value); else mapper.updateRoom(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("直播间名称或编码已经存在");
        }
    }

    public List<Map<String, Object>> listSubjectMappings(Map<String, Object> q) {
        List<Map<String,Object>> result=mapper.selectSubjectMappings(limitedQuery(q,500,500));
        for(Map<String,Object> row:result) if("SHOP".equals(row.get("subjectType")))
            row.put("staffGroupId",shopStaffGroups.groupForShop(Long.valueOf(row.get("subjectId").toString())));
        return result;
    }

    @Transactional
    public void saveSubjectMapping(Map<String, Object> value, String username)
    {
        String subjectType = Objects.toString(value.get("subjectType"), "").toUpperCase();
        if (!Set.of("ROOM", "SHOP").contains(subjectType)) throw new ServiceException("映射对象类型不正确");
        required(value, "subjectId", "请选择映射对象");
        Long subjectId = Long.valueOf(value.get("subjectId").toString());
        if ("ROOM".equals(subjectType) && mapper.selectRoomById(subjectId) == null) throw new ServiceException("直播间不存在");
        if ("SHOP".equals(subjectType) && mapper.selectShopById(subjectId) == null) throw new ServiceException("店铺不存在");
        if ("SHOP".equals(subjectType) && shopStaffGroups.groupForShop(subjectId)!=null)
            throw new ServiceException("此店铺继承人员组，请先切换为单独配置");
        for (String field : List.of("operatorIds", "customerServiceIds", "customerAdminIds", "anchorIds", "controllerIds"))
            for (Long userId : longList(value.get(field)))
                if (mapper.countActiveStaff(userId) == 0) throw new ServiceException("不能分配停用或不存在的人员");
        java.util.Set<Long> affected=shopStaffGroups.subjectUsers(subjectType,subjectId);
        mapper.deleteSubjectMappings(subjectType, subjectId);
        if ("SHOP".equals(subjectType))
        {
            longList(value.get("operatorIds")).stream().distinct()
                .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "operator", username));
            longList(value.get("customerServiceIds")).stream().distinct()
                .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "customer_service", username));
            longList(value.get("customerAdminIds")).stream().distinct()
                .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "customer_admin", username));
        }
        else
        {
            longList(value.get("anchorIds")).stream().distinct()
                .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "anchor", username));
            longList(value.get("controllerIds")).stream().distinct()
                .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "controller", username));
        }
        affected=new java.util.HashSet<>(affected);
        affected.addAll(shopStaffGroups.subjectUsers(subjectType,subjectId));
        shopStaffGroups.reconcile(affected);
    }


    private static void required(Map<String, Object> value, String key, String message)
    {
        if (value.get(key) == null || StringUtils.isBlank(value.get(key).toString())) throw new ServiceException(message);
    }

    private static List<Long> longList(Object value)
    {
        if (value == null) return List.of();
        if (value instanceof Collection<?> collection)
            return collection.stream().filter(Objects::nonNull).map(item -> Long.valueOf(item.toString())).toList();
        return Arrays.stream(value.toString().split(",")).filter(StringUtils::isNotBlank).map(Long::valueOf).toList();
    }

    private static Map<String, Object> limitedQuery(Map<String, Object> query, int defaultLimit, int maxLimit)
    {
        Map<String, Object> result = query == null ? new HashMap<>() : new HashMap<>(query);
        int limit = defaultLimit;
        try
        {
            if (result.get("limit") != null) limit = Integer.parseInt(result.get("limit").toString());
        }
        catch (NumberFormatException ignored) { }
        result.put("limit", Math.max(1, Math.min(maxLimit, limit)));
        return result;
    }
}
