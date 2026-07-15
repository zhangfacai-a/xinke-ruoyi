package com.xinke.erp.controller;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.service.IErpEtlService;

@RestController
@RequestMapping("/erp/etl")
public class ErpEtlController extends BaseController
{
    @Autowired
    private IErpEtlService etlService;

    @PreAuthorize("@ss.hasPermi('erp:etl:run')")
    @PostMapping("/order")
    public AjaxResult buildOrder(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dt)
    {
        int rows = etlService.buildOrderWideTable(dt);
        return success("ETL completed, dwd rows: " + rows);
    }
}
