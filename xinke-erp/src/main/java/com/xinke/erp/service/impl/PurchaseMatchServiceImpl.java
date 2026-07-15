package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.PurchaseMatchMapper;
import com.xinke.erp.service.PurchaseMatchService;

@Service
public class PurchaseMatchServiceImpl implements PurchaseMatchService
{
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat NUMBER_TEXT = new DecimalFormat("0.####################");
    private static final List<String> MANUAL_HEADERS = Arrays.asList("日期", "订单号", "型号", "单价", "数量", "金额", "姓名", "电话", "地址", "备注", "物流单号", "是否售后", "售后申请", "余款", "回单（）", "打款金额", "开票情况", "对账情况");
    private static final List<String> SUMMARY_HEADERS = Arrays.asList("单据供应商", "采购单备注", "合同备注", "商家编码", "采购数量", "匹配运单号", "运单号", "核查发货", "异常", "采购人员/备注");
    private static final Set<String> MANUAL_REQUIRED = Set.of("日期", "订单号", "型号", "数量", "物流单号");
    private static final Set<String> SUMMARY_REQUIRED = Set.of("单据供应商", "采购单备注", "商家编码", "采购数量");

    @Autowired
    private PurchaseMatchMapper mapper;

    @Override public List<Map<String, Object>> listSuppliers(Map<String, Object> query) { return mapper.selectSuppliers(query); }
    @Override public Map<String, Object> getSupplier(Long id) { return mapper.selectSupplierById(id); }
    @Override public List<Map<String, Object>> listAliases(Map<String, Object> query) { return mapper.selectAliases(query); }
    @Override public List<Map<String, Object>> listManual(Map<String, Object> query) { List<Map<String, Object>> rows = mapper.selectManualOrders(query); maskPhones(query, rows); return rows; }
    @Override public Map<String, Object> getManual(Long id) { return mapper.selectManualById(id); }
    @Override public List<Map<String, Object>> listSummary(Map<String, Object> query) { return mapper.selectSummaries(query); }
    @Override public Map<String, Object> getSummary(Long id) { return mapper.selectSummaryById(id); }
    @Override public Map<String, Object> summaryStats(Map<String, Object> query) { return mapper.selectSummaryStats(query); }
    @Override public List<Map<String, Object>> listImportBatches(Map<String, Object> query) { return mapper.selectImportBatches(query); }
    @Override public Map<String, Object> getImportBatch(Long batchId) { return mapper.selectImportBatchById(batchId); }
    @Override public List<Map<String, Object>> listImportDetails(Map<String, Object> query) { return mapper.selectImportDetails(query); }
    @Override public List<Map<String, Object>> listConflicts(Map<String, Object> query) { return mapper.selectConflicts(query); }
    @Override public List<Map<String, Object>> listMatchLogs(Map<String, Object> query) { return mapper.selectMatchLogs(query); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addSupplier(Map<String, Object> data, String username)
    {
        data.put("supplierName", norm(data.get("supplierName")));
        data.put("createBy", username);
        return mapper.insertSupplier(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSupplier(Map<String, Object> data, String username)
    {
        data.put("supplierName", norm(data.get("supplierName")));
        data.put("updateBy", username);
        int rows = mapper.updateSupplier(data);
        Long supplierId = longValue(data.get("supplierId"));
        if (supplierId != null)
        {
            rematchAffectedSupplier(supplierId, null, null, username);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSupplier(Long[] ids, String username)
    {
        return mapper.deleteSupplierByIds(ids, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addAlias(Map<String, Object> data, String username)
    {
        data.put("aliasName", norm(data.get("aliasName")));
        data.put("createBy", username);
        int rows = mapper.insertAlias(data);
        Long supplierId = longValue(data.get("supplierId"));
        if (supplierId != null) rematchAffectedSupplier(supplierId, null, null, username);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAlias(Map<String, Object> data, String username)
    {
        data.put("aliasName", norm(data.get("aliasName")));
        data.put("updateBy", username);
        int rows = mapper.updateAlias(data);
        Long supplierId = longValue(data.get("supplierId"));
        if (supplierId != null) rematchAffectedSupplier(supplierId, null, null, username);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAlias(Long[] ids, String username)
    {
        return mapper.deleteAliasByIds(ids, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addManual(Map<String, Object> data, Long userId, String username)
    {
        Map<String, Object> supplier = requireSupplier(longValue(data.get("supplierId")));
        normalizeManual(data, supplier);
        stampCreate(data, userId, username, "MANUAL", 0L, null);
        data.put("createBy", username);
        int rows = mapper.insertManual(data);
        rematchAffectedSupplier(longValue(data.get("supplierId")), text(data.get("orderNo")), userId, username);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateManual(Map<String, Object> data, Long userId, String username)
    {
        Map<String, Object> old = mapper.selectManualById(longValue(data.get("manualOrderId")));
        if (old == null) throw new ServiceException("手工订单不存在");
        if (data.get("supplierId") != null)
        {
            Map<String, Object> supplier = requireSupplier(longValue(data.get("supplierId")));
            data.put("supplierNameSnapshot", supplier.get("supplierName"));
            data.put("activeUniqueKey", activeKey(data.get("supplierId"), data.get("orderNo"), data.get("manualOrderId")));
        }
        data.put("updateBy", username);
        int rows = mapper.updateManual(data);
        logChangedFields("purchase_manual_order", longValue(data.get("manualOrderId")), old, data, null, userId, username);
        rematchAffectedSupplier(longValue(data.getOrDefault("supplierId", old.get("supplier_id"))), text(data.getOrDefault("orderNo", old.get("order_no"))), userId, username);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteManual(Long[] ids, String username)
    {
        return mapper.deleteManualByIds(ids, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSummary(Map<String, Object> data, Long userId, String username)
    {
        Map<String, Object> old = mapper.selectSummaryById(longValue(data.get("summaryId")));
        if (old == null) throw new ServiceException("采购汇总不存在");
        if (data.containsKey("documentSupplierName"))
        {
            Map<String, Object> supplier = findSupplier(text(data.get("documentSupplierName")));
            data.put("documentSupplierId", supplier == null ? null : supplier.get("supplierId"));
        }
        data.put("updateBy", username);
        int rows = mapper.updateSummary(data);
        logChangedFields("purchase_summary", longValue(data.get("summaryId")), old, data, null, userId, username);
        matchOne(longValue(data.get("summaryId")), userId, username);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSummary(Long[] ids, String username)
    {
        return mapper.deleteSummaryByIds(ids, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int matchOne(Long summaryId, Long userId, String username)
    {
        Map<String, Object> summary = mapper.selectSummaryById(summaryId);
        if (summary == null) return 0;
        MatchResult result = calculateMatch(summary);
        Map<String, Object> update = new HashMap<>();
        update.put("summaryId", summaryId);
        update.put("matchedOrderId", result.manualOrderId);
        update.put("matchedLogisticsNo", result.logisticsNo);
        update.put("matchStatus", result.status);
        update.put("matchType", result.type);
        update.put("matchMessage", result.message);
        update.put("updateBy", username);
        mapper.updateSummaryMatch(update);
        insertMatchLog(summaryId, result.manualOrderId, longValue(summary.get("document_supplier_id")),
                text(summary.get("purchase_order_remark")), text(summary.get("matched_logistics_no")), result.logisticsNo,
                result.status, result.type, result.message, userId, username);
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rematchFailed(Map<String, Object> query, Long userId, String username)
    {
        int count = 0;
        Map<String, Object> matchQuery = new HashMap<>(query);
        matchQuery.remove("pageNum");
        matchQuery.remove("pageSize");
        matchQuery.remove("orderByColumn");
        matchQuery.remove("isAsc");
        for (Map<String, Object> row : mapper.selectFailedSummaries(matchQuery))
        {
            count += matchOne(longValue(row.get("summary_id")), userId, username);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int manualBind(Long summaryId, Long manualOrderId, Long userId, String username)
    {
        Map<String, Object> order = mapper.selectManualById(manualOrderId);
        if (order == null) throw new ServiceException("手工订单不存在");
        Map<String, Object> summary = mapper.selectSummaryById(summaryId);
        Map<String, Object> update = new HashMap<>();
        update.put("summaryId", summaryId);
        update.put("matchedOrderId", manualOrderId);
        update.put("matchedLogisticsNo", order.get("logistics_no"));
        update.put("matchStatus", hasText(order.get("logistics_no")) ? "SUCCESS" : "LOGISTICS_EMPTY");
        update.put("matchType", "MANUAL");
        update.put("matchMessage", "手工指定匹配");
        update.put("updateBy", username);
        int rows = mapper.updateSummaryMatch(update);
        insertMatchLog(summaryId, manualOrderId, longValue(order.get("supplier_id")), text(order.get("order_no")),
                summary == null ? null : text(summary.get("matched_logistics_no")), text(order.get("logistics_no")),
                text(update.get("matchStatus")), "MANUAL", "手工指定匹配", userId, username);
        return rows;
    }

    @Override
    public Map<String, Object> preview(String importType, Long supplierId, MultipartFile file) throws Exception
    {
        validateFile(file);
        try (Workbook wb = WorkbookFactory.create(file.getInputStream()))
        {
            Sheet sheet = wb.getSheetAt(0);
            ParsedSheet parsed = parseSheet(wb, sheet, importType, 20);
            Map<String, Object> data = new HashMap<>();
            data.put("sheetNames", sheetNames(wb));
            data.put("sheetName", sheet.getSheetName());
            data.put("headerValid", parsed.headerValid);
            data.put("valid", parsed.headerValid);
            data.put("missingHeaders", parsed.missingHeaders);
            data.put("headers", parsed.headers);
            data.put("rows", parsed.rows);
            data.put("message", parsed.headerValid ? "模板校验通过" : "缺少关键列：" + parsed.missingHeaders);
            return data;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(String importType, Long supplierId, MultipartFile file, String sheetName, Long userId, String username) throws Exception
    {
        validateFile(file);
        if ("MANUAL_ORDER".equals(importType) && supplierId == null) throw new ServiceException("上传手工订单必须选择供应商");
        Map<String, Object> batch = new HashMap<>();
        batch.put("batchNo", "PI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        batch.put("importType", importType);
        batch.put("supplierId", supplierId);
        batch.put("originalFileName", file.getOriginalFilename());
        batch.put("fileHash", sha256(file.getBytes()));
        batch.put("uploadUserId", userId);
        batch.put("uploadUserName", username);
        mapper.insertImportBatch(batch);
        Long batchId = longValue(batch.get("batchId"));
        ImportCounter counter = new ImportCounter();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream()))
        {
            Sheet sheet = hasText(sheetName) ? wb.getSheet(sheetName) : wb.getSheetAt(0);
            if (sheet == null) throw new ServiceException("Sheet不存在：" + sheetName);
            ParsedSheet parsed = parseSheet(wb, sheet, importType, Integer.MAX_VALUE);
            if (!parsed.headerValid) throw new ServiceException("模板缺少关键列：" + parsed.missingHeaders);
            batch.put("sheetName", sheet.getSheetName());
            if ("MANUAL_ORDER".equals(importType))
            {
                importManualRows(parsed.rows, supplierId, batchId, file.getOriginalFilename(), userId, username, counter);
            }
            else
            {
                importSummaryRows(parsed.rows, batchId, file.getOriginalFilename(), userId, username, counter);
            }
            batch.put("batchStatus", "SUCCESS");
        }
        catch (Exception e)
        {
            batch.put("batchStatus", "FAILED");
            batch.put("errorMessage", e.getMessage());
            throw e;
        }
        finally
        {
            counter.apply(batch);
            mapper.updateImportBatch(batch);
        }
        return batch;
    }

    @Override
    public void downloadTemplate(String importType, HttpServletResponse response) throws Exception
    {
        boolean manual = "MANUAL_ORDER".equals(importType);
        List<String> headers = manual ? MANUAL_HEADERS : SUMMARY_HEADERS;
        String fileName = manual ? "手工订单总表模板.xlsx" : "采购汇总总表模板.xlsx";
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet(manual ? "手工订单总表" : "采购汇总总表");
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++)
            {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                sheet.setDefaultColumnStyle(i, textStyle);
                sheet.setColumnWidth(i, Math.max(12, headers.get(i).length() * 3) * 256);
            }
            sheet.createRow(1);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            wb.write(response.getOutputStream());
        }
    }

    private void importManualRows(List<Map<String, Object>> rows, Long supplierId, Long batchId, String fileName, Long userId, String username, ImportCounter counter)
    {
        Map<String, Object> supplier = requireSupplier(supplierId);
        Set<String> affected = new LinkedHashSet<>();
        for (Map<String, Object> row : rows)
        {
            counter.totalRows++;
            Map<String, Object> detail = baseDetail(batchId, row, "purchase_manual_order", userId, username);
            try
            {
                Map<String, Object> data = manualFromExcel(row, supplier);
                stampCreate(data, userId, username, fileName, batchId, intValue(row.get("_rowNumber")));
                Map<String, Object> old = mapper.selectManualByActiveKey(text(data.get("activeUniqueKey")));
                if (old == null)
                {
                    data.put("createBy", username);
                    mapper.insertManual(data);
                    detail.put("processResult", "INSERTED");
                    detail.put("targetRecordId", data.get("manualOrderId"));
                    counter.successRows++;
                }
                else
                {
                    data.put("manualOrderId", old.get("manual_order_id"));
                    data.put("updateBy", username);
                    DuplicateDecision decision = decide(old, data, manualComparableFields());
                    detail.put("targetRecordId", old.get("manual_order_id"));
                    if (decision.same)
                    {
                        mapper.updateManual(latestOnlyManual(data));
                        detail.put("processResult", "DUPLICATE");
                        counter.duplicateRows++;
                    }
                    else if (decision.conflictFields.isEmpty())
                    {
                        mapper.fillManualEmptyFields(data);
                        logChangedFields("purchase_manual_order", longValue(old.get("manual_order_id")), old, data, batchId, userId, username);
                        detail.put("processResult", "FILLED_EMPTY_FIELDS");
                        counter.updatedRows++;
                    }
                    else
                    {
                        createConflict("MANUAL_ORDER", "purchase_manual_order", old, data, batchId, detail, decision.conflictFields, userId, username);
                        detail.put("processResult", "CONFLICT");
                        counter.conflictRows++;
                    }
                }
                affected.add(data.get("supplierId") + "|" + data.get("orderNo"));
                detail.put("checkStatus", "VALID");
            }
            catch (Exception ex)
            {
                detail.put("checkStatus", "INVALID");
                detail.put("processResult", "FAILED");
                detail.put("errorMessage", ex.getMessage());
                counter.failedRows++;
            }
            mapper.insertImportDetail(detail);
        }
        for (String key : affected)
        {
            String[] parts = key.split("\\|", 2);
            rematchAffectedSupplier(Long.valueOf(parts[0]), parts.length > 1 ? parts[1] : null, userId, username);
        }
    }

    private void importSummaryRows(List<Map<String, Object>> rows, Long batchId, String fileName, Long userId, String username, ImportCounter counter)
    {
        for (Map<String, Object> row : rows)
        {
            counter.totalRows++;
            Map<String, Object> detail = baseDetail(batchId, row, "purchase_summary", userId, username);
            try
            {
                Map<String, Object> data = summaryFromExcel(row);
                stampCreate(data, userId, username, fileName, batchId, intValue(row.get("_rowNumber")));
                Map<String, Object> old = mapper.selectSummaryByActiveKey(text(data.get("activeUniqueKey")));
                if (old == null)
                {
                    data.put("createBy", username);
                    mapper.insertSummary(data);
                    detail.put("processResult", "INSERTED");
                    detail.put("targetRecordId", data.get("summaryId"));
                    counter.successRows++;
                }
                else
                {
                    data.put("summaryId", old.get("summary_id"));
                    data.put("updateBy", username);
                    DuplicateDecision decision = decide(old, data, summaryComparableFields());
                    detail.put("targetRecordId", old.get("summary_id"));
                    if (decision.same)
                    {
                        mapper.updateSummary(latestOnlySummary(data));
                        detail.put("processResult", "DUPLICATE");
                        counter.duplicateRows++;
                    }
                    else if (decision.conflictFields.isEmpty())
                    {
                        mapper.fillSummaryEmptyFields(data);
                        logChangedFields("purchase_summary", longValue(old.get("summary_id")), old, data, batchId, userId, username);
                        detail.put("processResult", "FILLED_EMPTY_FIELDS");
                        counter.updatedRows++;
                    }
                    else
                    {
                        createConflict("PURCHASE_SUMMARY", "purchase_summary", old, data, batchId, detail, decision.conflictFields, userId, username);
                        detail.put("processResult", "CONFLICT");
                        counter.conflictRows++;
                    }
                }
                detail.put("checkStatus", "VALID");
                Long summaryId = longValue(data.get("summaryId"));
                if (summaryId == null) summaryId = longValue(detail.get("targetRecordId"));
                if (summaryId != null) matchOne(summaryId, userId, username);
            }
            catch (Exception ex)
            {
                detail.put("checkStatus", "INVALID");
                detail.put("processResult", "FAILED");
                detail.put("errorMessage", ex.getMessage());
                counter.failedRows++;
            }
            mapper.insertImportDetail(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resolveConflict(Long conflictId, String action, Long userId, String username)
    {
        Map<String, Object> conflict = mapper.selectConflictById(conflictId);
        if (conflict == null) throw new ServiceException("冲突不存在");
        if (!"PENDING".equals(text(conflict.get("conflict_status")))) return 0;
        if ("USE_NEW".equals(action))
        {
            Map<String, Object> data = JSON.parseObject(text(conflict.get("new_data_json")), Map.class);
            data.put("updateBy", username);
            if ("purchase_manual_order".equals(conflict.get("target_table")))
            {
                mapper.updateManual(data);
                rematchAffectedSupplier(longValue(data.get("supplierId")), text(data.get("orderNo")), userId, username);
            }
            else if ("purchase_summary".equals(conflict.get("target_table")))
            {
                mapper.updateSummary(data);
                matchOne(longValue(data.get("summaryId")), userId, username);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("conflictId", conflictId);
        data.put("resolveResult", action);
        data.put("resolveUserId", userId);
        data.put("resolveUserName", username);
        data.put("updateBy", username);
        return mapper.updateConflictResolved(data);
    }

    @Override
    public void exportManual(Map<String, Object> query, HttpServletResponse response) throws Exception
    {
        List<Map<String, Object>> rows = mapper.selectManualOrders(query);
        exportRows(response, "手工订单总表.xlsx", MANUAL_HEADERS, rows, Arrays.asList("order_date","order_no","product_model","unit_price","quantity","total_amount","customer_name","customer_phone","customer_address","order_remark","logistics_no","after_sale_flag","after_sale_request","balance_amount","receipt_status","payment_amount","invoice_status","reconciliation_status"),
                Arrays.asList("供应商","上传人","上传时间","最后修改人","最后修改时间","来源文件"), Arrays.asList("supplier_name_snapshot","upload_user_name","upload_time","update_by","update_time","source_file_name"));
    }

    @Override
    public void exportSummary(Map<String, Object> query, HttpServletResponse response) throws Exception
    {
        List<Map<String, Object>> rows = mapper.selectSummaries(query);
        exportRows(response, "采购汇总总表.xlsx", SUMMARY_HEADERS, rows, Arrays.asList("document_supplier_name","purchase_order_remark","contract_remark","merchant_code","purchase_quantity","matched_logistics_no","original_logistics_no","shipping_check_status","exception_reason","purchaser_remark"),
                Arrays.asList("匹配状态","匹配说明","匹配时间","上传人","上传时间","最后修改人","最后修改时间","来源文件"), Arrays.asList("match_status","match_message","match_time","upload_user_name","upload_time","update_by","update_time","source_file_name"));
    }

    private ParsedSheet parseSheet(Workbook wb, Sheet sheet, String importType, int limit)
    {
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) throw new ServiceException("模板缺少表头");
        List<String> expected = "MANUAL_ORDER".equals(importType) ? MANUAL_HEADERS : SUMMARY_HEADERS;
        Set<String> required = "MANUAL_ORDER".equals(importType) ? MANUAL_REQUIRED : SUMMARY_REQUIRED;
        Map<Integer, String> index = new LinkedHashMap<>();
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < Math.max(headerRow.getLastCellNum(), expected.size()); i++)
        {
            String h = normalizeHeader(cellText(headerRow.getCell(i), evaluator));
            if (hasText(h))
            {
                String canonical = canonicalHeader(h, expected);
                index.put(i, canonical);
                headers.add(canonical);
            }
        }
        List<String> missing = required.stream().filter(r -> !headers.contains(r)).collect(Collectors.toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        int max = Math.min(sheet.getLastRowNum(), limit == Integer.MAX_VALUE ? sheet.getLastRowNum() : limit);
        for (int r = 1; r <= max; r++)
        {
            Row row = sheet.getRow(r);
            if (row == null || row.getZeroHeight()) continue;
            Map<String, Object> data = new LinkedHashMap<>();
            boolean empty = true;
            for (Map.Entry<Integer, String> entry : index.entrySet())
            {
                String value = cellText(row.getCell(entry.getKey()), evaluator);
                if (hasText(value)) empty = false;
                data.put(entry.getValue(), value);
            }
            if (!empty)
            {
                data.put("_rowNumber", r + 1);
                rows.add(data);
            }
        }
        return new ParsedSheet(headers, rows, missing);
    }

    private String cellText(Cell cell, FormulaEvaluator evaluator)
    {
        if (cell == null) return "";
        try
        {
            if (cell.getCellType() == CellType.FORMULA)
            {
                CellType type = evaluator.evaluateFormulaCell(cell);
                if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return DATE.format(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                if (type == CellType.NUMERIC) return NUMBER_TEXT.format(cell.getNumericCellValue());
                return new DataFormatter().formatCellValue(cell, evaluator).trim();
            }
            if (cell.getCellType() == CellType.NUMERIC)
            {
                if (DateUtil.isCellDateFormatted(cell)) return DATE.format(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                return NUMBER_TEXT.format(cell.getNumericCellValue());
            }
            return new DataFormatter().formatCellValue(cell).trim();
        }
        catch (Exception e)
        {
            return new DataFormatter().formatCellValue(cell).trim();
        }
    }

    private Map<String, Object> manualFromExcel(Map<String, Object> row, Map<String, Object> supplier)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("supplierId", supplier.get("supplierId"));
        data.put("supplierNameSnapshot", supplier.get("supplierName"));
        data.put("orderDate", dateValue(row.get("日期")));
        data.put("orderNo", requiredText(row.get("订单号"), "订单号"));
        data.put("productModel", requiredText(row.get("型号"), "型号"));
        data.put("unitPrice", decimal(row.get("单价")));
        data.put("quantity", decimal(row.get("数量")));
        data.put("totalAmount", totalAmount(row.get("金额"), data.get("unitPrice"), data.get("quantity")));
        data.put("customerName", norm(row.get("姓名")));
        data.put("customerPhone", norm(row.get("电话")));
        data.put("customerAddress", norm(row.get("地址")));
        data.put("orderRemark", norm(row.get("备注")));
        data.put("logisticsNo", requiredText(row.get("物流单号"), "物流单号"));
        data.put("afterSaleFlag", norm(row.get("是否售后")));
        data.put("afterSaleRequest", norm(row.get("售后申请")));
        data.put("balanceAmount", decimal(row.get("余款")));
        data.put("receiptStatus", norm(row.get("回单（）")));
        data.put("paymentAmount", decimal(row.get("打款金额")));
        data.put("invoiceStatus", norm(row.get("开票情况")));
        data.put("reconciliationStatus", norm(row.get("对账情况")));
        data.put("amountCheckStatus", amountCheck(data));
        data.put("dataStatus", "NORMAL");
        data.put("activeUniqueKey", activeKey(data.get("supplierId"), data.get("orderNo"), null));
        return data;
    }

    private Map<String, Object> summaryFromExcel(Map<String, Object> row)
    {
        String supplierName = requiredText(row.get("单据供应商"), "单据供应商");
        Map<String, Object> supplier = findSupplier(supplierName);
        Map<String, Object> data = new HashMap<>();
        data.put("documentSupplierId", supplier == null ? null : supplier.get("supplierId"));
        data.put("documentSupplierName", supplierName);
        data.put("purchaseOrderRemark", requiredText(row.get("采购单备注"), "采购单备注"));
        data.put("contractRemark", norm(row.get("合同备注")));
        data.put("merchantCode", requiredText(row.get("商家编码"), "商家编码"));
        data.put("purchaseQuantity", decimal(row.get("采购数量")));
        data.put("matchedLogisticsNo", null);
        data.put("originalLogisticsNo", norm(row.get("运单号")));
        data.put("shippingCheckStatus", norm(row.get("核查发货")));
        data.put("exceptionReason", norm(row.get("异常")));
        data.put("purchaserRemark", norm(row.get("采购人员/备注")));
        data.put("matchStatus", supplier == null ? "SUPPLIER_NOT_FOUND" : "PENDING");
        data.put("matchMessage", supplier == null ? "供应商未识别，请维护供应商或别名" : null);
        data.put("dataStatus", "NORMAL");
        data.put("activeUniqueKey", activeKey(supplier == null ? 0 : supplier.get("supplierId"), data.get("purchaseOrderRemark") + "|" + data.get("merchantCode"), null));
        return data;
    }

    private MatchResult calculateMatch(Map<String, Object> summary)
    {
        Long supplierId = longValue(summary.get("document_supplier_id"));
        String orderNo = text(summary.get("purchase_order_remark"));
        if (supplierId == null) return new MatchResult("SUPPLIER_NOT_FOUND", null, null, "AUTO", "供应商未识别");
        List<Map<String, Object>> orders = mapper.selectManualByBusinessKey(supplierId, orderNo);
        if (orders.isEmpty()) return new MatchResult("NOT_FOUND", null, null, "AUTO", "未找到供应商+订单号对应的手工订单");
        if (orders.size() > 1) return new MatchResult("MULTIPLE", null, null, "AUTO", "匹配到多条手工订单");
        Map<String, Object> order = orders.get(0);
        String logistics = text(order.get("logistics_no"));
        if (!hasText(logistics)) return new MatchResult("LOGISTICS_EMPTY", longValue(order.get("manual_order_id")), null, "AUTO", "找到订单但物流单号为空");
        return new MatchResult("SUCCESS", longValue(order.get("manual_order_id")), logistics, "AUTO", "匹配成功");
    }

    private void rematchAffectedSupplier(Long supplierId, String orderNo, Long userId, String username)
    {
        for (Map<String, Object> row : mapper.selectSummariesByBusinessKey(supplierId, orderNo))
        {
            matchOne(longValue(row.get("summary_id")), userId, username);
        }
    }

    private Map<String, Object> findSupplier(String supplierName)
    {
        String name = norm(supplierName);
        if (!hasText(name)) return null;
        Map<String, Object> supplier = mapper.selectSupplierByName(name);
        return supplier != null ? supplier : mapper.selectSupplierByAlias(name);
    }

    private Map<String, Object> requireSupplier(Long supplierId)
    {
        Map<String, Object> supplier = mapper.selectSupplierById(supplierId);
        if (supplier == null) throw new ServiceException("供应商不存在");
        return supplier;
    }

    private void normalizeManual(Map<String, Object> data, Map<String, Object> supplier)
    {
        data.put("supplierId", supplier.get("supplierId"));
        data.put("supplierNameSnapshot", supplier.get("supplierName"));
        data.put("orderNo", requiredText(data.get("orderNo"), "订单号"));
        data.put("activeUniqueKey", activeKey(data.get("supplierId"), data.get("orderNo"), data.get("manualOrderId")));
        data.put("dataStatus", data.getOrDefault("dataStatus", "NORMAL"));
    }

    private String activeKey(Object supplierId, Object business, Object idWhenDeleted)
    {
        return norm(supplierId) + "|" + norm(business);
    }

    private DuplicateDecision decide(Map<String, Object> old, Map<String, Object> data, Map<String, String> fields)
    {
        boolean same = true;
        List<String> conflicts = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            String oldValue = text(old.get(entry.getValue()));
            String newValue = text(data.get(entry.getKey()));
            if (!hasText(newValue)) continue;
            if (!hasText(oldValue))
            {
                same = false;
            }
            else if (!Objects.equals(oldValue, newValue))
            {
                same = false;
                conflicts.add(entry.getKey());
            }
        }
        return new DuplicateDecision(same, conflicts);
    }

    private Map<String, String> manualComparableFields()
    {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("orderDate","order_date"); f.put("productModel","product_model"); f.put("unitPrice","unit_price"); f.put("quantity","quantity"); f.put("totalAmount","total_amount");
        f.put("customerName","customer_name"); f.put("customerPhone","customer_phone"); f.put("customerAddress","customer_address"); f.put("orderRemark","order_remark"); f.put("logisticsNo","logistics_no");
        f.put("afterSaleFlag","after_sale_flag"); f.put("afterSaleRequest","after_sale_request"); f.put("balanceAmount","balance_amount"); f.put("receiptStatus","receipt_status"); f.put("paymentAmount","payment_amount");
        f.put("invoiceStatus","invoice_status"); f.put("reconciliationStatus","reconciliation_status");
        return f;
    }

    private Map<String, String> summaryComparableFields()
    {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("contractRemark","contract_remark"); f.put("purchaseQuantity","purchase_quantity"); f.put("originalLogisticsNo","original_logistics_no");
        f.put("shippingCheckStatus","shipping_check_status"); f.put("exceptionReason","exception_reason"); f.put("purchaserRemark","purchaser_remark");
        return f;
    }

    private Map<String, Object> latestOnlyManual(Map<String, Object> data)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("manualOrderId", data.get("manualOrderId")); copyLatest(data, m); m.put("updateBy", data.get("updateBy"));
        return m;
    }

    private Map<String, Object> latestOnlySummary(Map<String, Object> data)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("summaryId", data.get("summaryId")); copyLatest(data, m); m.put("updateBy", data.get("updateBy"));
        return m;
    }

    private void createConflict(String type, String table, Map<String, Object> old, Map<String, Object> data, Long batchId, Map<String, Object> detail, List<String> fields, Long userId, String username)
    {
        Map<String, Object> conflict = new HashMap<>();
        conflict.put("conflictType", type);
        conflict.put("targetTable", table);
        conflict.put("targetRecordId", old.get(table.equals("purchase_manual_order") ? "manual_order_id" : "summary_id"));
        conflict.put("batchId", batchId);
        conflict.put("businessKey", data.get("activeUniqueKey"));
        conflict.put("oldDataJson", JSON.toJSONString(old));
        conflict.put("newDataJson", JSON.toJSONString(data));
        conflict.put("conflictFields", String.join(",", fields));
        conflict.put("createBy", username);
        mapper.insertConflict(conflict);
        detail.put("beforeDataJson", JSON.toJSONString(old));
        detail.put("afterDataJson", JSON.toJSONString(data));
    }

    private void logChangedFields(String table, Long id, Map<String, Object> old, Map<String, Object> data, Long batchId, Long userId, String username)
    {
        data.forEach((k, v) -> {
            if (k.endsWith("Id") || k.endsWith("Json") || k.startsWith("latest") || k.equals("updateBy")) return;
            Object oldVal = old.get(camelToUnderline(k));
            if (v != null && !Objects.equals(text(oldVal), text(v)))
            {
                Map<String, Object> log = new HashMap<>();
                log.put("targetTable", table);
                log.put("targetRecordId", id);
                log.put("fieldName", k);
                log.put("oldValue", text(oldVal));
                log.put("newValue", text(v));
                log.put("batchId", batchId);
                log.put("operatorId", userId);
                log.put("operatorName", username);
                mapper.insertFieldChangeLog(log);
            }
        });
    }

    private void insertMatchLog(Long summaryId, Long manualOrderId, Long supplierId, String orderNo, String oldNo, String newNo, String status, String type, String message, Long userId, String username)
    {
        Map<String, Object> log = new HashMap<>();
        log.put("summaryId", summaryId); log.put("manualOrderId", manualOrderId); log.put("supplierId", supplierId); log.put("orderNo", orderNo);
        log.put("oldLogisticsNo", oldNo); log.put("newLogisticsNo", newNo); log.put("matchStatus", status); log.put("matchType", type); log.put("matchMessage", message);
        log.put("operatorId", userId); log.put("operatorName", username);
        mapper.insertMatchLog(log);
    }

    private Map<String, Object> baseDetail(Long batchId, Map<String, Object> row, String target, Long userId, String username)
    {
        Map<String, Object> d = new HashMap<>();
        d.put("batchId", batchId);
        d.put("sourceRowNumber", row.get("_rowNumber"));
        d.put("businessKey", JSON.toJSONString(row));
        d.put("rawDataJson", JSON.toJSONString(row));
        d.put("targetTable", target);
        d.put("uploadUserId", userId);
        d.put("uploadUserName", username);
        return d;
    }

    private void stampCreate(Map<String, Object> data, Long userId, String username, String fileName, Long batchId, Integer rowNumber)
    {
        data.put("uploadUserId", userId); data.put("uploadUserName", username); data.put("latestUploadUserId", userId); data.put("latestUploadUserName", username);
        data.put("sourceFileName", fileName); data.put("latestSourceFileName", fileName); data.put("importBatchId", batchId); data.put("latestImportBatchId", batchId);
        data.put("sourceRowNumber", rowNumber); data.put("latestSourceRowNumber", rowNumber);
    }

    private void copyLatest(Map<String, Object> from, Map<String, Object> to)
    {
        for (String key : Arrays.asList("latestUploadUserId","latestUploadUserName","latestSourceFileName","latestImportBatchId","latestSourceRowNumber"))
        {
            to.put(key, from.get(key));
        }
    }

    private void validateFile(MultipartFile file)
    {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择Excel文件");
        String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        if (!List.of("xlsx", "xls").contains(ext)) throw new ServiceException("只支持 .xlsx 和 .xls 文件");
        if (file.getSize() > 30L * 1024 * 1024) throw new ServiceException("文件不能超过30MB");
    }

    private String sha256(byte[] bytes) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void exportRows(HttpServletResponse response, String fileName, List<String> templateHeaders, List<Map<String, Object>> rows, List<String> keys, List<String> extraHeaders, List<String> extraKeys) throws Exception
    {
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("数据");
            Row header = sheet.createRow(0);
            List<String> heads = new ArrayList<>(templateHeaders);
            heads.addAll(extraHeaders);
            for (int i = 0; i < heads.size(); i++) header.createCell(i).setCellValue(heads.get(i));
            List<String> allKeys = new ArrayList<>(keys);
            allKeys.addAll(extraKeys);
            int r = 1;
            for (Map<String, Object> row : rows)
            {
                Row excel = sheet.createRow(r++);
                for (int c = 0; c < allKeys.size(); c++)
                {
                    String key = allKeys.get(c);
                    excel.createCell(c).setCellValue(exportSafe(exportDisplayValue(key, row.get(key))));
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            wb.write(response.getOutputStream());
        }
    }

    private void maskPhones(Map<String, Object> query, List<Map<String, Object>> rows)
    {
        if ("true".equals(String.valueOf(query.get("showSensitive")))) return;
        rows.forEach(r -> r.put("customer_phone", maskPhone(text(r.get("customer_phone")))));
    }

    private String maskPhone(String phone)
    {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String exportSafe(String value)
    {
        if (!hasText(value)) return "";
        return value.matches("^[=+\\-@].*") ? "'" + value : value;
    }

    private String exportDisplayValue(String key, Object value)
    {
        String text = text(value);
        if (!hasText(text)) return "";
        if ("match_status".equals(key) || "matchStatus".equals(key)) return matchStatusText(text);
        if ("data_status".equals(key) || "dataStatus".equals(key)) return dataStatusText(text);
        if ("batch_status".equals(key) || "batchStatus".equals(key)) return importStatusText(text);
        if ("process_result".equals(key) || "processResult".equals(key) || "check_status".equals(key) || "checkStatus".equals(key)) return rowStatusText(text);
        if ("conflict_status".equals(key) || "conflictStatus".equals(key)) return conflictStatusText(text);
        if ("import_type".equals(key) || "importType".equals(key)) return importTypeText(text);
        return text;
    }

    private String matchStatusText(String value)
    {
        return switch (value)
        {
            case "SUCCESS" -> "匹配成功";
            case "NOT_FOUND" -> "未找到订单";
            case "LOGISTICS_EMPTY" -> "物流为空";
            case "MULTIPLE" -> "多条匹配";
            case "SUPPLIER_NOT_FOUND" -> "供应商未识别";
            case "CONFLICT" -> "数据冲突";
            case "PENDING" -> "待匹配";
            default -> value;
        };
    }

    private String dataStatusText(String value)
    {
        return switch (value)
        {
            case "NORMAL" -> "正常";
            case "CONFLICT" -> "冲突";
            case "IGNORED" -> "已忽略";
            default -> value;
        };
    }

    private String importStatusText(String value)
    {
        return switch (value)
        {
            case "PARSING" -> "解析中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "PARTIAL_SUCCESS" -> "部分完成";
            default -> value;
        };
    }

    private String rowStatusText(String value)
    {
        return switch (value)
        {
            case "INSERTED" -> "新增";
            case "DUPLICATE" -> "重复无变化";
            case "FILLED_EMPTY_FIELDS" -> "补充空字段";
            case "CONFLICT" -> "数据冲突";
            case "INVALID" -> "无效行";
            case "FAILED" -> "失败";
            default -> value;
        };
    }

    private String conflictStatusText(String value)
    {
        return switch (value)
        {
            case "PENDING" -> "待处理";
            case "RESOLVED" -> "已处理";
            case "IGNORED" -> "已忽略";
            default -> value;
        };
    }

    private String importTypeText(String value)
    {
        return switch (value)
        {
            case "MANUAL_ORDER" -> "手工订单";
            case "PURCHASE_SUMMARY" -> "采购汇总";
            default -> value;
        };
    }

    private List<String> sheetNames(Workbook wb)
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) names.add(wb.getSheetName(i));
        return names;
    }

    private String canonicalHeader(String header, List<String> expected)
    {
        if ("回单()".equals(header)) return "回单（）";
        return expected.stream().filter(h -> normalizeHeader(h).equals(header)).findFirst().orElse(header);
    }

    private String normalizeHeader(Object value)
    {
        return norm(value).replace("（", "(").replace("）", ")").replace("回单()", "回单（）");
    }

    private String requiredText(Object value, String field)
    {
        String text = norm(value);
        if (!hasText(text)) throw new ServiceException(field + "不能为空");
        return text;
    }

    private String norm(Object value)
    {
        return value == null ? "" : String.valueOf(value).replace('\u3000', ' ').trim();
    }

    private String text(Object value) { return norm(value); }
    private boolean hasText(Object value) { return value != null && !String.valueOf(value).trim().isEmpty(); }
    private Long longValue(Object value) { return hasText(value) ? new BigDecimal(String.valueOf(value)).longValue() : null; }
    private Integer intValue(Object value) { return hasText(value) ? new BigDecimal(String.valueOf(value)).intValue() : null; }
    private BigDecimal decimal(Object value) { return hasText(value) ? new BigDecimal(text(value)).setScale(2, RoundingMode.HALF_UP) : null; }
    private Object dateValue(Object value) { return hasText(value) ? text(value) : null; }
    private BigDecimal totalAmount(Object total, Object unit, Object qty)
    {
        BigDecimal t = decimal(total);
        if (t != null) return t;
        if (unit == null || qty == null) return null;
        return ((BigDecimal) unit).multiply((BigDecimal) qty).setScale(2, RoundingMode.HALF_UP);
    }
    private String amountCheck(Map<String, Object> data)
    {
        BigDecimal unit = (BigDecimal) data.get("unitPrice");
        BigDecimal qty = (BigDecimal) data.get("quantity");
        BigDecimal amount = (BigDecimal) data.get("totalAmount");
        if (unit == null || qty == null || amount == null) return "OK";
        BigDecimal calc = unit.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        return calc.compareTo(amount) == 0 ? "OK" : "AMOUNT_MISMATCH";
    }
    private String camelToUnderline(String s) { return s.replaceAll("([A-Z])", "_$1").toLowerCase(); }

    private static class ParsedSheet
    {
        final List<String> headers; final List<Map<String, Object>> rows; final List<String> missingHeaders; final boolean headerValid;
        ParsedSheet(List<String> headers, List<Map<String, Object>> rows, List<String> missingHeaders) { this.headers = headers; this.rows = rows; this.missingHeaders = missingHeaders; this.headerValid = missingHeaders.isEmpty(); }
    }
    private static class MatchResult { final String status; final Long manualOrderId; final String logisticsNo; final String type; final String message; MatchResult(String s, Long id, String no, String t, String m) { status=s; manualOrderId=id; logisticsNo=no; type=t; message=m; } }
    private static class DuplicateDecision { final boolean same; final List<String> conflictFields; DuplicateDecision(boolean s, List<String> f) { same=s; conflictFields=f; } }
    private static class ImportCounter
    {
        int totalRows; int successRows; int duplicateRows; int updatedRows; int conflictRows; int failedRows;
        void apply(Map<String, Object> b) { b.put("totalRows", totalRows); b.put("successRows", successRows); b.put("duplicateRows", duplicateRows); b.put("updatedRows", updatedRows); b.put("conflictRows", conflictRows); b.put("failedRows", failedRows); }
    }
}
