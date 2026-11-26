package com.xinke.erp.service;

import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.system.service.ISysUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrator-managed personal settings with a version check and before/after audit. */
@Service
public class PersonnelFreightService {
    private final JdbcTemplate jdbc;
    private final ISysUserService users;
    private final FreightAccessService access;
    private final ApplicationEventPublisher events;
    public PersonnelFreightService(JdbcTemplate jdbc,ISysUserService users,FreightAccessService access,ApplicationEventPublisher events) {
        this.jdbc=jdbc;this.users=users;this.access=access;this.events=events;
    }
    public Map<String,Object> detail(Long id) {
        users.checkUserDataScope(id);
        if(users.selectUserById(id)==null) throw new ServiceException("人员不存在");
        var policy=access.policy(id);
        var result=new LinkedHashMap<String,Object>();
        result.put("policy",policy);
        result.put("shops",jdbc.queryForList("select shop_id shopId,shop_name shopName,status from erp_shop order by shop_code,shop_name"));
        result.put("sources",jdbc.queryForList("""
            select distinct r.role_name roleName,m.perms from sys_user_role ur
            join sys_role r on r.role_id=ur.role_id and r.status='0' and r.del_flag='0'
            join sys_role_menu rm on rm.role_id=r.role_id join sys_menu m on m.menu_id=rm.menu_id and m.status='0'
            where ur.user_id=? and m.perms like 'live:freight:%' order by r.role_name,m.perms
            """,id));
        result.put("history",jdbc.queryForList("""
            select a.id,a.before_config beforeConfig,a.after_config afterConfig,a.created_at createdAt,
              coalesce(u.nick_name,'已删除人员') actorName from live_personnel_access_audit a
            left join sys_user u on u.user_id=a.actor_id where a.user_id=? and a.business='FREIGHT' order by a.id desc limit 20
            """,id));
        return result;
    }
    public static FreightAccessService.Policy validated(Map<String,Object> body) {
        Set<String> actions=new TreeSet<>();
        if(!(body.get("actions") instanceof List<?> list)) throw new ServiceException("请选择功能权限");
        for(Object a:list) {
            if(!FreightAccessService.ACTIONS.contains(Objects.toString(a,""))) throw new ServiceException("未知功能权限");
            actions.add(a.toString());
        }
        if(!actions.isEmpty() && !actions.contains("view")) throw new ServiceException("使用运费功能需先允许查看明细");
        String view=Objects.toString(body.get("viewScope"),""),operate=Objects.toString(body.get("operateScope"),"");
        if(!Set.of("OWN","ASSIGNED","SELECTED","ALL").contains(view) || !Set.of("OWN","ASSIGNED","SELECTED","ALL").contains(operate))
            throw new ServiceException("请选择有效的数据范围");
        List<Long> vs=shopIds(body.get("viewShopIds")),os=shopIds(body.get("operateShopIds"));
        if("SELECTED".equals(view) && vs.isEmpty() || "SELECTED".equals(operate) && os.isEmpty()) throw new ServiceException("指定店铺范围不能为空");
        long version;
        try { version=Long.parseLong(Objects.toString(body.get("version"),"0")); }
        catch(NumberFormatException e) { throw new ServiceException("配置版本不正确，请刷新"); }
        if(version<0) throw new ServiceException("配置版本不正确，请刷新");
        return new FreightAccessService.Policy(actions,view,operate,"SELECTED".equals(view)?vs:List.of(),"SELECTED".equals(operate)?os:List.of(),version);
    }
    private static List<Long> shopIds(Object raw) {
        if(!(raw instanceof List<?> list) || list.size()>500) throw new ServiceException("店铺列表不正确");
        try {
            var ids=list.stream().map(x->Long.valueOf(x.toString())).distinct().sorted().toList();
            if(ids.stream().anyMatch(x->x<=0)) throw new IllegalArgumentException();
            return ids;
        } catch(Exception e) { throw new ServiceException("店铺列表不正确"); }
    }
    @Transactional
    public void save(Long id,Map<String,Object> body) {
        users.checkUserDataScope(id);
        var user=users.selectUserById(id);
        if(user==null || "2".equals(user.getDelFlag())) throw new ServiceException("人员不存在");
        users.checkUserAllowed(user);
        var next=validated(body);
        // Lock the account so two first-time saves cannot silently overwrite one another.
        jdbc.queryForObject("select user_id from sys_user where user_id=? for update",Long.class,id);
        var before=access.policy(id);
        if((before==null?0:before.version())!=next.version()) throw new ServiceException("配置已被其他人修改，请刷新后再保存");
        Set<Long> shops=new HashSet<>(next.viewShopIds());shops.addAll(next.operateShopIds());
        for(Long shop:shops) {
            if(jdbc.queryForObject("select count(*) from erp_shop where shop_id=? and status='0'",Integer.class,shop)!=1)
                throw new ServiceException("所选店铺不存在或已停用，请重新选择");
        }
        jdbc.update("""
            insert into live_personnel_freight_policy(user_id,actions,view_scope,operate_scope,view_shop_ids,operate_shop_ids,version,updated_by)
            values(?,?,?,?,?,?,1,?) on duplicate key update actions=values(actions),view_scope=values(view_scope),operate_scope=values(operate_scope),
            view_shop_ids=values(view_shop_ids),operate_shop_ids=values(operate_shop_ids),version=version+1,updated_by=values(updated_by),updated_at=now()
            """,id,String.join(",",new TreeSet<>(next.actions())),next.viewScope(),next.operateScope(),join(next.viewShopIds()),join(next.operateShopIds()),SecurityUtils.getUserId());
        // A dedicated role supplies navigation only. The policy above enforces every action and scope.
        String key="personnel_freight_"+id;
        jdbc.update("""
            insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
            select ?,?,98,'5','1','1','0','0',?,now(),'仅提供运费入口，业务权限由人员配置控制'
            where not exists(select 1 from sys_role where role_key=?)
            ""","运费个人配置-"+id,key,SecurityUtils.getUsername(),key);
        var roles=jdbc.queryForList("select role_id from sys_role where role_key=? and status='0' and del_flag='0'",Long.class,key);
        if(roles.size()!=1) throw new ServiceException("个人配置角色异常，请核查");
        Long role=roles.get(0);
        jdbc.update("delete from sys_role_menu where role_id=?",role);
        if(!next.actions().isEmpty()) {
            var menus=jdbc.queryForList("select menu_id,parent_id from sys_menu where component='live/freight/index' and status='0'");
            if(menus.size()!=1) throw new ServiceException("运费页面入口不存在或重复");
            var page=menus.get(0);
            for(Object menu:List.of(page.get("menu_id"),page.get("parent_id")))
                jdbc.update("insert ignore into sys_role_menu(role_id,menu_id) values(?,?)",role,menu);
            Long entry=jdbc.queryForObject("select menu_id from sys_menu where perms='live:freight:entry' and status='0'",Long.class);
            jdbc.update("insert ignore into sys_role_menu(role_id,menu_id) values(?,?)",role,entry);
            jdbc.update("insert ignore into sys_user_role(user_id,role_id) values(?,?)",id,role);
        } else jdbc.update("delete from sys_user_role where user_id=? and role_id=?",id,role);
        jdbc.update("insert into live_personnel_access_audit(user_id,business,before_config,after_config,actor_id) values(?,'FREIGHT',?,?,?)",
            id,JSON.toJSONString(before),JSON.toJSONString(access.policy(id)),SecurityUtils.getUserId());
        events.publishEvent(new LiveAccessChangedEvent(Set.of(id)));
    }
    private static String join(List<Long> ids) { return ids.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(",")); }
}
