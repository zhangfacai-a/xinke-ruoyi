package com.xinke.erp.controller;

import java.util.Map;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.erp.service.RetentionService;

@RestController
@RequestMapping("/live/retention")
@PreAuthorize("@ss.hasPermi('live:retention:access')")
public class RetentionController {
 private final RetentionService service;
 private static final Semaphore EXPORT_LIMIT=new Semaphore(1);
 public RetentionController(RetentionService service){this.service=service;}
 @GetMapping("/options") public AjaxResult options(){return AjaxResult.success(service.options());}
 @GetMapping public AjaxResult list(@RequestParam Map<String,Object> q){return AjaxResult.success(service.list(q));}
 @GetMapping("/dashboard") public AjaxResult dashboard(@RequestParam Map<String,Object> q){return AjaxResult.success(service.dashboard(q));}
 @GetMapping("/{id}") public AjaxResult detail(@PathVariable Long id){return AjaxResult.success(service.detail(id));}
 @PostMapping public AjaxResult create(@RequestBody Map<String,Object> v){return AjaxResult.success(Map.of("id",service.create(v,SecurityUtils.getUserId(),name())));}
 @PostMapping("/batch/check") public AjaxResult batchCheck(@RequestBody Map<String,Object> v){return AjaxResult.success(service.batchCheck(v));}
 @PostMapping("/batch") public AjaxResult batchCreate(@RequestBody Map<String,Object> v){return AjaxResult.success(service.batchCreate(v,SecurityUtils.getUserId(),name()));}
 @PutMapping("/batch-owner") public AjaxResult batchOwner(@RequestBody Map<String,Object> v){return AjaxResult.success(service.batchAssign(v,SecurityUtils.getUserId(),name()));}
 @PutMapping("/{id}") public AjaxResult update(@PathVariable Long id,@RequestBody Map<String,Object> v){service.update(id,v,SecurityUtils.getUserId(),name());return AjaxResult.success();}
 @DeleteMapping("/{id}") public AjaxResult delete(@PathVariable Long id,@RequestParam Long version){service.delete(id,version);return AjaxResult.success();}
 @PostMapping("/{id}/attachments") public AjaxResult uploadAttachment(@PathVariable Long id,@RequestPart("file") MultipartFile file){return AjaxResult.success(service.uploadAttachment(id,file,SecurityUtils.getUserId()));}
 @DeleteMapping("/{id}/attachments/{attachmentId}") public AjaxResult deleteAttachment(@PathVariable Long id,@PathVariable Long attachmentId){service.deleteAttachment(id,attachmentId);return AjaxResult.success();}
 private String name(){String nickname=SecurityUtils.getLoginUser().getUser().getNickName();return nickname==null||nickname.isBlank()?SecurityUtils.getUsername():nickname;}
 @GetMapping("/export") public void export(@RequestParam Map<String,Object> q,HttpServletResponse response)throws Exception{
  if(!EXPORT_LIMIT.tryAcquire())throw new com.xinke.common.exception.ServiceException("有其他下载正在处理，请稍后重试");
  try{
   List<Map<String,Object>> rows=service.export(q);
   response.setContentType("text/csv;charset=UTF-8");response.setHeader("Content-Disposition","attachment; filename=retention-orders.csv");
   PrintWriter out=response.getWriter();out.write('\uFEFF');
   out.println("创建时间,店铺,原订单号,电话地址,负责人,处理进度,挽单结果,挽单销售额,转推成功,转推店铺,转推订单号,转推金额,征收运费,运费金额,首次跟进时间,最新跟进");
   String[] keys={"createdAt","shopName","orderNo","address","ownerName","progress","retentionResult","retentionAmount","referralSuccess","referralShopName","referralOrderNo","referralAmount","freightCollected","freightAmount","firstFollowedAt","latestNote"};
   for (Map<String,Object> row : rows) {
    for (int i=0; i<keys.length; i++) {
     if (i>0) out.print(',');
     String key=keys[i], value=java.util.Objects.toString(row.get(key), "");
     if (key.equals("progress") || key.equals("retentionResult")) {
      value=switch(value) {
       case "PENDING" -> "待处理";
       case "FOLLOWING" -> "跟进中";
       case "COMPLETED" -> "已完成";
       case "UNKNOWN" -> "未确定";
       case "SUCCESS" -> "挽单成功";
       case "FAILED" -> "挽单失败";
       default -> value;
      };
     }
     if (key.equals("referralSuccess") || key.equals("freightCollected")) {
      value=(value.equals("1") || value.equalsIgnoreCase("true")) ? "是" : "否";
     }
     out.print(csvCell(value,key.equals("orderNo") || key.equals("referralOrderNo")));
    }
    out.println();
   }
   out.flush();
  }finally{EXPORT_LIMIT.release();}
 }
 static String csvCell(String value,boolean preserveDigits) {
  String leading=value.stripLeading();
  if (!value.isEmpty() && (preserveDigits || (!leading.isEmpty() && "=+@-".indexOf(leading.charAt(0))>=0))) {
   value="'"+value;
  }
  return "\""+value.replace("\"","\"\"")+"\"";
 }
}
