package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveMetricsMapper
{
    List<Map<String,Object>> listRoomOptions();
    List<Map<String,Object>> listRoomAnchors(@Param("roomId") Long roomId);
    List<Map<String,Object>> listStaffOptions();
    Map<String,Object> findRoomByAliasKey(@Param("aliasKey") String aliasKey);
    int countActiveUser(@Param("userId") Long userId);
    int countRoomAnchor(@Param("roomId") Long roomId,@Param("userId") Long userId);
    int insertRoomAnchor(@Param("roomId") Long roomId,@Param("userId") Long userId,@Param("createBy") String createBy);
    int deleteRoomAnchor(@Param("roomId") Long roomId,@Param("userId") Long userId);
    Map<String,Object> findHandoverSessionByPlatformId(@Param("platformSessionId") String platformSessionId);
    Map<String,Object> getHandoverSession(@Param("sessionId") Long sessionId);
    Map<String,Object> lockHandoverSession(@Param("sessionId") Long sessionId);
    int insertHandoverSession(Map<String,Object> value);
    int updateHandoverSessionSnapshot(@Param("sessionId") Long sessionId,@Param("capturedAt") String capturedAt);
    int resetHandoverSessionSnapshot(@Param("sessionId") Long sessionId,@Param("capturedAt") String capturedAt);
    Map<String,Object> getHandoverSnapshotByKey(@Param("idempotencyKey") String idempotencyKey);
    Map<String,Object> getLatestHandoverSnapshot(@Param("sessionId") Long sessionId);
    Map<String,Object> getHandoverSnapshot(@Param("snapshotId") Long snapshotId);
    int insertHandoverSnapshot(Map<String,Object> value);
    Map<String,Object> getHandoverSegment(@Param("segmentId") Long segmentId);
    int insertHandoverSegment(Map<String,Object> value);
    int countHandoverSegmentOverlap(@Param("sessionId") Long sessionId,@Param("startTime") Object startTime,@Param("endTime") Object endTime,@Param("excludeSegmentId") Long excludeSegmentId);
    int updateHandoverSegment(Map<String,Object> value);
    int voidHandoverSegment(Map<String,Object> value);
    List<Map<String,Object>> listHandoverRooms();
    Map<String,Object> getHandoverRoom(@Param("roomId") Long roomId);
    List<Map<String,Object>> listHandoverSessions(Map<String,Object> query);
    List<Map<String,Object>> listHandoverSegments(@Param("sessionId") Long sessionId);
    List<Map<String,Object>> listHandoverSnapshots(@Param("sessionId") Long sessionId);
    List<Map<String,Object>> listHandoverCorrections(@Param("sessionId") Long sessionId);
    int insertHandoverCorrection(Map<String,Object> value);
    @org.apache.ibatis.annotations.Select("select room_id roomId from live_room where room_id=#{roomId} and status='0' for update")
    Map<String,Object> lockRoom(@Param("roomId") Long roomId);
    @org.apache.ibatis.annotations.Select("select count(*) from live_subject_user_map m join live_room r on r.room_id=m.subject_id and r.status='0' join sys_user u on u.user_id=m.user_id and u.status='0' and u.del_flag='0' where m.subject_type='ROOM' and m.role_code='anchor' and m.user_id=#{userId}")
    int countActiveAnchorRooms(Long userId);
    @org.apache.ibatis.annotations.Insert("insert ignore into sys_user_role(user_id,role_id) select #{userId},role_id from sys_role where role_key='auto_live_anchor_app' and status='0' and del_flag='0'")
    int grantAppRole(Long userId);
    @org.apache.ibatis.annotations.Delete("delete ur from sys_user_role ur join sys_role r on r.role_id=ur.role_id where ur.user_id=#{userId} and r.role_key='auto_live_anchor_app'")
    int revokeAppRole(Long userId);
    @org.apache.ibatis.annotations.Insert("insert into live_metric_room_alias(room_id,alias_name,alias_key,create_by,create_time) values(#{roomId},#{name},#{key},#{operator},now())")
    int bindAlias(@Param("roomId") Long roomId,@Param("name") String name,@Param("key") String key,@Param("operator") String operator);
}
