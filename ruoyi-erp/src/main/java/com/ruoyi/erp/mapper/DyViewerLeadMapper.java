package com.ruoyi.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DyViewerLeadMapper
{
    int insertCaptureBatch(Map<String, Object> data);

    int upsertLiveRoom(Map<String, Object> data);

    List<Map<String, Object>> selectLiveRoomList(Map<String, Object> query);

    Map<String, Object> selectLiveRoomByKey(@Param("roomKey") String roomKey);

    int insertLiveRoom(Map<String, Object> data);

    int updateLiveRoom(Map<String, Object> data);

    int deleteLiveRoomByKey(@Param("roomKey") String roomKey);

    Long selectViewerIdBySecUid(@Param("secUid") String secUid);

    int insertViewer(Map<String, Object> data);

    int updateViewerSeen(Map<String, Object> data);

    int updateViewerCommentStats(@Param("viewerId") Long viewerId);

    Long selectLeadId(@Param("leadDate") String leadDate,
                      @Param("roomKey") String roomKey,
                      @Param("secUid") String secUid);

    int insertDailyLead(Map<String, Object> data);

    int updateDailyLeadSeen(Map<String, Object> data);

    Map<String, Object> selectLeadByNickname(@Param("leadDate") String leadDate,
                                             @Param("roomKey") String roomKey,
                                             @Param("nickname") String nickname);

    Map<String, Object> selectLeadByNicknameAnyRoom(@Param("leadDate") String leadDate,
                                                    @Param("nickname") String nickname);

    int insertComment(Map<String, Object> data);

    int updateLeadCommentStats(@Param("leadId") Long leadId,
                               @Param("content") String content,
                               @Param("capturedAt") String capturedAt);

    int bindUnmatchedComments(@Param("leadId") Long leadId,
                              @Param("viewerId") Long viewerId,
                              @Param("leadDate") String leadDate,
                              @Param("roomKey") String roomKey,
                              @Param("nickname") String nickname);

    int repairUnmatchedCommentBindings();

    int refreshLeadCommentStats(@Param("leadId") Long leadId);

    int syncDailyCommentStats();

    int syncViewerCommentStats();

    int releaseExpiredOwners();

    int syncLeadStatusRules();

    int syncViewerLeadRules();

    List<Map<String, Object>> selectLeadList(Map<String, Object> query);

    List<String> selectRoomNames(@Param("keyword") String keyword);

    List<String> selectOwnerNames(@Param("keyword") String keyword);

    Map<String, Object> selectLeadById(@Param("leadId") Long leadId);

    List<Map<String, Object>> selectVisitsByLeadId(@Param("leadId") Long leadId);

    List<Map<String, Object>> selectCommentsByLeadId(@Param("leadId") Long leadId);

    List<Map<String, Object>> selectFollowRecordsByLeadId(@Param("leadId") Long leadId);

    int updateLead(Map<String, Object> data);

    int updateViewerLead(Map<String, Object> data);

    int insertFollowRecord(Map<String, Object> data);

    Map<String, Object> selectSummary(Map<String, Object> query);

    Map<String, Object> selectBiOverview(Map<String, Object> query);

    List<Map<String, Object>> selectBiDailyTrend(Map<String, Object> query);

    List<Map<String, Object>> selectBiRoomRank(Map<String, Object> query);

    List<Map<String, Object>> selectBiShopRank(Map<String, Object> query);

    List<Map<String, Object>> selectBiOwnerRank(Map<String, Object> query);

    List<Map<String, Object>> selectBiHourHeat(Map<String, Object> query);

    List<Map<String, Object>> selectBiCommentKeywords(Map<String, Object> query);

    List<Map<String, Object>> selectBiSignalWords(Map<String, Object> query);

    List<Map<String, Object>> selectBiDataQuality(Map<String, Object> query);
}
