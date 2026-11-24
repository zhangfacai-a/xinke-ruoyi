package com.xinke.erp.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.LiveGiftMapper;
import com.xinke.erp.mapper.ShopStaffGroupMapper;

@Service
public class ShopStaffGroupService {
    private static final Map<String,String> FIELDS=Map.of(
        "operatorIds","operator","customerServiceIds","customer_service","customerAdminIds","customer_admin");
    private final ShopStaffGroupMapper groups;
    private final LiveGiftMapper gifts;
    private final LiveAutoAccessService access;

    public ShopStaffGroupService(ShopStaffGroupMapper groups,LiveGiftMapper gifts,LiveAutoAccessService access) {
        this.groups=groups; this.gifts=gifts; this.access=access;
    }

    public List<Map<String,Object>> list() { return groups.groups(); }

    public Map<String,Object> detail(Long id) {
        Map<String,Object> group=groups.group(id);
        if(group==null) throw new ServiceException("人员组不存在");
        Map<String,Object> result=new HashMap<>(group);
        Map<String,List<Long>> memberIds=new HashMap<>();
        for(String field:FIELDS.keySet()) memberIds.put(field,new ArrayList<>());
        for(Map<String,Object> member:groups.members(id)) {
            FIELDS.forEach((field,role)->{ if(role.equals(member.get("roleCode")))
                memberIds.get(field).add(((Number)member.get("userId")).longValue()); });
        }
        result.putAll(memberIds);
        result.put("shopIds",groups.shops(id));
        return result;
    }

    @Transactional
    public Long save(Map<String,Object> input,String username) {
        String name=Objects.toString(input.get("groupName"),"").trim();
        if(name.isEmpty() || name.length()>100) throw new ServiceException("人员组名称应为1到100个字符");
        Long id=number(input.get("groupId"));
        Map<String,Object> value=new HashMap<>();
        value.put("groupName",name); value.put("username",username); value.put("groupId",id);
        if(id==null) { groups.insertGroup(value); id=number(value.get("groupId")); }
        else if(groups.updateGroup(value)!=1) throw new ServiceException("人员组不存在");
        groups.deleteMembers(id);
        for(var entry:FIELDS.entrySet()) for(Long userId:ids(input.get(entry.getKey()))) {
            if(groups.activeUser(userId)==0) throw new ServiceException("人员组包含停用或不存在的账号："+userId);
            groups.insertMember(id,userId,entry.getValue());
        }
        Set<Long> desired=ids(input.get("shopIds"));
        if(desired.size()>500) throw new ServiceException("店铺数量超出限制");
        for(Long shopId:desired) if(groups.activeShop(shopId)==0) throw new ServiceException("人员组包含停用或不存在的店铺："+shopId);
        for(Long shopId:groups.shops(id)) if(!desired.contains(shopId)) groups.unlinkShop(shopId);
        for(Long shopId:desired) {
            Long previous=groups.groupForShop(shopId);
            if(!id.equals(previous)) { groups.unlinkShop(shopId); groups.linkShop(shopId,id); }
            materialize(shopId,id,username);
        }
        return id;
    }

    @Transactional
    public void bind(Long shopId,Long groupId,String username) {
        if(groups.activeShop(shopId)==0) throw new ServiceException("店铺不存在或已停用");
        if(groupId!=null && groups.group(groupId)==null) throw new ServiceException("人员组不存在");
        groups.unlinkShop(shopId);
        if(groupId!=null) { groups.linkShop(shopId,groupId); materialize(shopId,groupId,username); }
    }

    @Transactional
    public void removeShop(Long shopId) {
        groups.unlinkShop(shopId);
    }

    @Transactional
    public void materialize(Long shopId,Long groupId,String username) {
        Set<Long> affected=new HashSet<>(groups.affectedUsers(shopId));
        gifts.deleteSubjectMappings("SHOP",shopId);
        for(Map<String,Object> member:groups.members(groupId)) {
            Long userId=((Number)member.get("userId")).longValue();
            gifts.insertSubjectMapping("SHOP",shopId,userId,(String)member.get("roleCode"),username);
            affected.add(userId);
        }
        access.reconcile(affected);
    }

    public Long groupForShop(Long shopId) { return groups.groupForShop(shopId); }
    public Set<Long> affectedUsers(Long shopId) { return new HashSet<>(groups.affectedUsers(shopId)); }
    public Set<Long> subjectUsers(String type,Long id) { return new HashSet<>(groups.subjectUsers(type,id)); }
    public void reconcile(Set<Long> users) { access.reconcile(users); }

    private static Long number(Object value) {
        if(value==null || value.toString().isBlank()) return null;
        try { return Long.valueOf(value.toString()); }
        catch(NumberFormatException ex) { throw new ServiceException("编号格式不正确"); }
    }
    private static Set<Long> ids(Object raw) {
        if(raw==null) return Set.of();
        if(!(raw instanceof Collection<?> values)) throw new ServiceException("人员列表格式不正确");
        if(values.size()>500) throw new ServiceException("人员数量超出限制");
        Set<Long> result=new LinkedHashSet<>();
        for(Object value:values) { Long id=number(value); if(id==null) throw new ServiceException("人员编号不能为空"); result.add(id); }
        return result;
    }
}
