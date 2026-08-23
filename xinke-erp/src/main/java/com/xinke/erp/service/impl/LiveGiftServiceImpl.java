package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.common.utils.StringUtils;
import com.xinke.erp.domain.LiveGiftBatchSaveRequest;
import com.xinke.erp.domain.LiveGiftCostImportRow;
import com.xinke.erp.domain.LiveGiftSaveRequest;
import com.xinke.erp.mapper.LiveGiftMapper;
import com.xinke.erp.service.ILiveGiftService;

@Service
public class LiveGiftServiceImpl implements ILiveGiftService
{
    private static final Set<String> GIFT_STATUSES = Set.of("selected", "customer_declined", "not_applicable", "pending");
    private static final Set<String> IGNORED_DING_USER_IDS = Set.of("131853658");
    private static final int MAX_BATCH_ORDERS = 500;
    private static final Pattern ASCII_ALIAS_CHAR = Pattern.compile("[A-Za-z0-9]");
    private static final String DING_API = "https://api.dingtalk.com";
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Autowired
    private LiveGiftMapper mapper;
    @Value("${live.dingtalk.app-key:}")
    private String appKey;
    @Value("${live.dingtalk.app-secret:}")
    private String appSecret;
    @Value("${live.gift.inventory.enabled:false}")
    private boolean giftInventoryEnabled;
    @Value("${live.dingtalk.default-password:xk123456}")
    private String defaultPassword;

    @Override public List<Map<String, Object>> listLiveUserOptions(Map<String, Object> q) { return mapper.selectLiveUserOptions(q); }
    @Override public List<Map<String, Object>> listShopOptions() { return mapper.selectShopOptions(); }

    @Override public List<Map<String, Object>> listShops(Map<String, Object> q) { return mapper.selectShops(q); }
    @Override public Map<String, Object> getShop(Long id) { return mapper.selectShopById(id); }

    @Override
    @Transactional
    public void saveShop(Map<String, Object> value, String username)
    {
        required(value, "shopCode", "店铺编码不能为空");
        required(value, "shopName", "店铺名称不能为空");
        value.putIfAbsent("status", "0"); value.put("username", username);
        try
        {
            if (value.get("shopId") == null) mapper.insertShop(value); else mapper.updateShop(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("店铺编码已经存在");
        }
    }

    @Override public List<Map<String, Object>> listRooms(Map<String, Object> q) { return mapper.selectRooms(q); }
    @Override public Map<String, Object> getRoom(Long id) { return mapper.selectRoomById(id); }

    @Override
    @Transactional
    public void saveRoom(Map<String, Object> value, String username)
    {
        required(value, "roomCode", "直播间编码不能为空");
        required(value, "roomName", "直播间名称不能为空");
        value.put("username", username);
        value.putIfAbsent("status", "0");
        try
        {
            if (value.get("roomId") == null) mapper.insertRoom(value); else mapper.updateRoom(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("直播间编码已经存在");
        }
    }

    @Override public List<Map<String, Object>> listSubjectMappings(Map<String, Object> q) { return mapper.selectSubjectMappings(q); }

    @Override
    @Transactional
    public void saveSubjectMapping(Map<String, Object> value, String username)
    {
        String subjectType = Objects.toString(value.get("subjectType"), "").toUpperCase();
        if (!Set.of("ROOM", "SHOP").contains(subjectType)) throw new ServiceException("映射对象类型不正确");
        required(value, "subjectId", "请选择映射对象");
        Long subjectId = Long.valueOf(value.get("subjectId").toString());
        if ("ROOM".equals(subjectType) && mapper.selectRoomById(subjectId) == null) throw new ServiceException("直播间不存在");
        if ("SHOP".equals(subjectType) && mapper.selectShopById(subjectId) == null) throw new ServiceException("店铺不存在");
        mapper.deleteSubjectMappings(subjectType, subjectId);
        longList(value.get("anchorIds")).stream().distinct()
            .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "anchor", username));
        longList(value.get("controllerIds")).stream().distinct()
            .forEach(userId -> mapper.insertSubjectMapping(subjectType, subjectId, userId, "controller", username));
    }

    @Override public List<Map<String, Object>> listDaily(Map<String, Object> q) { return mapper.selectDailyRecords(q); }
    @Override public Map<String, Object> getDaily(Long id) { return mapper.selectDailyById(id); }

    @Override
    @Transactional
    public void saveDaily(Map<String, Object> value, String username)
    {
        required(value, "liveDate", "请选择直播日期");
        required(value, "shopId", "请选择店铺");
        required(value, "roomId", "请选择直播间");
        List<Long> anchors = longList(value.get("anchorIds"));
        List<Long> controllers = longList(value.get("controllerIds"));
        if (anchors.isEmpty()) throw new ServiceException("至少选择一名主播");
        if (controllers.isEmpty()) throw new ServiceException("至少选择一名场控");
        value.put("username", username);
        try
        {
            if (value.get("dailyId") == null) mapper.insertDaily(value); else mapper.updateDaily(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("同一天、同一店铺和直播间只能登记一次");
        }
        Long dailyId = Long.valueOf(value.get("dailyId").toString());
        mapper.deleteDailyStaff(dailyId);
        anchors.stream().distinct().forEach(id -> mapper.insertDailyStaff(dailyId, id, "anchor"));
        controllers.stream().distinct().forEach(id -> mapper.insertDailyStaff(dailyId, id, "controller"));
    }

    @Override
    public List<Map<String, Object>> listGifts(Map<String, Object> query)
    {
        String keyword = Objects.toString(query.get("keyword"), "").trim();
        if (keyword.isEmpty() || !keyword.matches("[A-Za-z0-9]+")) return mapper.selectGifts(query);
        Map<String, Object> allQuery = new HashMap<>(query);
        allQuery.remove("keyword");
        String needle = keyword.toUpperCase();
        return mapper.selectGifts(allQuery).stream().filter(gift -> giftSearchText(gift).contains(needle)).toList();
    }

    @Override
    @Transactional
    public void saveGiftPreference(Map<String, Object> value, Long userId, String username)
    {
        required(value, "giftId", "礼品不能为空");
        if (userId == null) throw new ServiceException("当前用户无效");
        Long giftId = Long.valueOf(value.get("giftId").toString());
        if (mapper.selectGiftById(giftId) == null) throw new ServiceException("礼品不存在");
        Map<String, Object> preference = new HashMap<>();
        preference.put("userId", userId);
        preference.put("giftId", giftId);
        preference.put("hidden", booleanValue(value.get("hidden")) ? 1 : 0);
        preference.put("pinned", booleanValue(value.get("pinned")) ? 1 : 0);
        preference.put("sortOrder", value.get("sortOrder") == null ? 0 : Integer.valueOf(value.get("sortOrder").toString()));
        preference.put("username", username);
        mapper.upsertGiftPreference(preference);
    }

    private static boolean booleanValue(Object value)
    {
        return value == Boolean.TRUE || "true".equalsIgnoreCase(Objects.toString(value, ""))
            || "1".equals(Objects.toString(value, "")) || "是".equals(value);
    }

    private static String giftSearchText(Map<String, Object> gift)
    {
        String name = Objects.toString(gift.get("giftName"), "");
        String aliases = Objects.toString(gift.get("aliases"), "");
        StringBuilder result = new StringBuilder();
        result.append(Objects.toString(gift.get("giftCode"), "")).append('|')
            .append(name).append('|').append(Objects.toString(gift.get("shortName"), "")).append('|')
            .append(aliases).append('|').append(pinyinInitials(name));
        stringList(aliases).forEach(alias -> result.append('|').append(pinyinInitials(alias)));
        return result.toString().toUpperCase();
    }

    @Override
    public Map<String, Object> getGift(Long id)
    {
        Map<String, Object> value = mapper.selectGiftById(id);
        if (value != null)
        {
            value.put("costs", mapper.selectGiftCosts(id));
            value.put("versions", mapper.selectGiftVersions(id));
        }
        return value;
    }

    @Override
    @Transactional
    public void saveGift(Map<String, Object> value, String username)
    {
        boolean isNew = value.get("giftId") == null;
        required(value, "giftCode", "礼品编码不能为空");
        required(value, "giftName", "礼品名称不能为空");
        String giftName = value.get("giftName").toString().trim();
        value.put("giftName", giftName);
        Long giftId = isNew ? null : Long.valueOf(value.get("giftId").toString());
        if (isNew)
        {
            if (hasRow(mapper.selectGiftByName(giftName, null))) throw new ServiceException("礼品名称已经存在，不能重复新建");
        }
        else
        {
            Map<String, Object> original = mapper.selectGiftById(giftId);
            if (original == null) throw new ServiceException("礼品不存在");
            if (!Objects.equals(Objects.toString(original.get("giftCode"), ""), Objects.toString(value.get("giftCode"), "")))
                throw new ServiceException("礼品编码创建后不允许修改");
            if (!Objects.equals(Objects.toString(original.get("giftName"), ""), giftName)
                && hasRow(mapper.selectGiftByName(giftName, giftId)))
                throw new ServiceException("礼品名称已经存在");
        }
        value.putIfAbsent("unit", "件");
        value.putIfAbsent("status", "0");
        value.putIfAbsent("sortOrder", 100);
        value.put("username", username);
        try
        {
            if (value.get("giftId") == null) mapper.insertGift(value); else mapper.updateGift(value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("礼品编码已经存在");
        }
        giftId = Long.valueOf(value.get("giftId").toString());
        final Long savedGiftId = giftId;
        mapper.deleteGiftAliases(savedGiftId);
        LinkedHashSet<String> aliases = new LinkedHashSet<>(stringList(value.get("aliases")));
        aliases.add(pinyinInitials(giftName));
        new ArrayList<>(aliases).stream().map(LiveGiftServiceImpl::pinyinInitials)
            .filter(StringUtils::isNotBlank).forEach(aliases::add);
        aliases.stream().map(String::trim).filter(StringUtils::isNotEmpty)
            .filter(alias -> !alias.equalsIgnoreCase(giftName)).forEach(alias -> mapper.insertGiftAlias(savedGiftId, alias));
        mapper.insertGiftVersion(savedGiftId, isNew ? "CREATE" : Objects.toString(value.get("actionType"), "EDIT"), username);
        if (isNew && value.get("unitCost") != null)
        {
            required(value, "effectiveDate", "请选择首个成本的生效日期");
            BigDecimal cost = new BigDecimal(value.get("unitCost").toString());
            if (cost.signum() < 0) throw new ServiceException("单位成本不能小于0");
            if (cost.signum() == 0 && StringUtils.isBlank(Objects.toString(value.get("changeReason"), "")))
                throw new ServiceException("0元成本必须填写原因");
            Map<String, Object> initialCost = new HashMap<>();
            initialCost.put("giftId", giftId); initialCost.put("unitCost", cost);
            initialCost.put("effectiveDate", value.get("effectiveDate"));
            initialCost.put("changeReason", value.get("changeReason")); initialCost.put("username", username);
            mapper.insertGiftCost(initialCost);
        }
    }

    private static boolean hasRow(Map<String, Object> value)
    {
        return value != null && !value.isEmpty();
    }

    @Override
    public void addCost(Map<String, Object> value, String username)
    {
        required(value, "giftId", "礼品不能为空");
        required(value, "unitCost", "请输入单位成本");
        required(value, "effectiveDate", "请选择生效日期");
        BigDecimal cost = new BigDecimal(value.get("unitCost").toString());
        if (cost.signum() < 0) throw new ServiceException("单位成本不能小于0");
        if (cost.signum() == 0 && StringUtils.isBlank(Objects.toString(value.get("changeReason"), "")))
            throw new ServiceException("0元成本必须填写原因");
        value.put("username", username);
        mapper.insertGiftCost(value);
    }

    @Override public List<Map<String, Object>> listQuickTemplates(Long userId) { return mapper.selectQuickTemplates(userId); }
    @Override public Map<String, Object> getQuickTemplate(Long id, Long userId) { return mapper.selectQuickTemplate(id, userId); }

    @Override
    @Transactional
    public void saveQuickTemplate(Map<String, Object> value, Long userId, String username)
    {
        required(value, "templateName", "模板名称不能为空");
        Object content = value.get("content");
        if (content == null) throw new ServiceException("请至少配置一个模板字段");
        String contentJson = content instanceof String ? content.toString() : JSON.toJSONString(content);
        if (contentJson.length() > 30000) throw new ServiceException("模板内容过长");
        value.put("contentJson", contentJson); value.put("userId", userId);
        value.put("username", username); value.putIfAbsent("status", "0"); value.putIfAbsent("sortOrder", 100);
        try
        {
            if (value.get("templateId") == null) mapper.insertQuickTemplate(value); else if (mapper.updateQuickTemplate(value) == 0)
                throw new ServiceException("模板不存在或无权修改");
        }
        catch (ServiceException ex) { throw ex; }
        catch (Exception ex) { throw new ServiceException("模板名称已经存在"); }
    }

    @Override
    public void deleteQuickTemplate(Long id, Long userId)
    {
        if (mapper.deleteQuickTemplate(id, userId) == 0) throw new ServiceException("模板不存在或无权删除");
    }

    @Override
    @Transactional
    public Map<String, Object> importGiftCosts(List<LiveGiftCostImportRow> rows, String username)
    {
        if (rows == null || rows.isEmpty()) throw new ServiceException("导入文件没有数据");
        int success = 0;
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++)
        {
            LiveGiftCostImportRow row = rows.get(index);
            try
            {
                if (StringUtils.isBlank(row.getGiftCode()) || StringUtils.isBlank(row.getGiftName()))
                    throw new ServiceException("礼品编码和名称不能为空");
                if (row.getUnitCost() == null || row.getEffectiveDate() == null)
                    throw new ServiceException("单位成本和生效日期不能为空");
                if (row.getUnitCost().signum() < 0) throw new ServiceException("单位成本不能小于0");
                if (row.getUnitCost().signum() == 0 && StringUtils.isBlank(row.getRemark()))
                    throw new ServiceException("0元成本必须填写备注说明");
                LocalDate effectiveDate = toDate(row.getEffectiveDate());
                Map<String, Object> gift = mapper.selectGiftByCode(row.getGiftCode().trim());
                if (gift != null && mapper.selectGiftCostByDate(Long.valueOf(gift.get("giftId").toString()), effectiveDate) != null)
                    throw new ServiceException("该礼品在这个生效日期已经有成本记录");
                if (gift == null)
                {
                    gift = new HashMap<>(); gift.put("giftCode", row.getGiftCode().trim());
                    gift.put("giftName", row.getGiftName().trim()); gift.put("unit", "件"); gift.put("status", "0");
                    gift.put("sortOrder", 100); gift.put("remark", row.getRemark()); gift.put("username", username);
                    mapper.insertGift(gift);
                }
                Map<String, Object> cost = new HashMap<>(); cost.put("giftId", gift.get("giftId"));
                cost.put("unitCost", row.getUnitCost());
                cost.put("effectiveDate", effectiveDate);
                cost.put("changeReason", row.getRemark()); cost.put("username", username);
                mapper.insertGiftCost(cost);
                success++;
            }
            catch (Exception ex)
            {
                failures.add("第" + (index + 2) + "行 " + Objects.toString(row.getGiftCode(), "") + "：" + ex.getMessage());
            }
        }
        return Map.of("total", rows.size(), "success", success, "failure", failures.size(), "failures", failures);
    }

    @Override
    public Map<String, Object> getOrder(String orderNo)
    {
        String normalized = normalizeOrderNo(orderNo);
        Map<String, Object> order = mapper.selectOrderByNo(normalized);
        if (order == null)
        {
            order = new LinkedHashMap<>();
            order.put("orderNo", normalized);
            order.put("orderDate", LocalDate.now());
            order.put("synced", false);
        }
        else
        {
            order.put("synced", true);
        }
        Map<String, Object> giftStatus = mapper.selectOrderGiftStatus(normalized);
        order.put("giftStatus", giftStatus);
        order.put("gifts", mapper.selectOrderGiftItems(normalized));
        order.put("entryMode", giftStatus == null ? "create" : "edit");
        return order;
    }

    @Override
    @Transactional
    public void saveOrderGift(LiveGiftSaveRequest request, String username)
    {
        saveOrderGiftInternal(request, username);
    }

    @Override
    @Transactional
    public Map<String, Object> batchSaveOrderGifts(LiveGiftBatchSaveRequest request, String username)
    {
        LinkedHashSet<String> orderNos = new LinkedHashSet<>();
        request.getOrderNos().stream().filter(Objects::nonNull).map(String::trim)
            .filter(StringUtils::isNotEmpty).forEach(orderNos::add);
        if (orderNos.isEmpty()) throw new ServiceException("请至少输入一个订单号");
        if (orderNos.size() > MAX_BATCH_ORDERS) throw new ServiceException("单次最多处理500个订单");
        List<String> skipped = new ArrayList<>();
        List<String> targets = new ArrayList<>();
        for (String orderNo : orderNos)
        {
            if (!Boolean.TRUE.equals(request.getOverwriteExisting()) && hasRow(mapper.selectOrderGiftStatus(orderNo)))
                skipped.add(orderNo);
            else targets.add(orderNo);
        }
        for (String orderNo : targets) saveOrderGiftInternal(batchItem(request, orderNo), username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", orderNos.size());
        result.put("success", targets.size());
        result.put("skipped", skipped.size());
        result.put("skippedOrderNos", skipped);
        result.put("failure", 0);
        result.put("failures", List.of());
        return result;
    }

    private static LiveGiftSaveRequest batchItem(LiveGiftBatchSaveRequest source, String orderNo)
    {
        LiveGiftSaveRequest target = new LiveGiftSaveRequest();
        target.setOrderNo(orderNo); target.setProcessStatus(source.getGifts().isEmpty() ? "not_applicable" : "selected");
        target.setDailyId(source.getDailyId()); target.setGifts(source.getGifts()); target.setOperatorNote(source.getOperatorNote());
        target.setRoomId(source.getRoomId()); target.setRoomCodeSnapshot(source.getRoomCodeSnapshot()); target.setRoomNameSnapshot(source.getRoomNameSnapshot());
        target.setAnchorUserId(source.getAnchorUserId()); target.setAnchorNameSnapshot(source.getAnchorNameSnapshot());
        target.setControllerUserId(source.getControllerUserId()); target.setControllerNameSnapshot(source.getControllerNameSnapshot());
        target.setRefundAmount(source.getRefundAmount()); target.setRefundReason(source.getRefundReason()); target.setOtherRemark(source.getOtherRemark());
        target.setAfterSaleCompensation(source.getAfterSaleCompensation()); target.setServiceMark(source.getServiceMark());
        target.setExtendedWarranty(source.getExtendedWarranty()); target.setPriceProtection(source.getPriceProtection()); target.setDelayed(source.getDelayed());
        target.setFollowUp(source.getFollowUp()); target.setUrgent(source.getUrgent()); target.setTemplateId(source.getTemplateId());
        target.setTemplateNameSnapshot(source.getTemplateNameSnapshot()); target.setParsedText(source.getParsedText());
        return target;
    }

    private void saveOrderGiftInternal(LiveGiftSaveRequest request, String username)
    {
        String orderNo = normalizeOrderNo(request.getOrderNo());
        Map<String, Object> order = mapper.selectOrderByNo(orderNo);
        Map<String, Object> existingStatus = mapper.selectOrderGiftStatus(orderNo);
        boolean exists = existingStatus != null && !existingStatus.isEmpty();
        if (!GIFT_STATUSES.contains(request.getProcessStatus())) throw new ServiceException("礼品处理状态不正确");
        if (!"selected".equals(request.getProcessStatus()) && !request.getGifts().isEmpty())
            throw new ServiceException("只有“已选择礼品”状态可以添加礼品");
        if ("selected".equals(request.getProcessStatus()) && request.getGifts().isEmpty())
            throw new ServiceException("请至少选择一个礼品");
        if (request.getRefundAmount() != null && request.getRefundAmount().signum() < 0)
            throw new ServiceException("到返金额不能小于0");
        checkLength(request.getAnchorNameSnapshot(), 120, "主播名称");
        checkLength(request.getControllerNameSnapshot(), 120, "场控名称");
        checkLength(request.getRefundReason(), 500, "到返理由");
        checkLength(request.getOtherRemark(), 500, "其他备注");
        checkLength(request.getAfterSaleCompensation(), 500, "售后补偿");
        checkLength(request.getServiceMark(), 500, "服务标记");
        checkLength(request.getTemplateNameSnapshot(), 100, "模板名称");
        checkLength(request.getParsedText(), 2000, "结构化预览");

        Object orderDateValue = order == null ? null : order.get("orderDate");
        LocalDate orderDate = orderDateValue == null ? LocalDate.now() : toDate(orderDateValue);
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (Map<String, Object> gift : request.getGifts())
        {
            if (gift.get("giftId") == null || gift.get("quantity") == null) throw new ServiceException("礼品明细不完整");
            Long giftId = Long.valueOf(gift.get("giftId").toString());
            int quantity = Integer.parseInt(gift.get("quantity").toString());
            if (quantity < 1 || quantity > 10) throw new ServiceException("礼品数量必须在1到10之间");
            merged.merge(giftId, quantity, Integer::sum);
        }

        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : merged.entrySet())
        {
            Map<String, Object> gift = mapper.selectGiftById(entry.getKey());
            if (gift == null || !"0".equals(gift.get("status"))) throw new ServiceException("所选礼品不存在或已停用");
            Map<String, Object> cost = mapper.selectApplicableCost(entry.getKey(), orderDate);
            if (cost == null) throw new ServiceException(gift.get("giftName") + "在订单日期没有可用成本");
            BigDecimal unitCost = new BigDecimal(cost.get("unitCost").toString());
            Map<String, Object> item = new HashMap<>();
            item.put("orderNo", orderNo); item.put("giftId", entry.getKey()); item.put("quantity", entry.getValue());
            item.put("giftName", gift.get("giftName")); item.put("specification", gift.get("specification"));
            item.put("unit", gift.get("unit")); item.put("unitCost", unitCost);
            item.put("totalCost", unitCost.multiply(BigDecimal.valueOf(entry.getValue())));
            item.put("effectiveDate", cost.get("effectiveDate")); item.put("username", username);
            snapshots.add(item);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("orderNo", orderNo); status.put("processStatus", request.getProcessStatus());
        status.put("dailyId", request.getDailyId());
        status.put("roomId", request.getRoomId());
        status.put("roomCodeSnapshot", request.getRoomCodeSnapshot());
        status.put("roomNameSnapshot", request.getRoomNameSnapshot());
        String otherRemark = StringUtils.isNotBlank(request.getOtherRemark()) ? request.getOtherRemark() : request.getOperatorNote();
        status.put("operatorNote", otherRemark);
        status.put("anchorUserId", request.getAnchorUserId());
        status.put("anchorNameSnapshot", request.getAnchorNameSnapshot());
        status.put("controllerUserId", request.getControllerUserId());
        status.put("controllerNameSnapshot", request.getControllerNameSnapshot());
        status.put("refundAmount", request.getRefundAmount());
        status.put("refundReason", request.getRefundReason());
        status.put("otherRemark", otherRemark);
        status.put("afterSaleCompensation", request.getAfterSaleCompensation());
        status.put("serviceMark", request.getServiceMark());
        status.put("extendedWarranty", Boolean.TRUE.equals(request.getExtendedWarranty()));
        status.put("priceProtection", Boolean.TRUE.equals(request.getPriceProtection()));
        status.put("delayed", Boolean.TRUE.equals(request.getDelayed()));
        status.put("followUp", Boolean.TRUE.equals(request.getFollowUp()));
        status.put("urgent", Boolean.TRUE.equals(request.getUrgent()));
        status.put("templateId", request.getTemplateId());
        status.put("templateNameSnapshot", request.getTemplateNameSnapshot());
        status.put("parsedText", request.getParsedText());
        status.put("username", username);
        reconcileOrderInventory(orderNo, "selected".equals(request.getProcessStatus()) ? merged : Map.of(), username);
        mapper.upsertOrderGiftStatus(status);
        mapper.deleteOrderGiftItems(orderNo);
        snapshots.forEach(mapper::insertOrderGift);
        mapper.insertOrderGiftLog(orderNo, exists ? "UPDATE" : "CREATE", JSON.toJSONString(request), username);
    }

    @Override public List<Map<String, Object>> ledger(Map<String, Object> q)
    {
        List<Map<String, Object>> rows = mapper.selectOrderGiftLedger(q);
        rows.forEach(row -> {
            Map<String, Object> status = mapper.selectOrderGiftStatus(Objects.toString(row.get("orderNo"), ""));
            row.put("roomId", status == null ? null : status.get("roomId"));
            row.put("roomNameSnapshot", status == null ? null : status.get("roomNameSnapshot"));
        });
        Object roomId = q.get("roomId");
        if (roomId == null || StringUtils.isBlank(roomId.toString())) return rows;
        return rows.stream().filter(row -> Objects.equals(Objects.toString(row.get("roomId"), ""), roomId.toString())).toList();
    }

    @Override public Map<String, Object> getRoomPreference(Long userId)
    {
        Map<String, Object> value = mapper.selectUserRoomPreference(userId);
        return value == null ? new LinkedHashMap<>() : value;
    }

    @Override public void saveRoomPreference(Long userId, Long roomId, String username)
    {
        if (roomId != null)
        {
            Map<String, Object> room = mapper.selectRoomById(roomId);
            if (room == null || !"0".equals(Objects.toString(room.get("status"), ""))) throw new ServiceException("直播间不存在或已停用");
        }
        mapper.upsertUserRoomPreference(userId, roomId, username);
    }

    @Override public List<Map<String, Object>> inventory(Map<String, Object> q) { return mapper.selectGiftInventory(q); }

    @Override public List<Map<String, Object>> inventoryMovements(Map<String, Object> q) { return mapper.selectGiftInventoryMovements(q); }

    @Override
    public Map<String, Object> inventorySummary(Map<String, Object> q)
    {
        Map<String, Object> result = new LinkedHashMap<>(mapper.selectGiftInventorySummary(q));
        result.put("lowStock", mapper.selectGiftInventoryLowStock(q));
        return result;
    }

    @Override
    @Transactional
    public void adjustInventory(Map<String, Object> value, String username)
    {
        required(value, "giftId", "礼品不能为空");
        Long giftId = Long.valueOf(value.get("giftId").toString());
        Map<String, Object> gift = mapper.selectGiftById(giftId);
        if (gift == null) throw new ServiceException("礼品不存在");
        String type = Objects.toString(value.get("movementType"), "in").trim().toLowerCase(Locale.ROOT);
        int quantity = value.get("quantity") == null ? 0 : Integer.parseInt(value.get("quantity").toString());
        if (quantity < 0) throw new ServiceException("库存数量不能小于0");
        Map<String, Object> balance = mapper.selectGiftInventoryBalanceForUpdate(giftId);
        int before = balance == null ? 0 : intValue(balance.get("stockQty"));
        int safety = value.get("safetyQty") == null ? (balance == null ? 0 : intValue(balance.get("safetyQty"))) : Integer.parseInt(value.get("safetyQty").toString());
        if (safety < 0) throw new ServiceException("安全库存不能小于0");
        int delta;
        String movementType;
        if ("set".equals(type))
        {
            delta = quantity - before; movementType = "SET";
        }
        else if ("out".equals(type))
        {
            delta = -quantity; movementType = "OUT";
        }
        else
        {
            delta = quantity; movementType = "IN";
        }
        int after = before + delta;
        if (after < 0) throw insufficientInventory(gift, before, quantity);
        Map<String, Object> data = new HashMap<>();
        data.put("giftId", giftId); data.put("stockQty", after); data.put("safetyQty", safety); data.put("username", username);
        if (balance == null) mapper.insertGiftInventory(data); else mapper.updateGiftInventory(data);
        if (delta != 0)
        {
            Map<String, Object> movement = new HashMap<>(); movement.put("giftId", giftId); movement.put("movementType", movementType);
            movement.put("quantity", delta); movement.put("beforeQty", before); movement.put("afterQty", after);
            movement.put("sourceType", "MANUAL"); movement.put("sourceNo", value.get("sourceNo")); movement.put("remark", value.get("remark")); movement.put("username", username);
            mapper.insertGiftInventoryMovement(movement);
        }
    }

    private void reconcileOrderInventory(String orderNo, Map<Long, Integer> desired, String username)
    {
        if (!giftInventoryEnabled) return;
        Map<Long, Integer> old = new LinkedHashMap<>();
        List<Map<String, Object>> allocations = mapper.selectOrderInventoryAllocations(orderNo);
        // A null result is retained as a compatibility guard for deployments that have not run
        // the inventory migration yet; the production mapper returns an empty list when enabled.
        if (allocations == null) return;
        for (Map<String, Object> row : allocations)
            old.put(Long.valueOf(row.get("giftId").toString()), Integer.valueOf(row.get("quantity").toString()));
        for (Map.Entry<Long, Integer> entry : old.entrySet())
            changeInventory(entry.getKey(), entry.getValue(), "ORDER_RESTORE", orderNo, "订单修改恢复原扣减", username);
        mapper.deleteOrderInventoryAllocations(orderNo);
        for (Map.Entry<Long, Integer> entry : desired.entrySet())
        {
            changeInventory(entry.getKey(), -entry.getValue(), "ORDER_OUT", orderNo, "订单礼品自动扣库存", username);
            Map<String, Object> allocation = new HashMap<>(); allocation.put("orderNo", orderNo); allocation.put("giftId", entry.getKey());
            allocation.put("quantity", entry.getValue()); allocation.put("username", username); mapper.insertOrderInventoryAllocation(allocation);
        }
    }

    private void changeInventory(Long giftId, int delta, String movementType, String sourceNo, String remark, String username)
    {
        Map<String, Object> gift = mapper.selectGiftById(giftId);
        Map<String, Object> balance = mapper.selectGiftInventoryBalanceForUpdate(giftId);
        int before = balance == null ? 0 : intValue(balance.get("stockQty"));
        int after = before + delta;
        if (after < 0) throw insufficientInventory(gift, before, -delta);
        Map<String, Object> data = new HashMap<>(); data.put("giftId", giftId); data.put("stockQty", after);
        data.put("safetyQty", balance == null ? 0 : intValue(balance.get("safetyQty"))); data.put("username", username);
        if (balance == null) mapper.insertGiftInventory(data); else mapper.updateGiftInventory(data);
        Map<String, Object> movement = new HashMap<>(); movement.put("giftId", giftId); movement.put("movementType", movementType);
        movement.put("quantity", delta); movement.put("beforeQty", before); movement.put("afterQty", after); movement.put("sourceType", "ORDER");
        movement.put("sourceNo", sourceNo); movement.put("remark", remark); movement.put("username", username); mapper.insertGiftInventoryMovement(movement);
    }

    private ServiceException insufficientInventory(Map<String, Object> gift, int current, int requested)
    {
        return new ServiceException("礼品“" + Objects.toString(gift == null ? null : gift.get("giftName"), "未知礼品")
            + "”库存不足：当前 " + current + " 件，需要 " + requested + " 件。请先入库或调整库存后再保存订单。");
    }

    @Override
    public Map<String, Object> summary(Map<String, Object> q)
    {
        Map<String, Object> result = new LinkedHashMap<>(mapper.selectGiftSummary(q));
        result.put("groups", mapper.selectGiftSummaryGroups(q));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> syncDingTalk(String username)
    {
        if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret))
            throw new ServiceException("请先配置 DINGTALK_APP_KEY 和 DINGTALK_APP_SECRET");
        Map<String, Object> log = new HashMap<>();
        log.put("username", username);
        try
        {
            String token = getDingAccessToken();
            Map<Long, DingDepartment> departments = listDingDepartments(token);
            Map<String, Map<String, Object>> users = new LinkedHashMap<>();
            for (Long departmentId : departments.keySet())
            {
                listDingUsers(token, departmentId)
                    .forEach(user ->
                    {
                        String dingUserId = Objects.toString(user.get("dingUserId"), "");
                        if (!IGNORED_DING_USER_IDS.contains(dingUserId)) users.put(dingUserId, user);
                    });
            }
            String syncBatch = UUID.randomUUID().toString();
            Map<Long, Long> departmentIds = syncDepartments(departments, syncBatch, username);
            int success = 0;
            List<String> failures = new ArrayList<>();
            for (Map<String, Object> user : users.values())
            {
                String dingUserId = Objects.toString(user.get("dingUserId"), "");
                mapper.touchDingUserBinding(dingUserId, syncBatch);
                try
                {
                    syncSystemUser(user, departmentIds, syncBatch, username);
                    success++;
                }
                catch (Exception ex)
                {
                    failures.add(Objects.toString(user.get("nickName"), dingUserId) + "：" + ex.getMessage());
                }
            }
            int disabled = mapper.disableMissingDingUsers(syncBatch, username);
            IGNORED_DING_USER_IDS.forEach(mapper::deleteDingUserBinding);
            mapper.disableMissingDingDepartments(syncBatch, username);
            log.put("syncStatus", "success"); log.put("totalCount", users.size());
            log.put("successCount", success); log.put("failureCount", failures.size());
            log.put("errorMessage", failures.isEmpty() ? null : abbreviate(String.join("；", failures), 900));
            mapper.insertDingSyncLog(log);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", users.size()); result.put("success", success);
            result.put("failure", failures.size()); result.put("disabled", disabled);
            result.put("ignored", IGNORED_DING_USER_IDS.size());
            result.put("departments", departments.size()); result.put("failures", failures);
            return result;
        }
        catch (Exception ex)
        {
            log.put("syncStatus", "failed"); log.put("totalCount", 0); log.put("successCount", 0);
            log.put("failureCount", 1); log.put("errorMessage", abbreviate(ex.getMessage(), 900));
            mapper.insertDingSyncLog(log);
            if (ex instanceof ServiceException serviceException) throw serviceException;
            throw new ServiceException("钉钉通讯录同步失败：" + ex.getMessage());
        }
    }

    private String getDingAccessToken() throws Exception
    {
        JSONObject body = new JSONObject(); body.put("appKey", appKey); body.put("appSecret", appSecret);
        JSONObject response = postJson(DING_API + "/v1.0/oauth2/accessToken", body, null);
        String token = response.getString("accessToken");
        if (StringUtils.isBlank(token)) throw new ServiceException("钉钉未返回访问令牌");
        return token;
    }

    private Map<Long, DingDepartment> listDingDepartments(String token) throws Exception
    {
        Map<Long, DingDepartment> result = new LinkedHashMap<>();
        result.put(1L, new DingDepartment(1L, 0L, "钉钉通讯录", 100));
        List<Long> queue = new ArrayList<>(); queue.add(1L);
        for (int index = 0; index < queue.size(); index++)
        {
            JSONObject body = new JSONObject(); body.put("dept_id", queue.get(index));
            JSONObject response = postOldDingApi("/topapi/v2/department/listsub", body, token);
            JSONArray departments = response.getJSONArray("result");
            if (departments == null) continue;
            for (Object value : departments)
            {
                JSONObject department = (JSONObject) value;
                Long id = department.getLong("dept_id");
                if (id != null && !result.containsKey(id))
                {
                    Long parentId = department.getLong("parent_id");
                    if (parentId == null) parentId = queue.get(index);
                    int orderNum = Math.max(1, Math.min(9999, department.getIntValue("order")));
                    result.put(id, new DingDepartment(id, parentId,
                        Objects.toString(department.getString("name"), "部门" + id), orderNum));
                    queue.add(id);
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> listDingUsers(String token, Long departmentId) throws Exception
    {
        List<Map<String, Object>> result = new ArrayList<>();
        long cursor = 0;
        boolean hasMore;
        do
        {
            JSONObject body = new JSONObject();
            body.put("dept_id", departmentId); body.put("cursor", cursor); body.put("size", 100);
            JSONObject response = postOldDingApi("/topapi/v2/user/list", body, token);
            JSONObject page = response.getJSONObject("result");
            if (page == null) break;
            JSONArray list = page.getJSONArray("list");
            if (list != null)
            {
                for (Object value : list)
                {
                    JSONObject user = (JSONObject) value;
                    Map<String, Object> row = new HashMap<>();
                    row.put("dingUserId", user.getString("userid")); row.put("nickName", user.getString("name"));
                    row.put("mobile", user.getString("mobile")); row.put("avatar", user.getString("avatar"));
                    row.put("departmentIds", longValues(user.getJSONArray("dept_id_list")));
                    row.put("sex", dingSex(user.getString("gender")));
                    row.put("status", Boolean.FALSE.equals(user.getBoolean("active")) ? "1" : "0");
                    if (StringUtils.isNotBlank(Objects.toString(row.get("dingUserId"), ""))) result.add(row);
                }
            }
            hasMore = Boolean.TRUE.equals(page.getBoolean("has_more"));
            cursor = page.getLongValue("next_cursor");
        }
        while (hasMore);
        return result;
    }

    private Map<Long, Long> syncDepartments(Map<Long, DingDepartment> departments, String syncBatch,
                                             String username)
    {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (DingDepartment department : departments.values())
        {
            Long parentSysId = department.parentId() == 0 ? 0L : result.get(department.parentId());
            if (parentSysId == null) throw new ServiceException("钉钉部门层级不完整：" + department.name());
            String ancestors = "0";
            if (parentSysId != 0)
            {
                Map<String, Object> parent = mapper.selectDingDepartmentBinding(department.parentId());
                if (parent == null) throw new ServiceException("没有找到上级部门：" + department.name());
                ancestors = parent.get("ancestors") + "," + parentSysId;
            }
            Map<String, Object> binding = mapper.selectDingDepartmentBinding(department.id());
            Map<String, Object> value = new HashMap<>();
            value.put("parentId", parentSysId); value.put("ancestors", ancestors);
            value.put("deptName", abbreviate(department.name(), 30)); value.put("orderNum", department.orderNum());
            value.put("username", username);
            if (binding == null)
            {
                Map<String, Object> existing = mapper.selectSystemDepartment(parentSysId, value.get("deptName").toString());
                if (existing == null) mapper.insertSystemDepartment(value);
                else value.put("sysDeptId", existing.get("sysDeptId"));
            }
            else value.put("sysDeptId", binding.get("sysDeptId"));
            mapper.updateSystemDepartment(value);
            value.put("dingDeptId", department.id()); value.put("parentDingDeptId", department.parentId());
            value.put("syncBatch", syncBatch);
            mapper.upsertDingDepartmentBinding(value);
            result.put(department.id(), Long.valueOf(value.get("sysDeptId").toString()));
        }
        return result;
    }

    private void syncSystemUser(Map<String, Object> source, Map<Long, Long> departmentIds,
                                String syncBatch, String username)
    {
        String dingUserId = Objects.toString(source.get("dingUserId"), "").trim();
        String mobile = Objects.toString(source.get("mobile"), "").replaceAll("\\s+", "");
        if (!mobile.matches("^1[3-9]\\d{9}$")) throw new ServiceException("没有有效的中国大陆手机号");
        Map<String, Object> binding = mapper.selectDingUserBinding(dingUserId);
        Map<String, Object> value = new HashMap<>();
        value.put("userName", mobile); value.put("mobile", mobile);
        value.put("nickName", abbreviate(Objects.toString(source.get("nickName"), mobile), 30));
        value.put("avatar", abbreviate(Objects.toString(source.get("avatar"), ""), 100));
        value.put("sex", source.get("sex"));
        value.put("status", source.get("status")); value.put("operator", username);
        value.put("deptId", primaryDepartment(source.get("departmentIds"), departmentIds));
        if (binding == null)
        {
            Map<String, Object> existing = mapper.selectSystemUserByUserName(mobile);
            if (existing != null)
            {
                value.put("userId", existing.get("userId"));
            }
            else
            {
                value.put("password", SecurityUtils.encryptPassword(defaultPassword));
                mapper.insertSystemUser(value);
            }
        }
        else value.put("userId", binding.get("userId"));
        mapper.updateSystemUser(value);
        value.put("dingUserId", dingUserId); value.put("syncBatch", syncBatch);
        mapper.upsertDingUserBinding(value);
    }

    private static Long primaryDepartment(Object value, Map<Long, Long> departments)
    {
        if (value instanceof Collection<?> ids)
        {
            for (Object id : ids)
            {
                Long sysDeptId = departments.get(Long.valueOf(id.toString()));
                if (sysDeptId != null) return sysDeptId;
            }
        }
        return departments.get(1L);
    }

    private static List<Long> longValues(JSONArray values)
    {
        if (values == null) return List.of(1L);
        return values.stream().filter(Objects::nonNull).map(value -> Long.valueOf(value.toString())).toList();
    }

    private static String dingSex(String gender)
    {
        if ("1".equals(gender)) return "0";
        if ("2".equals(gender)) return "1";
        return "2";
    }

    private JSONObject postOldDingApi(String path, JSONObject body, String token) throws Exception
    {
        JSONObject response = postJson("https://oapi.dingtalk.com" + path + "?access_token=" + token, body, null);
        Integer code = response.getInteger("errcode");
        if (code != null && code != 0) throw new ServiceException("钉钉接口返回：" + response.getString("errmsg"));
        return response;
    }

    private JSONObject postJson(String url, JSONObject body, String token) throws Exception
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()));
        if (StringUtils.isNotBlank(token)) builder.header("x-acs-dingtalk-access-token", token);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new ServiceException("钉钉接口HTTP状态：" + response.statusCode());
        return JSON.parseObject(response.body());
    }

    private static String normalizeOrderNo(String value)
    {
        String result = Objects.toString(value, "").trim();
        if (result.isEmpty()) throw new ServiceException("订单号不能为空");
        if (result.length() > 64) throw new ServiceException("订单号长度不能超过64位");
        return result;
    }

    private static void checkLength(String value, int maxLength, String fieldName)
    {
        if (value != null && value.length() > maxLength)
            throw new ServiceException(fieldName + "长度不能超过" + maxLength + "个字符");
    }

    private static void required(Map<String, Object> value, String key, String message)
    {
        if (value.get(key) == null || StringUtils.isBlank(value.get(key).toString())) throw new ServiceException(message);
    }

    private static List<Long> longList(Object value)
    {
        if (value == null) return List.of();
        if (value instanceof Collection<?> collection)
            return collection.stream().filter(Objects::nonNull).map(item -> Long.valueOf(item.toString())).toList();
        return Arrays.stream(value.toString().split(",")).filter(StringUtils::isNotBlank).map(Long::valueOf).toList();
    }

    private static List<String> stringList(Object value)
    {
        if (value == null) return List.of();
        if (value instanceof Collection<?> collection)
            return collection.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                .filter(StringUtils::isNotEmpty).toList();
        return Arrays.stream(value.toString().split("[,，、\\n]"))
            .map(String::trim).filter(StringUtils::isNotEmpty).toList();
    }

    private static String pinyinInitials(String value)
    {
        StringBuilder result = new StringBuilder();
        for (char character : Objects.toString(value, "").toCharArray())
        {
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(character);
            if (pinyin != null && pinyin.length > 0 && StringUtils.isNotBlank(pinyin[0]))
                result.append(Character.toUpperCase(pinyin[0].charAt(0)));
            else if (ASCII_ALIAS_CHAR.matcher(String.valueOf(character)).matches())
                result.append(Character.toUpperCase(character));
        }
        return result.toString();
    }

    private static LocalDate toDate(Object value)
    {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof java.util.Date date) return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private static String abbreviate(String value, int max)
    {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static int intValue(Object value)
    {
        if (value == null) return 0;
        return Integer.parseInt(value.toString());
    }

    private record DingDepartment(Long id, Long parentId, String name, int orderNum) { }
}
