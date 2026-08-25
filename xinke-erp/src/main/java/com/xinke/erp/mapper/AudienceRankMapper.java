package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.erp.domain.audience.AudienceRankBatch;
import com.xinke.erp.domain.audience.AudienceRankBatchQuery;
import com.xinke.erp.domain.audience.AudienceRankProfile;
import com.xinke.erp.domain.audience.AudienceRankQuery;
import com.xinke.erp.domain.audience.AudienceRankRoomMatch;
import com.xinke.erp.domain.audience.AudienceRankSnapshot;
import com.xinke.erp.domain.audience.AudienceRankSummary;
import com.xinke.erp.domain.audience.AudienceFollowup;
import com.xinke.erp.domain.audience.AudienceFollowupLog;
import com.xinke.erp.domain.audience.AudienceFollowupQuery;
import com.xinke.erp.domain.audience.AudienceVisitRecord;
import com.xinke.erp.domain.audience.AudienceAssignmentRule;
import com.xinke.erp.domain.audience.AudienceOpportunity;
import com.xinke.erp.domain.audience.AudienceCustomerOrder;

@Mapper
public interface AudienceRankMapper
{
    AudienceRankBatch selectBatchByHash(String payloadHash);

    AudienceRankBatch selectBatchById(Long batchId);

    List<AudienceRankBatch> selectBatchList(AudienceRankBatchQuery query);

    List<AudienceRankSnapshot> selectSnapshotList(AudienceRankQuery query);

    List<AudienceRankSnapshot> selectSnapshotsByBatchId(Long batchId);

    AudienceRankSummary selectSummary(AudienceRankQuery query);

    List<AudienceRankRoomMatch> selectMatchingRooms(String roomName);

    int insertAutoRoom(AudienceRankRoomMatch room);

    int updateBatchRoom(AudienceRankBatch batch);

    int supersedeCurrentBatches(AudienceRankBatch batch);

    int activateBatch(AudienceRankBatch batch);

    int insertBatch(AudienceRankBatch batch);

    int insertSnapshots(@Param("list") List<AudienceRankSnapshot> snapshots);

    int upsertProfiles(@Param("list") List<AudienceRankProfile> profiles);

    List<AudienceFollowup> selectFollowupList(AudienceFollowupQuery query);
    List<AudienceFollowup> selectFollowupVisitStats(@Param("secUids") List<String> secUids);
    List<AudienceVisitRecord> selectFollowupVisits(String secUid);
    AudienceFollowup selectFollowupById(Long followupId);
    AudienceFollowup selectFollowupByUid(@Param("secUid") String secUid);
    AudienceFollowup selectFollowupByOrderNo(@Param("orderNo") String orderNo,
                                              @Param("excludeFollowupId") Long excludeFollowupId);
    List<AudienceFollowupLog> selectFollowupLogs(Long followupId);
    int insertFollowups(@Param("list") List<AudienceFollowup> followups);
    int refreshFollowupSource(AudienceFollowup followup);
    int updateFollowup(AudienceFollowup followup);
    int insertFollowupLog(AudienceFollowupLog log);
    AudienceOpportunity selectCurrentOpportunity(Long followupId);
    List<AudienceOpportunity> selectOpportunities(Long followupId);
    int insertOpportunity(AudienceOpportunity opportunity);
    int updateOpportunity(AudienceOpportunity opportunity);
    int closeCurrentOpportunities(Long followupId);
    List<AudienceCustomerOrder> selectCustomerOrders(Long followupId);
    AudienceCustomerOrder selectCustomerOrderByNo(String orderNo);
    int insertCustomerOrder(AudienceCustomerOrder order);
    int updateCustomerOrder(AudienceCustomerOrder order);
    int markReactivationPending(@Param("followupId") Long followupId,
                                @Param("lastSeenAt") java.util.Date lastSeenAt);
    List<Map<String, Object>> selectFollowupSummary(AudienceFollowupQuery query);
    Map<String, Object> selectTeamOverview(AudienceFollowupQuery query);
    List<Map<String, Object>> selectTeamFunnel(AudienceFollowupQuery query);
    List<Map<String, Object>> selectOwnerPerformance(AudienceFollowupQuery query);
    List<Map<String, Object>> selectRoomPerformance(AudienceFollowupQuery query);
    List<Map<String, Object>> selectDailyTrend(AudienceFollowupQuery query);
    List<Map<String, Object>> selectFollowupRoomOptions();
    List<Map<String, Object>> selectFollowupAssignees(@Param("roomId") Long roomId,
                                                        @Param("roleCode") String roleCode);
    List<Map<String, Object>> selectAllActiveUsers();
    List<AudienceAssignmentRule> selectAssignmentRules();
    AudienceAssignmentRule selectAssignmentRuleByRoomId(Long roomId);
    List<Map<String, Object>> selectAssignmentRuleMembers(Long ruleId);
    int upsertAssignmentRule(AudienceAssignmentRule rule);
    int deleteAssignmentRuleMembers(Long ruleId);
    int insertAssignmentRuleMembers(@Param("ruleId") Long ruleId,
                                    @Param("userIds") List<Long> userIds);
    List<AudienceFollowup> selectReclaimableFollowups(@Param("roomId") Long roomId,
                                                       @Param("reclaimHours") Integer reclaimHours,
                                                       @Param("limit") Integer limit);
    List<AudienceFollowup> selectUnassignedForAutoAssign(@Param("roomId") Long roomId,
                                                          @Param("limit") Integer limit);
    int countActiveFollowupsByOwner(Long ownerUserId);
    int updateAssignmentRuleCursor(@Param("ruleId") Long ruleId,
                                   @Param("nextMemberIndex") Integer nextMemberIndex);
}
