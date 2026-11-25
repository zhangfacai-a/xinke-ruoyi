package com.xinke.erp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.mapper.FreightCollectionMapper;

@Service
public class FreightCollectionService {
    private final FreightCollectionMapper mapper;

    public FreightCollectionService(FreightCollectionMapper mapper) { this.mapper=mapper; }

    public List<Map<String,Object>> shops() {
        return mapper.shops(SecurityUtils.getUserId(),canViewAll());
    }

    public Map<String,Object> list(Map<String,Object> input) {
        Map<String,Object> query=query(input,false);
        Map<String,Object> result=new HashMap<>(mapper.summary(query));
        result.put("rows",mapper.list(query));
        return result;
    }

    public List<Map<String,Object>> export(Map<String,Object> input) {
        Map<String,Object> query=query(input,true);
        if(((Number)mapper.summary(query).get("total")).longValue()>5000)
            throw new ServiceException("单次最多导出5000条，请缩短日期范围");
        return mapper.list(query);
    }

    public Map<String,Object> dashboard(Map<String,Object> input) {
        Map<String,Object> query=query(input,false);
        Map<String,Object> result=new HashMap<>(mapper.summary(query));
        result.put("daily",mapper.daily(query));
        return result;
    }

    @Transactional
    public Long create(Map<String,Object> input) {
        Map<String,Object> value=validated(input);
        try { mapper.insert(value); }
        catch(DuplicateKeyException ex) { throw new ServiceException("订单号已存在，请勿重复录入"); }
        return ((Number)value.get("id")).longValue();
    }

    @Transactional
    public void update(Long id,Map<String,Object> input) {
        Map<String,Object> old=mapper.get(id);
        if(old==null) throw new ServiceException("明细不存在");
        Long userId=SecurityUtils.getUserId();
        Long oldShop=((Number)old.get("shopId")).longValue();
        if(!SecurityUtils.isAdmin() && !userId.equals(((Number)old.get("createdByUserId")).longValue())
            && mapper.isManager(oldShop,userId)==0) throw new ServiceException("无权修改此明细");
        Map<String,Object> value=validated(input);
        value.put("id",id);
        Long version=longValue(input.get("version"));
        if(version==null || version<0) throw new ServiceException("请刷新后重试");
        value.put("version",version);
        try {
            if(mapper.update(value)!=1) throw new ServiceException("明细已被其他人修改，请刷新后重试");
        } catch(DuplicateKeyException ex) { throw new ServiceException("订单号已存在，请勿重复录入"); }
    }

    @Transactional
    public int importFile(MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>5*1024*1024)
            throw new ServiceException("请选择不超过5MB的 Excel 文件");
        String filename=Objects.toString(file.getOriginalFilename(),"").toLowerCase();
        if(!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))
            throw new ServiceException("只支持 Excel 文件");
        List<Map<String,Object>> pending=new ArrayList<>();
        Set<String> orderNos=new HashSet<>();
        Map<String,List<Long>> shopsByName=new HashMap<>();
        for(Map<String,Object> shop:shops()) shopsByName.computeIfAbsent(shop.get("shopName").toString(),key->new ArrayList<>())
            .add(((Number)shop.get("shopId")).longValue());
        try(Workbook workbook=WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet=workbook.getSheetAt(0);
            DataFormatter formatter=new DataFormatter();
            Row header=sheet.getRow(0);
            String[] expected={"店铺","订单状态","订单号","客服姓名","运费金额","划扣方式"};
            if(header==null) throw new ServiceException("Excel 表头不正确");
            for(int col=0;col<expected.length;col++)
                if(!expected[col].equals(cell(header,col,formatter))) throw new ServiceException("Excel 表头不正确：第"+(col+1)+"列应为"+expected[col]);
            if(sheet.getLastRowNum()>500) throw new ServiceException("单次最多导入500条");
            for(int index=1;index<=sheet.getLastRowNum();index++) {
                Row row=sheet.getRow(index);
                if(row==null) continue;
                String shopName=cell(row,0,formatter),orderNo=cell(row,2,formatter);
                if(shopName.isBlank() && orderNo.isBlank()) continue;
                if(row.getCell(2)!=null && row.getCell(2).getCellType()==CellType.FORMULA)
                    throw new ServiceException("第"+(index+1)+"行订单号不能使用公式");
                if(row.getCell(2)!=null && row.getCell(2).getCellType()==CellType.NUMERIC
                    && Math.abs(row.getCell(2).getNumericCellValue())>=1e15)
                    throw new ServiceException("第"+(index+1)+"行订单号超过 Excel 数字精度，请将该列设置为文本后重新导入");
                List<Long> matches=shopsByName.getOrDefault(shopName,List.of());
                if(matches.size()!=1) throw new ServiceException("第"+(index+1)+"行店铺不存在、无权限或名称重复");
                Map<String,Object> input=new HashMap<>();
                input.put("shopId",matches.get(0));
                input.put("orderNo",orderNo);
                input.put("deliveryStatus",normalizeStatus(cell(row,1,formatter)));
                input.put("freightAmount",cell(row,4,formatter));
                input.put("deductionMethod",normalizeMethod(cell(row,5,formatter)));
                Map<String,Object> value=validated(input);
                if(!orderNos.add(orderNo)) throw new ServiceException("第"+(index+1)+"行订单号在文件中重复");
                pending.add(value);
            }
        } catch(ServiceException ex) { throw ex; }
        catch(Exception ex) { throw new ServiceException("Excel 读取失败，请检查文件格式"); }
        if(pending.isEmpty()) throw new ServiceException("没有可导入的明细");
        for(Map<String,Object> value:pending) {
            try { mapper.insert(value); }
            catch(DuplicateKeyException ex) { throw new ServiceException("订单号"+value.get("orderNo")+"已存在，整份文件未导入"); }
        }
        return pending.size();
    }

    private Map<String,Object> validated(Map<String,Object> input) {
        Long shopId=longValue(input.get("shopId"));
        if(shopId==null) throw new ServiceException("请选择店铺");
        Map<String,Object> shop=mapper.shop(shopId,SecurityUtils.getUserId(),SecurityUtils.isAdmin());
        if(shop==null) throw new ServiceException("店铺不存在、已停用或未配置当前账号");
        String orderNo=Objects.toString(input.get("orderNo"),"").trim();
        if(orderNo.isEmpty() || orderNo.length()>64) throw new ServiceException("订单号应为1到64个字符");
        String status=Objects.toString(input.get("deliveryStatus"),"");
        if(!Set.of("SIGNED","SHIPPED_UNSIGNED").contains(status)) throw new ServiceException("订单状态不正确");
        String method=Objects.toString(input.get("deductionMethod"),"");
        if(!Set.of("ALIPAY_PERSONAL","ORDER_PAYMENT").contains(method)) throw new ServiceException("划扣方式不正确");
        BigDecimal amount;
        try { amount=new BigDecimal(Objects.toString(input.get("freightAmount"),"")); }
        catch(Exception ex) { throw new ServiceException("请填写运费金额"); }
        if(amount.signum()<=0 || amount.scale()>2 || amount.compareTo(new BigDecimal("999999999999.99"))>0)
            throw new ServiceException("运费金额须大于0，最多两位小数");
        String remark=Objects.toString(input.get("remark"),"").trim();
        if(remark.length()>500) throw new ServiceException("备注最多500字");
        LocalDateTime collectedAt=null;
        if(input.get("collectedAt")!=null && !input.get("collectedAt").toString().isBlank()) {
            try { collectedAt=LocalDateTime.parse(input.get("collectedAt").toString().replace(' ','T')); }
            catch(Exception ex) { throw new ServiceException("征收时间格式不正确"); }
        }
        Map<String,Object> value=new HashMap<>();
        value.put("shopId",shopId); value.put("shopName",shop.get("shopName")); value.put("orderNo",orderNo);
        value.put("deliveryStatus",status); value.put("freightAmount",amount); value.put("deductionMethod",method);
        value.put("deductionRate",method.equals("ALIPAY_PERSONAL")?new BigDecimal("0.09"):new BigDecimal("0.07"));
        value.put("collectedAt",collectedAt); value.put("remark",remark);
        value.put("actorId",SecurityUtils.getUserId());
        String name=SecurityUtils.getLoginUser().getUser().getNickName();
        value.put("actorName",name==null||name.isBlank()?SecurityUtils.getUsername():name);
        return value;
    }

    private Map<String,Object> query(Map<String,Object> input,boolean export) {
        LocalDate end,start;
        try {
            end=LocalDate.parse(Objects.toString(input.getOrDefault("endDate",LocalDate.now().toString())));
            start=LocalDate.parse(Objects.toString(input.getOrDefault("beginDate",end.minusDays(29).toString())));
        } catch(Exception ex) { throw new ServiceException("日期范围不正确"); }
        if(start.isAfter(end) || ChronoUnit.DAYS.between(start,end)>(export?30:365))
            throw new ServiceException(export?"单次最多导出31天":"查询范围最多一年");
        Map<String,Object> query=new HashMap<>();
        query.put("beginDate",start.toString()); query.put("endExclusive",end.plusDays(1).toString());
        Long shopId=longValue(input.get("shopId")); if(shopId!=null) query.put("shopId",shopId);
        String orderNo=Objects.toString(input.get("orderNo"),"").trim();
        if(orderNo.length()>64) throw new ServiceException("订单号搜索内容过长");
        if(!orderNo.isBlank()) query.put("orderNo",orderNo.replace("%","\\%").replace("_","\\_"));
        String creatorName=Objects.toString(input.get("creatorName"),"").trim();
        if(creatorName.length()>100) throw new ServiceException("录入人搜索内容过长");
        if(!creatorName.isBlank()) query.put("creatorName",creatorName.replace("%","\\%").replace("_","\\_"));
        String status=Objects.toString(input.get("deliveryStatus"),"");
        if(!status.isBlank()) { if(!Set.of("SIGNED","SHIPPED_UNSIGNED").contains(status)) throw new ServiceException("状态筛选不正确"); query.put("deliveryStatus",status); }
        String method=Objects.toString(input.get("deductionMethod"),"");
        if(!method.isBlank()) { if(!Set.of("ALIPAY_PERSONAL","ORDER_PAYMENT").contains(method)) throw new ServiceException("划扣方式筛选不正确"); query.put("deductionMethod",method); }
        int page=integer(input.get("pageNum"),1),size=integer(input.get("pageSize"),20);
        page=Math.max(1,Math.min(100000,page)); size=export?5000:Math.max(1,Math.min(100,size));
        query.put("limit",size); query.put("offset",export?0:(page-1)*size);
        if(!canViewAll()) query.put("scopeUserId",SecurityUtils.getUserId());
        return query;
    }

    private boolean canViewAll() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasPermi("live:freight:viewAll");
    }

    private static String cell(Row row,int index,DataFormatter formatter) {
        return row.getCell(index)==null?"":formatter.formatCellValue(row.getCell(index)).trim();
    }
    private static String normalizeStatus(String value) {
        return switch(value) {
            case "已签收","发货已签收" -> "SIGNED";
            case "已发货未签收","发货未签收" -> "SHIPPED_UNSIGNED";
            default -> value;
        };
    }
    private static String normalizeMethod(String value) {
        return switch(value) {
            case "支付宝个人账户9%" -> "ALIPAY_PERSONAL";
            case "订单打款7%" -> "ORDER_PAYMENT";
            default -> value;
        };
    }
    private static Long longValue(Object value) {
        if(value==null || value.toString().isBlank()) return null;
        try { return Long.valueOf(value.toString()); }
        catch(NumberFormatException ex) { throw new ServiceException("编号格式不正确"); }
    }
    private static int integer(Object value,int fallback) {
        try { return Integer.parseInt(Objects.toString(value)); }
        catch(Exception ex) { return fallback; }
    }
}
