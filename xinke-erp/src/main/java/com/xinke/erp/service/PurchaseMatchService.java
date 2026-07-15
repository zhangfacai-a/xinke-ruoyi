package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PurchaseMatchService
{
    List<Map<String, Object>> listSuppliers(Map<String, Object> query);
    Map<String, Object> getSupplier(Long id);
    int addSupplier(Map<String, Object> data, String username);
    int updateSupplier(Map<String, Object> data, String username);
    int deleteSupplier(Long[] ids, String username);
    List<Map<String, Object>> listAliases(Map<String, Object> query);
    int addAlias(Map<String, Object> data, String username);
    int updateAlias(Map<String, Object> data, String username);
    int deleteAlias(Long[] ids, String username);

    List<Map<String, Object>> listManual(Map<String, Object> query);
    Map<String, Object> getManual(Long id);
    int addManual(Map<String, Object> data, Long userId, String username);
    int updateManual(Map<String, Object> data, Long userId, String username);
    int deleteManual(Long[] ids, String username);

    List<Map<String, Object>> listSummary(Map<String, Object> query);
    Map<String, Object> getSummary(Long id);
    Map<String, Object> summaryStats(Map<String, Object> query);
    int updateSummary(Map<String, Object> data, Long userId, String username);
    int deleteSummary(Long[] ids, String username);
    int matchOne(Long summaryId, Long userId, String username);
    int rematchFailed(Map<String, Object> query, Long userId, String username);
    int manualBind(Long summaryId, Long manualOrderId, Long userId, String username);

    Map<String, Object> preview(String importType, Long supplierId, MultipartFile file) throws Exception;
    Map<String, Object> importExcel(String importType, Long supplierId, MultipartFile file, String sheetName, Long userId, String username) throws Exception;
    void downloadTemplate(String importType, HttpServletResponse response) throws Exception;
    List<Map<String, Object>> listImportBatches(Map<String, Object> query);
    Map<String, Object> getImportBatch(Long batchId);
    List<Map<String, Object>> listImportDetails(Map<String, Object> query);
    List<Map<String, Object>> listConflicts(Map<String, Object> query);
    int resolveConflict(Long conflictId, String action, Long userId, String username);
    List<Map<String, Object>> listMatchLogs(Map<String, Object> query);
    void exportManual(Map<String, Object> query, HttpServletResponse response) throws Exception;
    void exportSummary(Map<String, Object> query, HttpServletResponse response) throws Exception;
}
