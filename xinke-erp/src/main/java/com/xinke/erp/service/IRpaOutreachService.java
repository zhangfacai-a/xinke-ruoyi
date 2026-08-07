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

public interface IRpaOutreachService
{
    void ensureSchema();

    Map<String, Object> health();

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
}
