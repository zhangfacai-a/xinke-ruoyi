package com.xinke.erp.livecrm.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveCrmMapper {
    Map<String,Object> findOpenSession(@Param("roomNameKey") String roomNameKey, @Param("liveDate") String liveDate);
    Map<String,Object> findRoomByNameKey(@Param("roomNameKey") String roomNameKey);
    int insertDiscoveredRoom(Map<String,Object> value);
    int syncDiscoveredRooms();
    Map<String,Object> findRoomById(@Param("roomId") Long roomId);
    int deleteRoomAttachments(@Param("roomNameKey") String roomNameKey);
    int deleteRoomOrders(@Param("roomNameKey") String roomNameKey);
    int deleteRoomTasks(@Param("roomNameKey") String roomNameKey);
    int deleteRoomOpportunities(@Param("roomNameKey") String roomNameKey);
    int deleteRoomRegistrations(@Param("roomNameKey") String roomNameKey);
    int deleteRoomInteractions(@Param("roomNameKey") String roomNameKey);
    int deleteRoomPresence(@Param("roomNameKey") String roomNameKey);
    int deleteRoomBatches(@Param("roomNameKey") String roomNameKey);
    int deleteRoomSessions(@Param("roomNameKey") String roomNameKey);
    int deleteRoomStaff(@Param("roomId") Long roomId);
    int deleteRoom(@Param("roomId") Long roomId);
    Map<String,Object> getSession(Long id);
    int insertSession(Map<String,Object> value);
    List<Map<String,Object>> listSessions(@Param("limit") int limit);
    List<Map<String,Object>> listRoomOptions();
    List<Map<String,Object>> listEnabledUsers();
    Map<String,Object> findEnabledUser(Long userId);
    List<Map<String,Object>> listRoomStaff(@Param("roomNameKey") String roomNameKey);
    Map<String,Object> findCustomerByUid(String secUid);
    Map<String,Object> findCustomerByNickname(String nicknameKey);
    List<Map<String,Object>> searchCustomerSuggestions(@Param("keyword") String keyword,
                                                       @Param("limit") int limit);
    Map<String,Object> findIdentity(@Param("customerId") Long customerId, @Param("secUid") String secUid, @Param("nicknameKey") String nicknameKey);
    long countDistinctIdentityUids(@Param("customerId") Long customerId);
    int markSameNameMultiIdentity(Long customerId);
    int insertCustomer(Map<String,Object> value);
    int updateCustomer(Map<String,Object> value);
    Map<String,Object> lockCustomerForUpdate(@Param("customerId") Long customerId);
    int claimCustomerIfUnassigned(@Param("customerId") Long customerId,
                                  @Param("ownerUserId") Long ownerUserId,
                                  @Param("ownerName") String ownerName);
    int synchronizeActiveOpportunityOwners(@Param("customerId") Long customerId,
                                           @Param("ownerUserId") Long ownerUserId,
                                           @Param("ownerName") String ownerName);
    int transferCustomerOwner(@Param("customerId") Long customerId,
                              @Param("ownerUserId") Long ownerUserId,
                              @Param("ownerName") String ownerName);
    int synchronizeOpenTaskAssignees(@Param("customerId") Long customerId,
                                     @Param("ownerUserId") Long ownerUserId,
                                     @Param("ownerName") String ownerName);
    int updateCustomerOwnerByOpportunity(@Param("opportunityId") Long opportunityId,
                                         @Param("ownerUserId") Long ownerUserId,
                                         @Param("ownerName") String ownerName);
    int insertIdentity(Map<String,Object> value);
    int touchIdentity(Map<String,Object> value);
    int insertBatch(Map<String,Object> value);
    int updateBatch(Map<String,Object> value);
    int insertInteraction(Map<String,Object> value);
    int insertPresence(Map<String,Object> value);
    int updatePresence(Map<String,Object> value);
    List<Map<String,Object>> listWorkbench(@Param("keyword") String keyword, @Param("stage") String stage,
                                            @Param("claimedOnly") Boolean claimedOnly,
                                            @Param("ownerUserId") Long ownerUserId,
                                            @Param("sessionId") Long sessionId, @Param("beginDate") String beginDate,
                                            @Param("endDate") String endDate, @Param("offset") int offset,
                                            @Param("limit") int limit);
    long countWorkbench(@Param("keyword") String keyword, @Param("stage") String stage,
                        @Param("claimedOnly") Boolean claimedOnly,
                        @Param("ownerUserId") Long ownerUserId,
                        @Param("sessionId") Long sessionId, @Param("beginDate") String beginDate,
                        @Param("endDate") String endDate);
    /**
     * Actionable customer inbox used by the new customer center.  Unlike the
     * legacy workbench query this keeps one row per customer and exposes the
     * current task/order state so the UI does not need to join or count data.
     */
    List<Map<String,Object>> listInbox(@Param("keyword") String keyword, @Param("stage") String stage,
                                       @Param("sessionId") Long sessionId, @Param("filter") String filter,
                                       @Param("interactionType") String interactionType,
                                       @Param("beginDate") String beginDate, @Param("endDate") String endDate,
                                       @Param("offset") int offset, @Param("limit") int limit);
    long countInbox(@Param("keyword") String keyword, @Param("stage") String stage,
                    @Param("sessionId") Long sessionId, @Param("filter") String filter,
                    @Param("interactionType") String interactionType,
                    @Param("beginDate") String beginDate, @Param("endDate") String endDate);
    Map<String,Object> inboxSummary(@Param("sessionId") Long sessionId,
                                    @Param("interactionType") String interactionType,
                                    @Param("beginDate") String beginDate,
                                    @Param("endDate") String endDate);
    Map<String,Object> workbenchSummary(@Param("sessionId") Long sessionId);
    Map<String,Object> getCustomer(Long customerId);
    List<Map<String,Object>> listCustomerIdentities(Long customerId);
    List<Map<String,Object>> listCustomerInteractions(Long customerId);
    List<Map<String,Object>> listCustomerRegistrations(Long customerId);
    List<Map<String,Object>> listCustomerOpportunities(Long customerId);
    List<Map<String,Object>> listCustomerOrders(Long customerId);
    List<Map<String,Object>> listCustomerAttachments(Long customerId);
    List<Map<String,Object>> listCustomerTasks(Long customerId);
    Map<String,Object> getAttachment(Long id);
    int insertAttachment(Map<String,Object> value);
    int completeAttachment(@Param("id") Long id, @Param("sha256") String sha256);
    int updateAttachmentObjectKey(@Param("id") Long id, @Param("objectKey") String objectKey);
    int markAttachmentFailed(@Param("id") Long id, @Param("reason") String reason);
    List<Map<String,Object>> listReadyAttachmentsForCustomers(@Param("customerIds") List<Long> customerIds);
    int insertRegistration(Map<String,Object> value);
    List<Map<String,Object>> listPhoneHandoffCandidates();
    Map<String,Object> findOpenPhoneTask(@Param("opportunityId") Long opportunityId);
    int markOpportunityHandoff(Map<String,Object> value);
    int insertPhoneTask(Map<String,Object> value);
    Map<String,Object> findOpenOpportunity(@Param("customerId") Long customerId, @Param("sessionId") Long sessionId);
    int insertOpportunity(Map<String,Object> value);
    int updateOpportunity(Map<String,Object> value);
    Map<String,Object> findOpenTask(@Param("opportunityId") Long opportunityId);
    int insertTask(Map<String,Object> value);
    int updateTaskDate(@Param("id") Long id, @Param("taskDate") String taskDate);
    Map<String,Object> getTask(Long id);
    int updateTask(Map<String,Object> value);
    Map<String,Object> findOrderByOpportunity(@Param("opportunityId") Long opportunityId);
    Map<String,Object> findOrderByOrderNo(@Param("orderNo") String orderNo);
    int insertOrder(Map<String,Object> value);
    int updateOrder(Map<String,Object> value);
    Map<String,Object> getOrder(Long id);
    List<Map<String,Object>> listTasks(@Param("offset") int offset, @Param("limit") int limit);
    long countTasks();
    List<Map<String,Object>> listOrders(@Param("offset") int offset, @Param("limit") int limit);
    long countOrders();
    List<Map<String,Object>> listBatches(@Param("offset") int offset, @Param("limit") int limit);
    long countBatches();
    Map<String,Object> getBatch(Long id);
    List<Map<String,Object>> listBatchEvents(Long id);
}
