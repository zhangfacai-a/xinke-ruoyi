package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveMetricsMapper
{
    List<Map<String,Object>> listShifts(Map<String,Object> query);
    Map<String,Object> getShift(@Param("shiftId") Long shiftId);
    int insertShift(Map<String,Object> value);
    int updateShift(Map<String,Object> value);
    int countOverlappingShifts(Map<String,Object> value);
    int countOverlappingAnchorShifts(Map<String,Object> value);
    int countAnchorShiftsOnDate(Map<String,Object> value);
    int countShiftSubmissions(@Param("shiftId") Long shiftId);
    int deleteShift(@Param("shiftId") Long shiftId);

    List<Map<String,Object>> listRoomOptions();
    List<Map<String,Object>> listRoomOptionsForAnchor(@Param("userId") Long userId);
    List<Map<String,Object>> listStaffOptions();
    List<Long> listHiddenStaffUserIds(@Param("viewerUserId") Long viewerUserId);
    int deleteHiddenStaff(@Param("viewerUserId") Long viewerUserId);
    int insertHiddenStaff(@Param("viewerUserId") Long viewerUserId,@Param("hiddenUserIds") List<Long> hiddenUserIds);
    int countActiveUser(@Param("userId") Long userId);
    int countRoomAnchor(@Param("roomId") Long roomId,@Param("userId") Long userId);
    String getDingUserId(@Param("userId") Long userId);

    List<Map<String,Object>> listShiftTemplates(@Param("roomId") Long roomId);
    int countShiftTemplateName(@Param("roomId") Long roomId,@Param("templateName") String name,@Param("templateId") Long templateId);
    int insertShiftTemplate(Map<String,Object> value);
    int updateShiftTemplate(Map<String,Object> value);
    int deleteShiftTemplate(@Param("templateId") Long templateId);

    Map<String,Object> findRoomByAliasKey(@Param("aliasKey") String aliasKey);
    int insertRoomAlias(Map<String,Object> value);
    Map<String,Object> findLiveSession(@Param("roomNameKey") String roomNameKey,@Param("liveStartTime") String liveStartTime);
    Map<String,Object> findLiveSessionByRoom(@Param("roomId") Long roomId,@Param("liveStartTime") String liveStartTime);
    Map<String,Object> getLiveSession(@Param("sessionId") Long sessionId);
    int insertLiveSession(Map<String,Object> value);
    int bindLiveSessionRoom(Map<String,Object> value);
    List<Map<String,Object>> findAutoMatchShifts(@Param("roomId") Long roomId,
            @Param("sessionId") Long sessionId,@Param("liveStartTime") String liveStartTime,
            @Param("snapshotAt") String snapshotAt);

    Map<String,Object> getSubmissionByKey(@Param("idempotencyKey") String key);
    Map<String,Object> getSubmission(@Param("submissionId") Long submissionId);
    int insertRawSubmission(Map<String,Object> value);
    int countSessionSubmissions(@Param("sessionId") Long sessionId);
    int updateSubmissionShift(Map<String,Object> value);
    List<Map<String,Object>> listSessionSubmissions(@Param("sessionId") Long sessionId);
    List<Map<String,Object>> listRawSessionSubmissions(@Param("sessionId") Long sessionId);
    List<Map<String,Object>> listSessionShifts(@Param("sessionId") Long sessionId);
    int resetSessionCalculation(@Param("sessionId") Long sessionId);
    int resetShiftSubmissionStatus(@Param("sessionId") Long sessionId);
    int updateSubmissionCalculation(Map<String,Object> value);
    int markShiftSubmitted(@Param("shiftId") Long shiftId);
    int refreshShiftStatus(@Param("shiftId") Long shiftId);
    List<Map<String,Object>> listReportingRows(Map<String,Object> query);
    int insertCorrection(Map<String,Object> value);

    List<Map<String,Object>> findReminderCandidates();
    int insertReminderLog(Map<String,Object> value);
}
