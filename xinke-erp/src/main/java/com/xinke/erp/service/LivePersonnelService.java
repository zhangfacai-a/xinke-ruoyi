package com.xinke.erp.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.system.service.ISysUserService;

/** Read model for account assignments and the sources of effective access. */
@Service
public class LivePersonnelService {
    private final JdbcTemplate jdbc;
    private final ISysUserService users;
    public LivePersonnelService(JdbcTemplate jdbc, ISysUserService users) { this.jdbc=jdbc; this.users=users; }

    public List<Map<String,Object>> summaries(List<Long> ids) {
        if(ids==null || ids.isEmpty()) return List.of();
        if(ids.size()>100) throw new ServiceException("单次最多查询100名人员");
        ids.forEach(users::checkUserDataScope);
        String marks=String.join(",",Collections.nCopies(ids.size(),"?"));
        return jdbc.queryForList("""
            select u.user_id userId,u.local_disabled localDisabled,
              (select count(*) from live_subject_user_map m where m.user_id=u.user_id) assignmentCount,
              (select group_concat(distinct m.role_code order by m.role_code) from live_subject_user_map m where m.user_id=u.user_id) businessRoles,
              case when u.status<>'0' and exists(select 1 from live_subject_user_map m where m.user_id=u.user_id) then 1 else 0 end inactiveAssignment,
              case when not exists(select 1 from sys_user_role ur where ur.user_id=u.user_id) then 1 else 0 end noRole
            from sys_user u where u.user_id in (
            """+marks+")",ids.toArray());
    }

    public Map<String,Object> detail(Long id) {
        users.checkUserDataScope(id);
        if(users.selectUserById(id)==null) throw new ServiceException("账号不存在");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("assignments",jdbc.queryForList("""
            select m.subject_type subjectType,m.subject_id subjectId,m.role_code roleCode,
              case when m.subject_type='SHOP' then s.shop_name else r.room_name end subjectName,
              case when m.subject_type='SHOP' then s.status else r.status end subjectStatus,g.group_name groupName
            from live_subject_user_map m
            left join erp_shop s on m.subject_type='SHOP' and s.shop_id=m.subject_id
            left join live_room r on m.subject_type='ROOM' and r.room_id=m.subject_id
            left join live_shop_staff_group_shop gs on m.subject_type='SHOP' and gs.shop_id=m.subject_id
            left join live_shop_staff_group g on g.group_id=gs.group_id
            where m.user_id=? order by m.subject_type,m.subject_id,m.role_code
            """,id));
        result.put("roles",jdbc.queryForList("""
            select r.role_name roleName,r.role_key roleKey,r.status,r.data_scope dataScope,
              case when r.role_key like 'auto%' then '业务自动授权' else '系统角色 / 特殊授权' end source
            from sys_user_role ur join sys_role r on r.role_id=ur.role_id and r.del_flag='0'
            where ur.user_id=? order by r.role_sort
            """,id));
        result.put("pages",jdbc.queryForList("""
            select distinct m.menu_name menuName,m.perms,r.role_name roleName,r.role_key roleKey
            from sys_user_role ur join sys_role r on r.role_id=ur.role_id and r.status='0' and r.del_flag='0'
            join sys_role_menu rm on rm.role_id=r.role_id join sys_menu m on m.menu_id=rm.menu_id
            where ur.user_id=? and m.menu_type='C' and m.status='0' order by m.menu_name,r.role_name
            """,id));
        result.put("expectedAutoRoles",jdbc.queryForList("""
            select distinct r.role_key roleKey,r.role_name roleName,
              exists(select 1 from sys_user_role ur where ur.user_id=? and ur.role_id=r.role_id) granted
            from live_auto_access_rule a join sys_role r on r.role_id=a.grant_role_id and r.status='0' and r.del_flag='0'
            join live_subject_user_map m on m.subject_type=a.subject_type and m.role_code=a.role_code
            join sys_user u on u.user_id=m.user_id and u.status='0' and u.del_flag='0'
            left join erp_shop s on a.subject_type='SHOP' and s.shop_id=m.subject_id and s.status='0'
            left join live_room room on a.subject_type='ROOM' and room.room_id=m.subject_id and room.status='0'
            where m.user_id=? and (s.shop_id is not null or room.room_id is not null)
            """,id,id));
        result.put("pendingRetention",jdbc.queryForObject("select count(*) from live_retention_order where owner_id=? and progress<>'COMPLETED'",Long.class,id));
        result.put("pendingFollowups",jdbc.queryForObject("select count(*) from dy_audience_v2_followup_task where assignee_user_id=? and status not in ('DONE','RELEASED')",Long.class,id));
        result.put("localDisabled",jdbc.queryForObject("select local_disabled from sys_user where user_id=?",Integer.class,id));
        result.put("isSuperAdmin",Long.valueOf(1).equals(id));
        result.put("publicAccess",List.of("京东客户信息：所有正常登录账号可用"));
        return result;
    }
}
