package com.xinke.erp.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.service.AnchorAppReleaseService;

@RestController
@RequestMapping("/system/anchor-app-releases")
@PreAuthorize("@ss.hasPermi('system:appRelease:manage')")
public class AnchorAppReleaseController {
    private final AnchorAppReleaseService service;
    public AnchorAppReleaseController(AnchorAppReleaseService service){this.service=service;}
    @GetMapping public AjaxResult list()throws Exception{return AjaxResult.success(service.list());}
    @PostMapping("/upload") public AjaxResult upload(@RequestParam MultipartFile installer,@RequestParam MultipartFile manifest)throws Exception{service.rememberInitial();service.upload(installer,manifest);return AjaxResult.success();}
    @PostMapping("/uploads") public AjaxResult begin(@RequestParam MultipartFile manifest,@RequestParam long total)throws Exception{service.rememberInitial();return AjaxResult.success("上传任务已建立",service.beginUpload(manifest,total));}
    @PostMapping("/uploads/{id}/chunks") public AjaxResult chunk(@PathVariable String id,@RequestParam int index,@RequestParam MultipartFile chunk)throws Exception{service.chunk(id,index,chunk);return AjaxResult.success();}
    @PostMapping("/uploads/{id}/finish") public AjaxResult finish(@PathVariable String id)throws Exception{service.finishUpload(id);return AjaxResult.success();}
    @PostMapping("/{version}/publish") public AjaxResult publish(@PathVariable String version)throws Exception{service.rememberInitial();service.publish(version);return AjaxResult.success();}
    @PostMapping("/{version}/withdraw") public AjaxResult withdraw(@PathVariable String version)throws Exception{service.withdraw(version);return AjaxResult.success();}
}
