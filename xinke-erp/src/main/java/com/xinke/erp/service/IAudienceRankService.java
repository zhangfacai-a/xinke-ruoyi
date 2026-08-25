package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import java.util.Date;
import com.xinke.erp.domain.audience.AudienceRankBatch;
import com.xinke.erp.domain.audience.AudienceRankBatchDetail;
import com.xinke.erp.domain.audience.AudienceRankBatchQuery;
import com.xinke.erp.domain.audience.AudienceRankImportRequest;
import com.xinke.erp.domain.audience.AudienceRankImportResult;
import com.xinke.erp.domain.audience.AudienceRankQuery;
import com.xinke.erp.domain.audience.AudienceRankSnapshot;
import com.xinke.erp.domain.audience.AudienceRankSummary;
import com.xinke.erp.domain.audience.AudienceFollowup;
import com.xinke.erp.domain.audience.AudienceFollowupLog;
import com.xinke.erp.domain.audience.AudienceFollowupQuery;
import com.xinke.erp.domain.audience.AudienceVisitRecord;
import com.xinke.erp.domain.audience.AudienceAssignmentRule;
import com.xinke.erp.domain.audience.AudienceCustomerOrder;

public interface IAudienceRankService
{
    AudienceRankImportResult importRanks(AudienceRankImportRequest request, String uploadedIp);

    List<AudienceRankSnapshot> selectSnapshotList(AudienceRankQuery query);

    List<AudienceRankBatch> selectBatchList(AudienceRankBatchQuery query);

    AudienceRankBatchDetail selectBatchDetail(Long batchId);

    AudienceRankSummary selectSummary(AudienceRankQuery query);

    List<AudienceFollowup> selectFollowupList(AudienceFollowupQuery query, Long currentUserId);
    AudienceFollowup selectFollowup(Long followupId);
    AudienceFollowup selectFollowup(Long followupId, Long currentUserId);
    List<AudienceFollowupLog> selectFollowupLogs(Long followupId);
    List<AudienceFollowupLog> selectFollowupLogs(Long followupId, Long currentUserId);
    List<AudienceVisitRecord> selectFollowupVisits(Long followupId, Long currentUserId);
    List<Map<String, Object>> selectFollowupSummary(AudienceFollowupQuery query, Long currentUserId);
    Map<String, Object> selectTeamDashboard(AudienceFollowupQuery query);
    List<Map<String, Object>> selectFollowupRooms();
    List<Map<String, Object>> selectFollowupAssignees(Long roomId, String roleCode);
    List<AudienceAssignmentRule> selectAssignmentRules();
    AudienceAssignmentRule saveAssignmentRule(AudienceAssignmentRule rule, Long operatorUserId, String operatorName);
    Map<String, Object> autoAssignFollowups(Long roomId, Long operatorUserId, String operatorName);
    void updateFollowup(AudienceFollowup followup, Long operatorUserId, String operatorName);
    void claimFollowup(Long followupId, Long operatorUserId, String operatorName);
    void updateFollowupStatus(Long followupId, String status, String content, Date nextFollowAt,
                              Long operatorUserId, String operatorName);
    void batchUpdateFollowups(List<Long> followupIds, Map<String, Object> changes,
                              Long operatorUserId, String operatorName);
    AudienceFollowup reactivateFollowup(Long followupId, Long operatorUserId, String operatorName);
    List<AudienceCustomerOrder> selectCustomerOrders(Long followupId, Long currentUserId);
    AudienceCustomerOrder saveCustomerOrder(Long followupId, AudienceCustomerOrder order,
                                            Long operatorUserId, String operatorName);
}
