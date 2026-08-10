package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RpaOutreachMapper
{
    int createShopConfigTable();
    int createRoomShopTable();
    int createTaskTable();
    int createBatchTable();
    int createTrackingConfigTable();
    int createViewerTrackingRuleTable();
    int createViewerBlacklistTable();
    int initializeTrackingConfig();
    int upgradeDefaultTrackingLookback();
    int upgradeRoomShopKeyLength();
    int upgradeTaskSecUidLength();

    int releaseExpiredTasks();
    int expireBatches();
    int removeIneligiblePendingTasks();
    int prepareTasks();
    Map<String, Object> selectClaimableShop(@Param("preferredShopCode") String preferredShopCode);
    int insertBatch(Map<String, Object> data);
    List<Long> selectPendingTaskIds(@Param("shopConfigId") Long shopConfigId, @Param("limit") int limit);
    int leaseTasks(Map<String, Object> data);
    int updateBatchTaskCount(@Param("batchNo") String batchNo, @Param("taskCount") int taskCount);
    Map<String, Object> selectBatch(@Param("batchNo") String batchNo);
    List<Map<String, Object>> selectBatchTasks(@Param("batchNo") String batchNo);
    int heartbeatBatch(Map<String, Object> data);
    int heartbeatTasks(Map<String, Object> data);
    Map<String, Object> selectTaskForUpdate(@Param("taskNo") String taskNo);
    Map<String, Object> selectTaskByRequestId(@Param("requestId") String requestId);
    int updateTaskResult(Map<String, Object> data);
    int refreshBatchProgress(@Param("batchNo") String batchNo);
    int markViewerOrdered(Map<String, Object> data);
    int markViewerContacted(Map<String, Object> data);
    int insertOutreachFollowRecord(Map<String, Object> data);
    int releaseBatchTasks(Map<String, Object> data);
    int markBatchReleased(Map<String, Object> data);

    List<Map<String, Object>> selectShopConfigList();
    List<Map<String, Object>> selectUnmappedRooms();
    int insertShopConfig(Map<String, Object> data);
    int updateShopConfig(Map<String, Object> data);
    Map<String, Object> selectShopConfigById(@Param("shopConfigId") Long shopConfigId);
    int deleteRoomBindings(@Param("shopConfigId") Long shopConfigId);
    int upsertRoomBindings(@Param("shopConfigId") Long shopConfigId, @Param("roomKeys") List<String> roomKeys);

    Map<String, Object> selectTrackingConfig();
    int updateTrackingConfig(Map<String, Object> data);
    int deleteViewerTrackingRules(@Param("viewerIds") List<Long> viewerIds);
    int insertViewerTrackingRules(Map<String, Object> data);
    int deletePendingTasksByViewerIds(@Param("viewerIds") List<Long> viewerIds);
    int prepareTasksForViewerIds(@Param("viewerIds") List<Long> viewerIds);
    int resetCancelledTasksForViewerIds(@Param("viewerIds") List<Long> viewerIds);
    List<Map<String, Object>> selectWorkbenchCandidates(Map<String, Object> query);
    List<Map<String, Object>> selectWorkbenchTasks(Map<String, Object> query);
    List<Map<String, Object>> selectWorkbenchBlacklist(Map<String, Object> query);
    Map<String, Object> selectWorkbenchStats(Map<String, Object> query);
    int insertViewerBlacklist(Map<String, Object> data);
    int cancelBlacklistedTasks(Map<String, Object> data);
    int closeBatchesWithoutActiveTasks();
    int deleteBlacklistByIds(@Param("blacklistIds") List<Long> blacklistIds);
    int deleteRoomBinding(@Param("roomKey") String roomKey);
}
