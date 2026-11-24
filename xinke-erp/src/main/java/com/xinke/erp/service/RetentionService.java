package com.xinke.erp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.xinke.common.config.XinKeConfig;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.mapper.RetentionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RetentionService {
 private static final long MAX_IMAGE_SIZE=10L*1024*1024;
 private static final Pattern SAFE_NAME=Pattern.compile("[^\\p{L}\\p{N}._-]");
 private final RetentionMapper mapper;
 public RetentionService(RetentionMapper mapper){this.mapper=mapper;}
 public Map<String,Object> options(){
  List<Long> adminShops=SecurityUtils.isAdmin()?mapper.shops().stream().map(s->longId(s.get("shopId"))).toList():mapper.adminShops(SecurityUtils.getUserId());
  return Map.of("shops",mapper.shops(),"staff",mapper.staff(),"ownOnly",adminShops.isEmpty(),"adminShopIds",adminShops,"canCreate",!adminShops.isEmpty());
 }
 public Map<String,Object> list(Map<String,Object> input){Map<String,Object> q=query(input,false);return Map.of("rows",mapper.list(q),"total",mapper.count(q));}
 public List<Map<String,Object>> export(Map<String,Object> input){Map<String,Object> q=query(input,true);if(mapper.count(q)>5000)throw new ServiceException("最多下载5000条，请缩短时间范围");return mapper.list(q);}
 public Map<String,Object> dashboard(Map<String,Object> input){Map<String,Object> q=query(input,false);q.remove("progress");return mapper.dashboard(q);}
 public Map<String,Object> detail(Long id){Map<String,Object> row=required(id);row.put("history",mapper.history(id));row.put("attachments",withUrls(mapper.attachments(id)));row.put("canEditOrder",canManage(longId(row.get("shopId"))));row.put("canEditAddress",true);return row;}
 private Map<String,Object> required(Long id){Map<String,Object> row=mapper.get(id);if(row==null || (!canManage(longId(row.get("shopId")))&&!Objects.equals(SecurityUtils.getUserId(),longId(row.get("ownerId")))))throw new ServiceException("订单不存在或无权访问");return new HashMap<>(row);}
 private boolean canManage(Long shop){return SecurityUtils.isAdmin() || (shop!=null&&mapper.isShopAdmin(shop,SecurityUtils.getUserId())>0);}
 private void requireManager(Long shop){if(!canManage(shop))throw new ServiceException("仅该店铺客服管理员可以新增或修改订单信息");}

 @Transactional
 public void delete(Long id,Long version){
  Map<String,Object> row=required(id);
  if(!canManage(longId(row.get("shopId"))))throw new ServiceException("仅该店铺客服管理员可以删除订单");
  List<Map<String,Object>> files=Optional.ofNullable(mapper.attachments(id)).orElseGet(List::of);
  if(version==null||version<0||mapper.deleteOrder(id,version)!=1)throw new ServiceException("订单已被修改或删除，请刷新后重试");
  mapper.deleteAttachments(id);
  mapper.deleteHistory(id);
  files.forEach(this::deleteFileQuietly);
 }

 public Map<String,Object> uploadAttachment(Long orderId,MultipartFile file,Long userId){
  required(orderId);
  if(file==null||file.isEmpty())throw new ServiceException("请选择需要上传的图片");
  if(Optional.ofNullable(mapper.attachments(orderId)).orElseGet(List::of).size()>=9)throw new ServiceException("每个订单最多上传9张凭证");
  String type=Objects.toString(file.getContentType(),"").toLowerCase(Locale.ROOT);
  if(!Set.of("image/jpeg","image/png","image/webp").contains(type))throw new ServiceException("只支持 JPG、PNG 或 WEBP 图片");
  if(file.getSize()<=0||file.getSize()>MAX_IMAGE_SIZE)throw new ServiceException("单张图片不能超过10MB");
  String original=safeName(file.getOriginalFilename()),relative="retention/"+LocalDate.now()+"/"+orderId+"/"+UUID.randomUUID().toString().replace("-","")+"-"+original;
  Path profile=Path.of(XinKeConfig.getProfile()).toAbsolutePath().normalize(),target=profile.resolve(relative).normalize();
  if(!target.startsWith(profile))throw new ServiceException("图片路径不正确");
  try(InputStream input=file.getInputStream()){
   Files.createDirectories(target.getParent());Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);
   Map<String,Object> value=new HashMap<>();value.put("orderId",orderId);value.put("objectKey",relative.replace('\\','/'));value.put("originalName",original);value.put("contentType",type);value.put("fileSize",file.getSize());value.put("uploadedBy",userId);mapper.insertAttachment(value);
   return withUrl(mapper.attachment(longId(value.get("id"))));
  }catch(Exception ex){try{Files.deleteIfExists(target);}catch(Exception ignored){}throw new ServiceException("图片保存失败，请重试");}
 }

 public void deleteAttachment(Long orderId,Long attachmentId){
  Map<String,Object> order=required(orderId),file=mapper.attachment(attachmentId);
  if(file==null||!Objects.equals(orderId,longId(file.get("orderId"))))throw new ServiceException("凭证不存在");
  if(!canManage(longId(order.get("shopId")))&&!Objects.equals(SecurityUtils.getUserId(),longId(file.get("uploadedBy"))))throw new ServiceException("只能删除自己上传的凭证");
  if(mapper.deleteAttachment(attachmentId)!=1)throw new ServiceException("凭证已被删除");deleteFileQuietly(file);
 }

 @Transactional
 public Long create(Map<String,Object> input,Long userId,String userName){
  Map<String,Object> v=fields(input);requireManager(longId(v.get("shopId")));base(v);v.put("userId",userId);
  try{mapper.insert(v);}catch(DuplicateKeyException ex){throw new ServiceException("该店铺的订单号已存在，请搜索已有任务");}
  Long id=longId(v.get("id"));audit(id,"CREATE","创建订单",null,v,userId,userName);return id;
 }
 public Map<String,Object> batchCheck(Map<String,Object> input){
  Long shop=longId(input.get("shopId"));if(shop==null||mapper.validShop(shop)==0)throw new ServiceException("请选择有效店铺");requireManager(shop);
  Object raw=input.get("orderNos");if(!(raw instanceof Collection<?> values))throw new ServiceException("请按每行一个订单号填写");
  if(values.size()>200)throw new ServiceException("单次最多检查200条订单");
  LinkedHashMap<String,String> uniqueByKey=new LinkedHashMap<>();LinkedHashSet<String> duplicateSet=new LinkedHashSet<>(),invalidSet=new LinkedHashSet<>();
  int blankCount=0;
  for(Object value:values){String orderNo=Objects.toString(value,"").trim();if(orderNo.isEmpty()){blankCount++;continue;}if(orderNo.length()>64){invalidSet.add(orderNo);continue;}String key=orderNo.toLowerCase(Locale.ROOT);if(uniqueByKey.containsKey(key)){duplicateSet.add(orderNo);continue;}uniqueByKey.put(key,orderNo);}
  List<String> unique=new ArrayList<>(uniqueByKey.values());if(unique.isEmpty()&&invalidSet.isEmpty())throw new ServiceException("请至少填写一个订单号");
  List<String> stored=unique.isEmpty()?List.of():mapper.existingOrderNos(shop,unique);
  List<String> existing=unique.stream().filter(orderNo->stored.stream().anyMatch(value->value.equalsIgnoreCase(orderNo))).toList();
  Set<String> existingKeys=existing.stream().map(value->value.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());List<String> valid=unique.stream().filter(orderNo->!existingKeys.contains(orderNo.toLowerCase(Locale.ROOT))).toList();
  Map<String,Object> result=new LinkedHashMap<>();result.put("validOrderNos",valid);result.put("existingOrderNos",existing);result.put("duplicateInputOrderNos",new ArrayList<>(duplicateSet));result.put("invalidOrderNos",new ArrayList<>(invalidSet));result.put("blankCount",blankCount);result.put("validCount",valid.size());result.put("existingCount",existing.size());result.put("duplicateInputCount",duplicateSet.size());result.put("invalidCount",invalidSet.size());return result;
 }
 @Transactional
 public Map<String,Object> batchCreate(Map<String,Object> input,Long userId,String userName){
  Long shop=longId(input.get("shopId"));if(shop==null||mapper.validShop(shop)==0)throw new ServiceException("请选择有效店铺");requireManager(shop);
  Object raw=input.get("orderNos");if(!(raw instanceof Collection<?> values))throw new ServiceException("请按每行一个订单号填写");
  if(values.size()>200)throw new ServiceException("单次最多创建200条订单");
  LinkedHashSet<String> orderNos=new LinkedHashSet<>();int blank=0,duplicateInput=0;
  for(Object value:values){String orderNo=Objects.toString(value,"").trim();if(orderNo.isEmpty()){blank++;continue;}if(orderNo.length()>64)throw new ServiceException("订单号不能超过64个字符："+orderNo.substring(0,Math.min(20,orderNo.length()))+"…");if(!orderNos.add(orderNo))duplicateInput++;}
  if(orderNos.isEmpty())throw new ServiceException("请至少填写一个订单号");
  String address=Objects.toString(input.get("address"),"").trim();if(address.length()>500)throw new ServiceException("电话地址不能超过500个字符");
  Long owner=longId(input.get("ownerId"));if(owner!=null&&mapper.validAssignee(shop,owner)==0)throw new ServiceException("请选择该店铺已配置的有效电话客服");
  List<String> skipped=new ArrayList<>();int created=0;
  for(String orderNo:orderNos){Map<String,Object> row=new HashMap<>();row.put("shopId",shop);row.put("orderNo",orderNo);row.put("address",address);row.put("ownerId",owner);row.put("userId",userId);
   try{mapper.insert(row);}catch(DuplicateKeyException ex){skipped.add(orderNo);continue;}
   Long id=longId(row.get("id"));audit(id,"CREATE","批量创建订单",null,row,userId,userName);created++;
  }
  return Map.of("createdCount",created,"skippedCount",skipped.size(),"duplicateInputCount",duplicateInput,"blankCount",blank,"skippedOrderNos",skipped);
 }

 @Transactional
 public Map<String,Object> batchAssign(Map<String,Object> input,Long userId,String userName){
  Long owner=longId(input.get("ownerId"));if(owner==null)throw new ServiceException("请选择电话客服");
  Object raw=input.get("orders");if(!(raw instanceof Collection<?> values)||values.isEmpty())throw new ServiceException("请先选择需要分配的订单");
  if(values.size()>200)throw new ServiceException("单次最多分配200条订单");
  List<Map<String,Object>> orders=new ArrayList<>();Set<Long> ids=new HashSet<>();
  for(Object value:values){if(!(value instanceof Map<?,?> item))throw new ServiceException("订单数据格式不正确");Long id=longId(item.get("id")),version=longId(item.get("version"));if(id==null||version==null||!ids.add(id))continue;Map<String,Object> row=required(id);Long shop=longId(row.get("shopId"));requireManager(shop);if("COMPLETED".equals(row.get("progress")))throw new ServiceException("已完成订单不能批量改派，请单独处理");if(mapper.validAssignee(shop,owner)==0)throw new ServiceException("所选电话客服不属于全部已选店铺，请按店铺分别分配");if(!Objects.equals(version,longId(row.get("version"))))throw new ServiceException("订单已被修改，请刷新后重试");orders.add(row);}
  String ownerName=Objects.toString(mapper.userName(owner),"");if(ownerName.isBlank())throw new ServiceException("电话客服账号不存在或已停用");int assigned=0,skipped=0;
  for(Map<String,Object> row:orders){Long oldOwner=longId(row.get("ownerId"));if(Objects.equals(oldOwner,owner)){skipped++;continue;}Map<String,Object> update=new HashMap<>();update.put("id",row.get("id"));update.put("version",row.get("version"));update.put("ownerId",owner);if(mapper.updateOwner(update)!=1)throw new ServiceException("订单已被修改，请刷新后重试");audit(longId(row.get("id")),"OWNER_CHANGE","批量分配电话客服",ownerSnapshot(oldOwner),Map.of("ownerId",owner,"ownerName",ownerName),userId,userName);assigned++;}
  return Map.of("assignedCount",assigned,"skippedCount",skipped);
 }
 @Transactional
 public void update(Long id,Map<String,Object> input,Long userId,String userName){
  Map<String,Object> old=required(id),v=fields(input);
  for(String key:List.of("shopId","orderNo","address","ownerId"))v.putIfAbsent(key,old.get(key));
  if(!canManage(longId(old.get("shopId")))){
   for(String key:List.of("shopId","orderNo","ownerId")){
    if(input.containsKey(key)&&!Objects.equals(Objects.toString(old.get(key),""),Objects.toString(v.get(key),"")))throw new ServiceException("仅客服管理员可以修改订单信息");
    v.put(key,old.get(key));
   }
  }else requireManager(longId(v.get("shopId")));
  v.put("id",id);base(v);
  if(!Objects.equals(longId(old.get("version")),longId(v.get("version"))))throw new ServiceException("此订单已被其他人修改，请重新打开");
  String progress=text(v,"progress",16,true),result=text(v,"retentionResult",16,true);
  if(!Set.of("PENDING","FOLLOWING","COMPLETED").contains(progress)||!Set.of("UNKNOWN","SUCCESS","FAILED").contains(result))throw new ServiceException("处理状态不正确");
  boolean followup=Boolean.TRUE.equals(v.get("isFollowup"));String note=text(v,"note",2000,followup);if(note.isBlank())note=!Objects.equals(Objects.toString(old.get("address"),""),Objects.toString(v.get("address"),""))?"修改电话地址":"修改订单";v.put("note",note);
  if(followup&&v.get("ownerId")==null)throw new ServiceException("请先分配负责人再跟进");
  if(progress.equals("FOLLOWING")&&v.get("ownerId")==null)throw new ServiceException("跟进中的任务必须有负责人");
  if(followup&&progress.equals("PENDING"))v.put("progress","FOLLOWING");
  boolean startsFollowup=followup||!"PENDING".equals(progress);
  v.put("isFollowup",followup?1:0);v.put("startsFollowup",startsFollowup?1:0);
  v.put("retentionAmount",result.equals("SUCCESS")?amount(v.get("retentionAmount"),"挽单销售额"):BigDecimal.ZERO);
  boolean referral=flag(v.get("referralSuccess")),freight=!result.equals("SUCCESS")&&v.get("freightAmount")!=null&&!v.get("freightAmount").toString().isBlank();
  v.put("referralSuccess",referral?1:0);v.put("freightCollected",freight?1:0);
  if(referral){Long shop=longId(v.get("referralShopId"));if(shop==null||mapper.validShop(shop)==0)throw new ServiceException("请选择有效的转推店铺");v.put("referralShopId",shop);text(v,"referralOrderNo",64,true);v.put("referralAmount",amount(v.get("referralAmount"),"转推金额"));}
  else{v.put("referralShopId",null);v.put("referralOrderNo","");v.put("referralAmount",BigDecimal.ZERO);}
  v.put("freightAmount",freight?amount(v.get("freightAmount"),"征收运费金额"):BigDecimal.ZERO);
  if(progress.equals("COMPLETED")&&result.equals("UNKNOWN")&&!referral&&!freight)throw new ServiceException("完成前请选择至少一项业务结果");
  try{if(mapper.update(v)==0)throw new ServiceException("订单已被修改，请重新打开");}catch(DuplicateKeyException ex){throw new ServiceException("该店铺的订单号已存在");}
  audit(id,followup?"FOLLOWUP":"EDIT",note,old,v,userId,userName);
  if(!Objects.equals(longId(old.get("ownerId")),longId(v.get("ownerId"))))audit(id,"OWNER_CHANGE",note,ownerSnapshot(longId(old.get("ownerId"))),ownerSnapshot(longId(v.get("ownerId"))),userId,userName);
 }
 private Map<String,Object> ownerSnapshot(Long id){return Map.of("ownerId",id==null?"":id,"ownerName",id==null?"未分配":Objects.toString(mapper.userName(id),"未分配"));}
 private static Map<String,Object> fields(Map<String,Object> input){Map<String,Object> v=new HashMap<>();for(String k:List.of("shopId","orderNo","address","ownerId","version","progress","retentionResult","note","isFollowup","retentionAmount","referralSuccess","referralShopId","referralOrderNo","referralAmount","freightAmount"))if(input.containsKey(k))v.put(k,input.get(k));return v;}
 private void base(Map<String,Object> v){Long shop=longId(v.get("shopId"));if(shop==null||mapper.validShop(shop)==0)throw new ServiceException("请选择有效店铺");v.put("shopId",shop);text(v,"orderNo",64,true);text(v,"address",500,false);Long owner=longId(v.get("ownerId"));if(owner!=null&&mapper.validAssignee(shop,owner)==0)throw new ServiceException("请选择该店铺已配置的有效电话客服");v.put("ownerId",owner);}
 private void audit(Long id,String action,String note,Object old,Object next,Long userId,String name){Map<String,Object> v=new HashMap<>();v.put("id",id);v.put("action",action);v.put("note",note);v.put("oldValue",old==null?null:JSON.toJSONString(old));v.put("newValue",JSON.toJSONString(next));v.put("userId",userId);v.put("userName",name);mapper.historyInsert(v);}
 private Map<String,Object> query(Map<String,Object> input,boolean export){
  Map<String,Object> q=new HashMap<>();LocalDate end,start;
  try{end=LocalDate.parse(Objects.toString(input.getOrDefault("endDate",LocalDate.now().toString())));start=LocalDate.parse(Objects.toString(input.getOrDefault("beginDate",end.minusDays(1).toString())));}catch(Exception ex){throw new ServiceException("请选择正确的日期范围");}
  if(start.isAfter(end)||ChronoUnit.DAYS.between(start,end)>365||(export&&ChronoUnit.DAYS.between(start,end)>30))throw new ServiceException(export?"单次下载最多31天":"查询范围最多一年");
  q.put("beginDate",start.toString());q.put("endExclusive",end.plusDays(1).toString());
  for(String key:List.of("shopId","ownerId")){Long value=longId(input.get(key));if(value!=null)q.put(key,value);}
  String progress=Objects.toString(input.get("progress"),"");if(!progress.isBlank()){if(!Set.of("PENDING","FOLLOWING","COMPLETED").contains(progress))throw new ServiceException("状态不正确");q.put("progress",progress);}
  String resultType=Objects.toString(input.get("resultType"),"");if(!resultType.isBlank()){if(!Set.of("UNKNOWN","SUCCESS","FAILED","REFERRAL_SUCCESS","FREIGHT_COLLECTED").contains(resultType))throw new ServiceException("结果筛选不正确");q.put("resultType",resultType);}
  String addressStatus=Objects.toString(input.get("addressStatus"),"");if(!addressStatus.isBlank()){if(!Set.of("MISSING","FILLED").contains(addressStatus))throw new ServiceException("电话地址筛选不正确");q.put("addressStatus",addressStatus);}
  String keyword=Objects.toString(input.get("keyword"),"").trim();if(keyword.length()>64)throw new ServiceException("搜索内容过长");if(!keyword.isBlank())q.put("keyword",keyword.replace("%","").replace("_",""));
  int size=export?5000:Math.max(1,Math.min(100,integer(input.get("pageSize"),20))),page=Math.max(1,Math.min(100000,integer(input.get("pageNum"),1)));
  q.put("limit",size);q.put("offset",export?0:(page-1)*size);
  if(!SecurityUtils.isAdmin())q.put("scopeUserId",SecurityUtils.getUserId());
  return q;
 }
 private static String text(Map<String,Object> v,String key,int max,boolean required){String s=Objects.toString(v.get(key),"").trim();if((required&&s.isEmpty())||s.length()>max)throw new ServiceException("请完整填写订单、联系方式和跟进信息，且不要超过长度限制");v.put(key,s);return s;}
 private static BigDecimal amount(Object value,String label){try{if(value==null||value.toString().isBlank())throw new Exception();BigDecimal n=new BigDecimal(value.toString());if(n.signum()<0||n.scale()>2||n.compareTo(new BigDecimal("999999999999.99"))>0)throw new Exception();return n;}catch(Exception ex){throw new ServiceException(label+"必须是非负金额，最多两位小数");}}
 private static boolean flag(Object value){return Boolean.TRUE.equals(value)||"1".equals(Objects.toString(value));}
 private static int integer(Object v,int fallback){try{return Integer.parseInt(Objects.toString(v));}catch(Exception ex){return fallback;}}
 private static Long longId(Object v){if(v==null||v.toString().isBlank())return null;try{return Long.valueOf(v.toString());}catch(Exception ex){throw new ServiceException("编号格式不正确");}}
 private List<Map<String,Object>> withUrls(List<Map<String,Object>> files){return Optional.ofNullable(files).orElseGet(List::of).stream().map(this::withUrl).toList();}
 private Map<String,Object> withUrl(Map<String,Object> source){Map<String,Object> file=new HashMap<>(source);file.put("url","/profile/"+Objects.toString(file.get("objectKey"),"").replace('\\','/'));return file;}
 private void deleteFileQuietly(Map<String,Object> file){try{Path profile=Path.of(XinKeConfig.getProfile()).toAbsolutePath().normalize(),target=profile.resolve(Objects.toString(file.get("objectKey"),"")).normalize();if(target.startsWith(profile))Files.deleteIfExists(target);}catch(Exception ignored){}}
 private static String safeName(String value){String name=Objects.toString(value,"image").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);name=SAFE_NAME.matcher(name).replaceAll("_");if(name.isBlank())name="image";return name.length()>180?name.substring(name.length()-180):name;}
}
