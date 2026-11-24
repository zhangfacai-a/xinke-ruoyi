package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ShopStaffGroupMapper {
    @Select("select g.group_id groupId,g.group_name groupName,(select count(*) from live_shop_staff_group_shop s where s.group_id=g.group_id) shopCount,(select count(distinct user_id) from live_shop_staff_group_member m where m.group_id=g.group_id) memberCount from live_shop_staff_group g order by group_name")
    List<Map<String,Object>> groups();

    @Select("select group_id groupId,group_name groupName from live_shop_staff_group where group_id=#{id}")
    Map<String,Object> group(Long id);

    @Select("select group_id groupId,user_id userId,role_code roleCode from live_shop_staff_group_member where group_id=#{id}")
    List<Map<String,Object>> members(Long id);

    @Select("select shop_id shopId from live_shop_staff_group_shop where group_id=#{id}")
    List<Long> shops(Long id);

    @Select("select group_id from live_shop_staff_group_shop where shop_id=#{shopId}")
    Long groupForShop(Long shopId);

    @Insert("insert into live_shop_staff_group(group_name,created_by) values(#{groupName},#{username})")
    @Options(useGeneratedKeys=true,keyProperty="groupId")
    int insertGroup(Map<String,Object> value);

    @Update("update live_shop_staff_group set group_name=#{groupName},updated_by=#{username},updated_at=now() where group_id=#{groupId}")
    int updateGroup(Map<String,Object> value);

    @Delete("delete from live_shop_staff_group_member where group_id=#{id}")
    int deleteMembers(Long id);

    @Insert("insert into live_shop_staff_group_member(group_id,user_id,role_code) values(#{groupId},#{userId},#{roleCode})")
    int insertMember(@Param("groupId") Long groupId,@Param("userId") Long userId,@Param("roleCode") String roleCode);

    @Delete("delete from live_shop_staff_group_shop where shop_id=#{shopId}")
    int unlinkShop(Long shopId);

    @Insert("insert into live_shop_staff_group_shop(shop_id,group_id) values(#{shopId},#{groupId})")
    int linkShop(@Param("shopId") Long shopId,@Param("groupId") Long groupId);

    @Select("select distinct user_id from live_subject_user_map where subject_type='SHOP' and subject_id=#{shopId}")
    List<Long> affectedUsers(Long shopId);

    @Select("select distinct user_id from live_subject_user_map where subject_type=#{type} and subject_id=#{id}")
    List<Long> subjectUsers(@Param("type") String type,@Param("id") Long id);

    @Select("select distinct user_id from live_subject_user_map")
    List<Long> allStaffUsers();

    @Select("select role_id from sys_role where role_key='auto_live_retention' and status='0' and del_flag='0'")
    Long retentionRole();

    @Select("select distinct a.grant_role_id from live_auto_access_rule a join sys_role r on r.role_id=a.grant_role_id and r.status='0' and r.del_flag='0'")
    List<Long> managedRoles();

    @Select("select role_id from sys_role where role_key='auto_live_authenticated' and status='0' and del_flag='0'")
    Long authenticatedRole();

    @Select("select user_id from sys_user where status='0' and del_flag='0'")
    List<Long> allActiveUsers();

    @Select("select count(*) from live_auto_access_rule r join live_subject_user_map m on m.subject_type=r.subject_type and m.role_code=r.role_code and m.user_id=#{userId} join sys_user u on u.user_id=m.user_id and u.status='0' and u.del_flag='0' left join erp_shop s on r.subject_type='SHOP' and s.shop_id=m.subject_id and s.status='0' left join live_room room on r.subject_type='ROOM' and room.room_id=m.subject_id and room.status='0' where r.grant_role_id=#{roleId} and (s.shop_id is not null or room.room_id is not null)")
    int eligible(@Param("userId") Long userId,@Param("roleId") Long roleId);

    @Select("select count(*) from sys_user_role where user_id=#{userId} and role_id=#{roleId}")
    int hasRole(@Param("userId") Long userId,@Param("roleId") Long roleId);

    @Insert("insert into sys_user_role(user_id,role_id) values(#{userId},#{roleId})")
    int grant(@Param("userId") Long userId,@Param("roleId") Long roleId);

    @Delete("delete from sys_user_role where user_id=#{userId} and role_id=#{roleId}")
    int revoke(@Param("userId") Long userId,@Param("roleId") Long roleId);

    @Select("select user_id from sys_user_role where role_id=#{roleId}")
    List<Long> roleUsers(Long roleId);

    @Select("select count(*) from sys_user where user_id=#{userId} and status='0' and del_flag='0'")
    int activeUser(Long userId);

    @Select("select count(*) from erp_shop where shop_id=#{shopId} and status<>'2'")
    int activeShop(Long shopId);
}
