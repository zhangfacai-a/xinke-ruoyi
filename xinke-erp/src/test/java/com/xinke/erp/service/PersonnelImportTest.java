package com.xinke.erp.service;

import java.io.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.utils.poi.ExcelUtil;
import static org.junit.jupiter.api.Assertions.*;

class PersonnelImportTest {
    @Test void acceptsOldAndNewPersonnelColumnNames() throws Exception {
        for(String[] columns:new String[][]{{"登录账号","姓名"},{"登录名称","用户名称"}}) {
            byte[] bytes;
            try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
                var sheet=book.createSheet();var header=sheet.createRow(0);
                header.createCell(0).setCellValue(columns[0]);header.createCell(1).setCellValue(columns[1]);
                var row=sheet.createRow(1);row.createCell(0).setCellValue("test-account");row.createCell(1).setCellValue("测试人员");
                book.write(out);bytes=out.toByteArray();
            }
            var users=new ExcelUtil<>(SysUser.class).importExcel(new ByteArrayInputStream(bytes));
            assertEquals(1,users.size());assertEquals("test-account",users.get(0).getUserName());assertEquals("测试人员",users.get(0).getNickName());
        }
    }
}
