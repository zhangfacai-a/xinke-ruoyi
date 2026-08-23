package com.xinke.erp.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveGiftMapper
{
    List<Map<String, Object>> selectLiveUserOptions(Map<String, Object> query);
    Map<String, Object> selectDingDepartmentBinding(Long dingDeptId);
    Map<String, Object> selectSystemDepartment(@Param("parentId") Long parentId,
                                                @Param("deptName") String deptName);
    int insertSystemDepartment(Map<String, Object> department);
    int updateSystemDepartment(Map<String, Object> department);
    int upsertDingDepartmentBinding(Map<String, Object> binding);
    int disableMissingDingDepartments(@Param("syncBatch") String syncBatch,
                                      @Param("username") String username);
    Map<String, Object> selectDingUserBinding(String dingUserId);
    Map<String, Object> selectSystemUserByUserName(String userName);
    int insertSystemUser(Map<String, Object> user);
    int updateSystemUser(Map<String, Object> user);
    int touchDingUserBinding(@Param("dingUserId") String dingUserId,
                             @Param("syncBatch") String syncBatch);
    int upsertDingUserBinding(Map<String, Object> binding);
    int deleteDingUserBinding(String dingUserId);
    int disableMissingDingUsers(@Param("syncBatch") String syncBatch,
                                @Param("username") String username);
    List<Map<String, Object>> selectShopOptions();
    List<Map<String, Object>> selectShops(Map<String, Object> query);
    Map<String, Object> selectShopById(Long shopId);
    int insertShop(Map<String, Object> shop);
    int updateShop(Map<String, Object> shop);
    List<Map<String, Object>> selectRooms(Map<String, Object> query);
    Map<String, Object> selectRoomById(Long roomId);
    int insertRoom(Map<String, Object> room);
    int updateRoom(Map<String, Object> room);
    List<Map<String, Object>> selectSubjectMappings(Map<String, Object> query);
    int deleteSubjectMappings(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId);
    int insertSubjectMapping(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId,
                             @Param("userId") Long userId, @Param("roleCode") String roleCode,
                             @Param("username") String username);
    List<Map<String, Object>> selectDailyRecords(Map<String, Object> query);
    Map<String, Object> selectDailyById(Long dailyId);
    int insertDaily(Map<String, Object> daily);
    int updateDaily(Map<String, Object> daily);
    int deleteDailyStaff(Long dailyId);
    int insertDailyStaff(@Param("dailyId") Long dailyId, @Param("userId") Long userId,
                         @Param("roleCode") String roleCode);

    List<Map<String, Object>> selectGifts(Map<String, Object> query);
    Map<String, Object> selectGiftById(Long giftId);
    Map<String, Object> selectGiftByCode(String giftCode);
    Map<String, Object> selectGiftByName(@Param("giftName") String giftName, @Param("excludeGiftId") Long excludeGiftId);
    int insertGift(Map<String, Object> gift);
    int updateGift(Map<String, Object> gift);
    int deleteGiftAliases(Long giftId);
    int insertGiftAlias(@Param("giftId") Long giftId, @Param("aliasName") String aliasName);
    int insertGiftVersion(@Param("giftId") Long giftId, @Param("actionType") String actionType,
                          @Param("username") String username);
    List<Map<String, Object>> selectGiftVersions(Long giftId);
    List<Map<String, Object>> selectGiftCosts(Long giftId);
    Map<String, Object> selectGiftCostByDate(@Param("giftId") Long giftId,
                                             @Param("effectiveDate") LocalDate effectiveDate);
    Map<String, Object> selectApplicableCost(@Param("giftId") Long giftId, @Param("costDate") LocalDate costDate);
    int insertGiftCost(Map<String, Object> cost);
    int upsertGiftPreference(Map<String, Object> preference);

    List<Map<String, Object>> selectQuickTemplates(Long userId);
    Map<String, Object> selectQuickTemplate(@Param("templateId") Long templateId, @Param("userId") Long userId);
    int insertQuickTemplate(Map<String, Object> template);
    int updateQuickTemplate(Map<String, Object> template);
    int deleteQuickTemplate(@Param("templateId") Long templateId, @Param("userId") Long userId);

    Map<String, Object> selectOrderByNo(String orderNo);
    Map<String, Object> selectOrderGiftStatus(String orderNo);
    List<Map<String, Object>> selectOrderGiftItems(String orderNo);
    int upsertOrderGiftStatus(Map<String, Object> status);
    int deleteOrderGiftItems(String orderNo);
    int insertOrderGift(Map<String, Object> item);
    int insertOrderGiftLog(@Param("orderNo") String orderNo, @Param("actionType") String actionType,
                           @Param("detailJson") String detailJson, @Param("operatorName") String operatorName);
    Map<String, Object> selectUserRoomPreference(Long userId);
    int upsertUserRoomPreference(@Param("userId") Long userId, @Param("roomId") Long roomId, @Param("username") String username);
    List<Map<String, Object>> selectGiftInventory(Map<String, Object> query);
    List<Map<String, Object>> selectGiftInventoryMovements(Map<String, Object> query);
    Map<String, Object> selectGiftInventoryBalanceForUpdate(Long giftId);
    int insertGiftInventory(Map<String, Object> value);
    int updateGiftInventory(Map<String, Object> value);
    int insertGiftInventoryMovement(Map<String, Object> value);
    List<Map<String, Object>> selectOrderInventoryAllocations(String orderNo);
    int insertOrderInventoryAllocation(Map<String, Object> value);
    int deleteOrderInventoryAllocations(String orderNo);
    Map<String, Object> selectGiftInventorySummary(Map<String, Object> query);
    List<Map<String, Object>> selectGiftInventoryLowStock(Map<String, Object> query);
    List<Map<String, Object>> selectOrderGiftLedger(Map<String, Object> query);
    Map<String, Object> selectGiftSummary(Map<String, Object> query);
    List<Map<String, Object>> selectGiftSummaryGroups(Map<String, Object> query);
    int insertDingSyncLog(Map<String, Object> log);
}
