package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.erp.domain.DyCaptureReport;

public interface IDyViewerLeadService
{
    Map<String, Object> report(DyCaptureReport report);

    List<Map<String, Object>> listRooms(Map<String, Object> query);

    Map<String, Object> getRoom(String roomKey);

    int addRoom(Map<String, Object> form);

    int updateRoom(String roomKey, Map<String, Object> form);

    int deleteRoom(String roomKey);

    List<Map<String, Object>> listLeads(Map<String, Object> query);

    Map<String, Object> getLeadDetail(Long leadId);

    int updateLead(Long leadId, Map<String, Object> form);

    int addFollowRecord(Long leadId, Map<String, Object> form, Long operatorId, String operatorName);

    Map<String, Object> summary(Map<String, Object> query);
}
