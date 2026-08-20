package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import com.xinke.erp.domain.RpaBatchRequest;
import com.xinke.erp.domain.RpaRoomBindingRequest;
import com.xinke.erp.domain.RpaShopConfigRequest;
import com.xinke.erp.domain.RpaTaskClaimRequest;
import com.xinke.erp.domain.RpaTaskResultRequest;
import com.xinke.erp.domain.RpaTrackingConfigRequest;
import com.xinke.erp.domain.RpaViewerTrackingRequest;
import com.xinke.erp.domain.RpaBlacklistRequest;

public interface IRpaOutreachService
{
    void ensureSchema();

    Map<String, Object> health();

    void reclaimExpiredLeases();

    Map<String, Object> claim(RpaTaskClaimRequest request);

    Map<String, Object> heartbeat(RpaBatchRequest request);

    Map<String, Object> submitResult(RpaTaskResultRequest request);

    Map<String, Object> release(RpaBatchRequest request);

    List<Map<String, Object>> listShopConfigs();

    List<Map<String, Object>> listUnmappedRooms();

    long saveShopConfig(Long shopConfigId, RpaShopConfigRequest request);

    int bindRooms(Long shopConfigId, RpaRoomBindingRequest request);

    Map<String, Object> getTrackingConfig();

    Map<String, Object> updateTrackingConfig(RpaTrackingConfigRequest request);

    int updateViewerTracking(RpaViewerTrackingRequest request);

    List<Map<String, Object>> listWorkbench(Map<String, Object> query);

    Map<String, Object> workbenchStats(Map<String, Object> query);

    int enqueueViewers(List<Long> viewerIds);

    int blacklistViewers(RpaBlacklistRequest request, Long userId, String username);

    int restoreBlacklist(List<Long> blacklistIds);

    int mapRoomToShop(String roomKey, Long shopConfigId);

    int unmapRoom(String roomKey);
}
