package com.xinke.erp.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.service.JdCustomerService;

@RestController
@RequestMapping("/live/jd-customers")
public class JdCustomerController {
    private final JdCustomerService service;
    public JdCustomerController(JdCustomerService service) { this.service = service; }

    @GetMapping("/records")
    public AjaxResult records(@RequestParam Map<String,String> filters) { return AjaxResult.success(service.records(filters)); }
    @GetMapping("/customers")
    public AjaxResult customers(@RequestParam Map<String,String> filters) { return AjaxResult.success(service.customers(filters)); }
    @GetMapping("/records/{id}/orders")
    public AjaxResult recordOrders(@PathVariable long id) { return AjaxResult.success(service.recordOrders(id)); }
    @GetMapping("/customers/{id}")
    public AjaxResult customer(@PathVariable long id) { return AjaxResult.success(service.customer(id)); }
    @PutMapping("/customers/{id}/contact")
    public AjaxResult contact(@PathVariable long id, @RequestBody Map<String,String> input) {
        service.updateContact(id,input); return AjaxResult.success();
    }
    @PostMapping("/orders/link")
    public AjaxResult linkOrder(@RequestBody Map<String,String> input) {
        service.linkOrder(input); return AjaxResult.success();
    }
    @PostMapping("/customers/merge")
    public AjaxResult merge(@RequestBody Map<String,String> input) {
        service.mergeCustomers(input); return AjaxResult.success();
    }
    @PostMapping("/import")
    public AjaxResult importFile(@RequestParam("file") MultipartFile file) { return AjaxResult.success(service.importFile(file)); }
    @GetMapping("/batches")
    public AjaxResult batches() { return AjaxResult.success(service.batches()); }
}
