package com.xinke.erp.controller;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.service.FreightCollectionService;
import com.xinke.erp.service.FreightAccessService;

@RestController
@RequestMapping("/live/freight")
@PreAuthorize("@freightAccess.allowed('view')")
public class FreightCollectionController {
    private final FreightCollectionService service;
    private final FreightAccessService access;

    public FreightCollectionController(FreightCollectionService service,FreightAccessService access) { this.service=service;this.access=access; }

    @GetMapping("/capabilities")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult capabilities() { return AjaxResult.success(access.capabilities()); }

    @GetMapping("/shops")
    public AjaxResult shops() { return AjaxResult.success(service.shops()); }

    @GetMapping
    public AjaxResult list(@RequestParam Map<String,Object> query) { return AjaxResult.success(service.list(query)); }

    @GetMapping("/dashboard")
    @PreAuthorize("@freightAccess.allowed('dashboard')")
    public AjaxResult dashboard(@RequestParam Map<String,Object> query) { return AjaxResult.success(service.dashboard(query)); }

    @PostMapping
    @PreAuthorize("@freightAccess.allowed('create')")
    public AjaxResult create(@RequestBody Map<String,Object> value) {
        return AjaxResult.success(Map.of("id",service.create(value)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@freightAccess.allowed('edit')")
    public AjaxResult update(@PathVariable Long id,@RequestBody Map<String,Object> value) {
        service.update(id,value);
        return AjaxResult.success();
    }

    @PostMapping("/import")
    @PreAuthorize("@freightAccess.allowed('import')")
    public AjaxResult importFile(@RequestParam("file") MultipartFile file) {
        return AjaxResult.success(Map.of("imported",service.importFile(file)));
    }

    @GetMapping("/export")
    @PreAuthorize("@freightAccess.allowed('export')")
    public void export(@RequestParam Map<String,Object> query,HttpServletResponse response) throws Exception {
        List<Map<String,Object>> rows=service.export(query);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=freight-collection.csv");
        PrintWriter out=response.getWriter();
        out.write('\uFEFF');
        out.println("录入时间,店铺,订单状态,订单号,客服姓名,运费金额,划扣方式,征收时间,备注");
        String[] keys={"createdAt","shopName","deliveryStatus","orderNo","createdByName","freightAmount","deductionMethod","collectedAt","remark"};
        for(Map<String,Object> row:rows) {
            for(int index=0;index<keys.length;index++) {
                if(index>0) out.print(',');
                String key=keys[index],value=Objects.toString(row.get(key),"");
                if(key.equals("deliveryStatus")) value=value.equals("SIGNED")?"已签收":"已发货未签收";
                if(key.equals("deductionMethod")) value=value.equals("ALIPAY_PERSONAL")?"支付宝个人账户9%":"订单打款7%";
                out.print(csv(value,key.equals("orderNo")));
            }
            out.println();
        }
        out.flush();
    }

    static String csv(String value,boolean orderNo) {
        String leading=value.stripLeading();
        if(!value.isEmpty() && (orderNo || (!leading.isEmpty() && "=+@-".indexOf(leading.charAt(0))>=0)))
            value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
}
