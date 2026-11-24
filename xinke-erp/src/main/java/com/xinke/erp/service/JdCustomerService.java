package com.xinke.erp.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;

@Service
public class JdCustomerService {
    private final JdbcTemplate db;
    public JdCustomerService(JdbcTemplate db) { this.db = db; }

    public Map<String,Object> records(Map<String,String> input) {
        int page = page(input.get("pageNum")), size = size(input.get("pageSize"));
        StringBuilder where = new StringBuilder(" where 1=1"); List<Object> args = new ArrayList<>();
        String search = text(input.get("search"),160);
        if (!search.isEmpty()) { where.append(" and (r.customer_nickname like ? or r.service_nickname like ? or exists (select 1 from jd_service_record_order ro where ro.record_id=r.record_id and ro.order_no like ?))");
            for (int i=0;i<3;i++) args.add("%"+search+"%"); }
        String status = text(input.get("status"),16);
        if (!status.isEmpty()) { if (!Set.of("MATCHED","NO_ORDER","CONFLICT").contains(status)) throw new ServiceException("匹配状态不正确"); where.append(" and r.match_status=?"); args.add(status); }
        Long total = db.queryForObject("select count(*) from jd_service_record r"+where,Long.class,args.toArray());
        List<Object> paged = new ArrayList<>(args); paged.add(size); paged.add((page-1)*size);
        var rows = db.queryForList("select r.record_id recordId,r.occurred_at occurredAt,r.service_nickname serviceNickname,r.customer_nickname customerNickname,r.raw_order_nos rawOrderNos,r.customer_id customerId,r.match_status matchStatus,r.source_row_no sourceRowNo,b.file_name fileName from jd_service_record r join jd_rpa_import_batch b on b.batch_id=r.batch_id"+where+" order by r.occurred_at desc,r.record_id desc limit ? offset ?",paged.toArray());
        return Map.of("total",total,"rows",rows);
    }

    public Map<String,Object> customers(Map<String,String> input) {
        int page=page(input.get("pageNum")), size=size(input.get("pageSize")); String search=text(input.get("search"),160);
        String where=" where (?='' or c.display_nickname like concat('%',?,'%') or c.contact_phone like concat('%',?,'%') or c.contact_wechat like concat('%',?,'%') or exists (select 1 from jd_customer_order o where o.customer_id=c.customer_id and o.order_no like concat('%',?,'%')))";
        Long total=db.queryForObject("select count(*) from jd_customer c"+where,Long.class,search,search,search,search,search);
        var rows=db.queryForList("select c.customer_id customerId,c.display_nickname displayNickname,c.contact_phone contactPhone,c.contact_wechat contactWechat,c.first_seen_at firstSeenAt,c.last_seen_at lastSeenAt,(select count(*) from jd_customer_order o where o.customer_id=c.customer_id) orderCount from jd_customer c"+where+" order by c.last_seen_at desc,c.customer_id desc limit ? offset ?",search,search,search,search,search,size,(page-1)*size);
        return Map.of("total",total,"rows",rows);
    }

    public Map<String,Object> customer(long id) {
        var rows=db.queryForList("select customer_id customerId,display_nickname displayNickname,contact_phone contactPhone,contact_wechat contactWechat,contact_updated_by contactUpdatedBy,contact_updated_at contactUpdatedAt,first_seen_at firstSeenAt,last_seen_at lastSeenAt from jd_customer where customer_id=?",id);
        if(rows.isEmpty()) throw new ServiceException("客户不存在");
        Map<String,Object> result=new HashMap<>(rows.get(0));
        result.put("orders",db.queryForList("select order_no orderNo,first_seen_at firstSeenAt,last_seen_at lastSeenAt from jd_customer_order where customer_id=? order by last_seen_at desc",id));
        return result;
    }

    public List<Map<String,Object>> recordOrders(long id) {
        if(db.queryForObject("select count(*) from jd_service_record where record_id=?",Integer.class,id)==0) throw new ServiceException("接待记录不存在");
        return db.queryForList("select ro.order_no orderNo,o.customer_id customerId,c.display_nickname displayNickname from jd_service_record_order ro left join jd_customer_order o on o.order_no=ro.order_no left join jd_customer c on c.customer_id=o.customer_id where ro.record_id=? order by ro.order_no",id);
    }

    @Transactional
    public void updateContact(long id,Map<String,String> input) {
        String phone=text(input.get("phone"),32),wechat=text(input.get("wechat"),100);
        if(!phone.isEmpty() && !phone.matches("[0-9+() -]{5,32}")) throw new ServiceException("手机号格式不正确");
        if(db.update("update jd_customer set contact_phone=?,contact_wechat=?,contact_updated_by=?,contact_updated_at=now() where customer_id=?",phone.isEmpty()?null:phone,wechat.isEmpty()?null:wechat,SecurityUtils.getUserId(),id)!=1) throw new ServiceException("客户不存在");
    }

    @Transactional
    public void linkOrder(Map<String,String> input) {
        String order=text(input.get("orderNo"),64); if(order.isEmpty()) throw new ServiceException("请输入订单号");
        long customerId; try { customerId=Long.parseLong(input.getOrDefault("customerId","")); } catch(Exception ex) { throw new ServiceException("请选择客户"); }
        customer(customerId);
        var existing=db.queryForList("select customer_id from jd_customer_order where order_no=? for update",order);
        if(!existing.isEmpty()) {
            if(((Number)existing.get(0).get("customer_id")).longValue()==customerId) return;
            throw new ServiceException("该订单已归属其他客户，不能直接改绑");
        }
        var seen=db.queryForList("select min(r.occurred_at) firstSeen,max(r.occurred_at) lastSeen from jd_service_record r join jd_service_record_order ro on ro.record_id=r.record_id where ro.order_no=?",order);
        if(seen.isEmpty() || seen.get(0).get("firstSeen")==null) throw new ServiceException("订单未出现在导入记录中");
        db.update("insert into jd_customer_order(order_no,customer_id,first_seen_at,last_seen_at) values(?,?,?,?)",order,customerId,seen.get(0).get("firstSeen"),seen.get(0).get("lastSeen"));
        db.update("update jd_customer set first_seen_at=least(first_seen_at,?),last_seen_at=greatest(last_seen_at,?) where customer_id=?",seen.get(0).get("firstSeen"),seen.get(0).get("lastSeen"),customerId);
        refreshRecords(order);
    }

    @Transactional
    public void mergeCustomers(Map<String,String> input) {
        long sourceId,targetId;
        try { sourceId=Long.parseLong(input.getOrDefault("sourceId","")); targetId=Long.parseLong(input.getOrDefault("targetId","")); }
        catch(Exception ex) { throw new ServiceException("请输入两个客户ID"); }
        if(sourceId==targetId) throw new ServiceException("不能合并同一客户");
        long first=Math.min(sourceId,targetId), second=Math.max(sourceId,targetId);
        var locked=db.queryForList("select customer_id,contact_phone,contact_wechat from jd_customer where customer_id in (?,?) order by customer_id for update",first,second);
        if(locked.size()!=2) throw new ServiceException("客户不存在");
        Map<String,Object> source=locked.get(sourceId==first?0:1),target=locked.get(targetId==first?0:1);
        for(String field:List.of("contact_phone","contact_wechat")) {
            String a=Objects.toString(source.get(field),""),b=Objects.toString(target.get(field),"");
            if(!a.isBlank()&&!b.isBlank()&&!a.equals(b)) throw new ServiceException("两个档案的联系方式不一致，请先人工核实并修改");
        }
        db.update("update jd_customer_order set customer_id=? where customer_id=?",targetId,sourceId);
        db.update("update jd_customer t join jd_customer s on s.customer_id=? set t.contact_phone=coalesce(t.contact_phone,s.contact_phone),t.contact_wechat=coalesce(t.contact_wechat,s.contact_wechat),t.first_seen_at=least(t.first_seen_at,s.first_seen_at),t.last_seen_at=greatest(t.last_seen_at,s.last_seen_at) where t.customer_id=?",sourceId,targetId);
        db.update("update jd_service_record set customer_id=?,match_status='MATCHED' where customer_id=?",targetId,sourceId);
        reconcileConflicts();
        db.update("delete from jd_customer where customer_id=?",sourceId);
    }

    private void reconcileConflicts() {
        for(Map<String,Object> row:db.queryForList("select record_id recordId,occurred_at occurredAt from jd_service_record where match_status='CONFLICT'")) {
            long recordId=((Number)row.get("recordId")).longValue();
            List<String> orderNos=db.queryForList("select order_no from jd_service_record_order where record_id=?",String.class,recordId);
            Set<Long> owners=new HashSet<>();
            for(String order:orderNos) db.queryForList("select customer_id from jd_customer_order where order_no=?",Long.class,order).forEach(owners::add);
            if(owners.size()!=1) continue;
            long owner=owners.iterator().next();
            for(String order:orderNos) db.update("insert into jd_customer_order(order_no,customer_id,first_seen_at,last_seen_at) values(?,?,?,?) on duplicate key update last_seen_at=greatest(last_seen_at,values(last_seen_at))",order,owner,row.get("occurredAt"),row.get("occurredAt"));
            for(String order:orderNos) refreshRecords(order);
        }
    }

    public List<Map<String,Object>> batches() { return db.queryForList("select batch_id batchId,file_name fileName,imported_at importedAt,source_row_count sourceRowCount,new_record_count newRecordCount,duplicate_record_count duplicateRecordCount from jd_rpa_import_batch order by batch_id desc limit 30"); }

    @Transactional
    public Map<String,Object> importFile(MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>10*1024*1024) throw new ServiceException("请选择不超过10MB的 .xlsx 或 .csv 文件");
        String name=Objects.toString(file.getOriginalFilename(),"");
        if(!name.toLowerCase(Locale.ROOT).endsWith(".xlsx") && !name.toLowerCase(Locale.ROOT).endsWith(".csv")) throw new ServiceException("只支持 .xlsx 和 .csv 文件");
        byte[] bytes; try { bytes=file.getBytes(); } catch(Exception ex) { throw new ServiceException("文件读取失败"); }
        String hash=hash(bytes);
        if(db.queryForObject("select count(*) from jd_rpa_import_batch where file_sha256=?",Integer.class,hash)>0) throw new ServiceException("这份文件已经导入过");
        List<SourceRow> rows=name.toLowerCase(Locale.ROOT).endsWith(".xlsx")?xlsx(bytes):csv(bytes);
        if(rows.isEmpty()) throw new ServiceException("文件中没有有效记录");
        if(rows.size()>10000) throw new ServiceException("单次最多导入10000行");
        org.springframework.jdbc.support.KeyHolder key=new GeneratedKeyHolder();
        db.update(con -> { var ps=con.prepareStatement("insert into jd_rpa_import_batch(file_sha256,file_name,source_date,imported_by_user_id,status) values(?,?,?,?,'IMPORTING')",java.sql.Statement.RETURN_GENERATED_KEYS); ps.setString(1,hash);ps.setString(2,name);ps.setDate(3,java.sql.Date.valueOf(rows.get(0).time.toLocalDate()));ps.setLong(4,SecurityUtils.getUserId());return ps; },key);
        long batch=Objects.requireNonNull(key.getKey()).longValue(); int added=0,duplicates=0;
        for(SourceRow row:rows) {
            List<String> orders=orders(row.rawOrders);
            List<String> sortedOrders=new ArrayList<>(orders); Collections.sort(sortedOrders);
            String dedup=hash((row.time+"\u001f"+row.agent+"\u001f"+row.nickname+"\u001f"+String.join(",",sortedOrders)).getBytes(StandardCharsets.UTF_8));
            if(db.queryForObject("select count(*) from jd_service_record where dedup_key=?",Integer.class,dedup)>0) { duplicates++; continue; }
            Set<Long> owners=new HashSet<>();
            for(String order:orders) db.queryForList("select customer_id from jd_customer_order where order_no=?",Long.class,order).forEach(owners::add);
            Long owner=null;
            if(!orders.isEmpty() && owners.size()<=1) {
                if(owners.isEmpty()) {
                    var customerKey=new GeneratedKeyHolder();
                    db.update(con -> { var ps=con.prepareStatement("insert into jd_customer(display_nickname,first_seen_at,last_seen_at) values(?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);ps.setString(1,row.nickname);ps.setTimestamp(2,Timestamp.valueOf(row.time));ps.setTimestamp(3,Timestamp.valueOf(row.time));return ps; },customerKey);
                    owner=Objects.requireNonNull(customerKey.getKey()).longValue();
                } else owner=owners.iterator().next();
                db.update("update jd_customer set display_nickname=case when last_seen_at<=? then ? else display_nickname end,first_seen_at=least(first_seen_at,?),last_seen_at=greatest(last_seen_at,?) where customer_id=?",Timestamp.valueOf(row.time),row.nickname,Timestamp.valueOf(row.time),Timestamp.valueOf(row.time),owner);
                for(String order:orders) db.update("insert into jd_customer_order(order_no,customer_id,first_seen_at,last_seen_at) values(?,?,?,?) on duplicate key update first_seen_at=least(first_seen_at,values(first_seen_at)),last_seen_at=greatest(last_seen_at,values(last_seen_at))",order,owner,Timestamp.valueOf(row.time),Timestamp.valueOf(row.time));
            }
            final Long recordOwner=owner; final String status=orders.isEmpty()?"NO_ORDER":owners.size()>1?"CONFLICT":"MATCHED";
            var recordKey=new GeneratedKeyHolder();
            db.update(con -> {var ps=con.prepareStatement("insert into jd_service_record(batch_id,source_row_no,dedup_key,occurred_at,service_nickname,customer_nickname,raw_order_nos,customer_id,match_status) values(?,?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);ps.setLong(1,batch);ps.setInt(2,row.line);ps.setString(3,dedup);ps.setTimestamp(4,Timestamp.valueOf(row.time));ps.setString(5,row.agent);ps.setString(6,row.nickname);ps.setString(7,row.rawOrders);if(recordOwner==null)ps.setNull(8,java.sql.Types.BIGINT);else ps.setLong(8,recordOwner);ps.setString(9,status);return ps;},recordKey);
            long record=Objects.requireNonNull(recordKey.getKey()).longValue();
            for(String order:orders) db.update("insert into jd_service_record_order(record_id,order_no) values(?,?)",record,order);
            added++;
        }
        db.update("update jd_rpa_import_batch set source_row_count=?,new_record_count=?,duplicate_record_count=?,status='COMPLETED' where batch_id=?",rows.size(),added,duplicates,batch);
        return Map.of("batchId",batch,"sourceRows",rows.size(),"imported",added,"duplicates",duplicates);
    }

    private void refreshRecords(String order) {
        db.update("update jd_service_record r set r.customer_id=(select min(o.customer_id) from jd_service_record_order ro join jd_customer_order o on o.order_no=ro.order_no where ro.record_id=r.record_id),r.match_status=case when (select count(distinct o.customer_id) from jd_service_record_order ro join jd_customer_order o on o.order_no=ro.order_no where ro.record_id=r.record_id)>1 then 'CONFLICT' else 'MATCHED' end where exists(select 1 from jd_service_record_order ro where ro.record_id=r.record_id and ro.order_no=?)",order);
        db.update("update jd_service_record r set customer_id=null where match_status='CONFLICT' and exists(select 1 from jd_service_record_order ro where ro.record_id=r.record_id and ro.order_no=?)",order);
    }

    static List<String> orders(String raw) {
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(String part:raw.split("[,，]")) { String order=part.trim(); if(!order.isEmpty()) {if(order.length()>64) throw new ServiceException("订单号不能超过64字符");result.add(order);} }
        return List.copyOf(result);
    }
    private static List<SourceRow> xlsx(byte[] bytes) {
        List<SourceRow> result=new ArrayList<>();
        try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet=book.getSheetAt(0);DataFormatter format=new DataFormatter();
            Row header=sheet.getRow(0);if(header==null) throw new ServiceException("缺少表头");
            int[] cols=columns(i -> header.getCell(i)==null?"":format.formatCellValue(header.getCell(i)).trim(),header.getLastCellNum());
            for(int i=1;i<=sheet.getLastRowNum();i++) { Row row=sheet.getRow(i);if(row==null)continue;
                String[] values=new String[4];for(int c=0;c<4;c++) {Cell cell=row.getCell(cols[c]);if(cell!=null && cell.getCellType()==CellType.FORMULA) throw new ServiceException("第"+(i+1)+"行不能包含公式");if(c==3 && cell!=null && cell.getCellType()==CellType.NUMERIC && Math.abs(cell.getNumericCellValue())>=1e15) throw new ServiceException("第"+(i+1)+"行订单号超过Excel数字精度，请设置为文本");values[c]=cell==null?"":format.formatCellValue(cell).trim();}
                add(result,i+1,values);
            }
        } catch(ServiceException ex) {throw ex;} catch(Exception ex) {throw new ServiceException("Excel读取失败");}
        return result;
    }
    static List<SourceRow> csv(byte[] bytes) {
        String value=new String(bytes,StandardCharsets.UTF_8);if(value.startsWith("\uFEFF"))value=value.substring(1);
        List<List<String>> lines=new ArrayList<>();List<String> fields=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
        for(int i=0;i<value.length();i++) {char ch=value.charAt(i);if(ch=='"') {if(quoted && i+1<value.length() && value.charAt(i+1)=='"'){field.append('"');i++;}else quoted=!quoted;}else if(ch==',' && !quoted){fields.add(field.toString());field.setLength(0);}else if((ch=='\n'||ch=='\r')&&!quoted){if(ch=='\r'&&i+1<value.length()&&value.charAt(i+1)=='\n')i++;fields.add(field.toString());field.setLength(0);lines.add(fields);fields=new ArrayList<>();}else field.append(ch);}
        if(quoted)throw new ServiceException("CSV引号未闭合");if(!fields.isEmpty()||field.length()>0){fields.add(field.toString());lines.add(fields);}
        if(lines.isEmpty())throw new ServiceException("缺少表头");List<String> header=lines.get(0);int[] cols=columns(i -> header.get(i).trim(),header.size());
        List<SourceRow> result=new ArrayList<>();for(int i=1;i<lines.size();i++){List<String> line=lines.get(i);String[] cells=new String[4];for(int c=0;c<4;c++)cells[c]=cols[c]<line.size()?line.get(cols[c]).trim():"";add(result,i+1,cells);}return result;
    }
    private static int[] columns(java.util.function.IntFunction<String> header,int count){String[] names={"时间","客服昵称","客户昵称","订单号"};int[] result=new int[4];for(int n=0;n<4;n++){result[n]=-1;for(int i=0;i<count;i++)if(names[n].equals(header.apply(i)))result[n]=i;if(result[n]<0)throw new ServiceException("缺少表头："+names[n]);}return result;}
    private static void add(List<SourceRow> rows,int line,String[] v){if(Arrays.stream(v).allMatch(String::isBlank))return;try{LocalDateTime time=LocalDateTime.parse(v[0],DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));String agent=text(v[1],100),nickname=text(v[2],160),raw=text(v[3],1000);if(agent.isEmpty()||nickname.isEmpty())throw new ServiceException("第"+line+"行客服或客户昵称为空");rows.add(new SourceRow(line,time,agent,nickname,raw));}catch(java.time.format.DateTimeParseException ex){throw new ServiceException("第"+line+"行时间格式应为 yyyy-MM-dd HH:mm:ss");}}
    private static String text(String value,int max){String result=Objects.toString(value,"").trim();if(result.length()>max)throw new ServiceException("内容超过"+max+"字符");return result;}
    private static int page(String value){try{return Math.max(1,Math.min(100000,Integer.parseInt(value)));}catch(Exception ex){return 1;}}
    private static int size(String value){try{return Math.max(1,Math.min(100,Integer.parseInt(value)));}catch(Exception ex){return 20;}}
    private static String hash(byte[] bytes){try{byte[] result=MessageDigest.getInstance("SHA-256").digest(bytes);return HexFormat.of().formatHex(result);}catch(Exception ex){throw new IllegalStateException(ex);}}
    record SourceRow(int line,LocalDateTime time,String agent,String nickname,String rawOrders) {}
}
