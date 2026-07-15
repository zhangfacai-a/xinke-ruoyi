package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseMatchMapper
{
    List<Map<String, Object>> selectSuppliers(Map<String, Object> query);
    Map<String, Object> selectSupplierById(Long supplierId);
    Map<String, Object> selectSupplierByName(String supplierName);
    Map<String, Object> selectSupplierByAlias(String aliasName);
    int insertSupplier(Map<String, Object> data);
    int updateSupplier(Map<String, Object> data);
    int deleteSupplierByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);
    List<Map<String, Object>> selectAliases(Map<String, Object> query);
    int insertAlias(Map<String, Object> data);
    int updateAlias(Map<String, Object> data);
    int deleteAliasByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);

    List<Map<String, Object>> selectManualOrders(Map<String, Object> query);
    Map<String, Object> selectManualById(Long manualOrderId);
    Map<String, Object> selectManualByActiveKey(String activeUniqueKey);
    List<Map<String, Object>> selectManualByBusinessKey(@Param("supplierId") Long supplierId, @Param("orderNo") String orderNo);
    int insertManual(Map<String, Object> data);
    int updateManual(Map<String, Object> data);
    int fillManualEmptyFields(Map<String, Object> data);
    int deleteManualByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);

    List<Map<String, Object>> selectSummaries(Map<String, Object> query);
    Map<String, Object> selectSummaryById(Long summaryId);
    Map<String, Object> selectSummaryByActiveKey(String activeUniqueKey);
    List<Map<String, Object>> selectSummariesByBusinessKey(@Param("supplierId") Long supplierId, @Param("orderNo") String orderNo);
    List<Map<String, Object>> selectFailedSummaries(Map<String, Object> query);
    Map<String, Object> selectSummaryStats(Map<String, Object> query);
    int insertSummary(Map<String, Object> data);
    int updateSummary(Map<String, Object> data);
    int fillSummaryEmptyFields(Map<String, Object> data);
    int updateSummaryMatch(Map<String, Object> data);
    int deleteSummaryByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);

    int insertImportBatch(Map<String, Object> data);
    int updateImportBatch(Map<String, Object> data);
    List<Map<String, Object>> selectImportBatches(Map<String, Object> query);
    Map<String, Object> selectImportBatchById(Long batchId);
    int insertImportDetail(Map<String, Object> data);
    List<Map<String, Object>> selectImportDetails(Map<String, Object> query);

    int insertConflict(Map<String, Object> data);
    List<Map<String, Object>> selectConflicts(Map<String, Object> query);
    Map<String, Object> selectConflictById(Long conflictId);
    int updateConflictResolved(Map<String, Object> data);

    int insertMatchLog(Map<String, Object> data);
    List<Map<String, Object>> selectMatchLogs(Map<String, Object> query);
    int insertFieldChangeLog(Map<String, Object> data);
}
