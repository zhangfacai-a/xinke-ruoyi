package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginUser;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.FreightCollectionMapper;

class FreightCollectionServiceTest {
    @Test void globalOperatorCreatesAndEditsOtherUsersRecordsWithoutShopAssignment() {
        SysUser user=new SysUser();user.setUserId(7L);user.setUserName("operator");
        LoginUser login=new LoginUser(7L,1L,user,Set.of("live:freight:viewAll","live:freight:access"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        when(mapper.shop(10L,7L,true)).thenReturn(Map.of("shopId",10L,"shopName","测试店铺"));
        when(mapper.insert(anyMap())).thenAnswer(call -> { Map<String,Object> value=call.getArgument(0);value.put("id",42L);return 1; });
        assertEquals(42L,service.create(input()));
        when(mapper.get(42L)).thenReturn(Map.of("shopId",10L,"createdByUserId",99L));
        when(mapper.update(anyMap())).thenReturn(1);
        Map<String,Object> value=input();value.put("version",1L);
        service.update(42L,value);
        verify(mapper).update(anyMap());
        verify(mapper,org.mockito.Mockito.times(2)).shop(10L,7L,true);
    }

    @Test void globalViewerReadsAllShopsAndDashboardWithoutChangingWriteScope() {
        SysUser user=new SysUser();user.setUserId(7L);user.setUserName("viewer");
        LoginUser login=new LoginUser(7L,1L,user,Set.of("live:freight:viewAll"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        when(mapper.summary(anyMap())).thenReturn(Map.of("total",0));
        when(mapper.daily(anyMap())).thenReturn(List.of());
        service.shops();verify(mapper).shops(7L,true);
        service.dashboard(Map.of());
        var query=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).daily(query.capture());
        org.junit.jupiter.api.Assertions.assertFalse(query.getValue().containsKey("scopeUserId"));
        when(mapper.shop(10L,7L,false)).thenReturn(null);
        assertThrows(ServiceException.class,()->service.create(input()));
        verify(mapper).shop(10L,7L,false);
    }
    private final FreightCollectionMapper mapper=mock(FreightCollectionMapper.class);
    private final FreightCollectionService service=new FreightCollectionService(mapper);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private void login() {
        SysUser user=new SysUser();
        user.setUserName("staff"); user.setNickName("录入人");
        LoginUser login=new LoginUser(7L,1L,user,Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
    }

    private Map<String,Object> input() {
        Map<String,Object> value=new HashMap<>();
        value.put("shopId",10L); value.put("orderNo"," 001234 ");
        value.put("deliveryStatus","SIGNED"); value.put("freightAmount","57.50");
        value.put("deductionMethod","ORDER_PAYMENT");
        return value;
    }

    @Test void recordsCurrentUserAndFixedRate() {
        login();
        when(mapper.shop(10L,7L,false)).thenReturn(Map.of("shopId",10L,"shopName","测试店铺"));
        when(mapper.insert(anyMap())).thenAnswer(call -> { Map<String,Object> value=call.getArgument(0); value.put("id",42L); return 1; });
        assertEquals(42L,service.create(input()));
        org.mockito.ArgumentCaptor<Map<String,Object>> saved=org.mockito.ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(mapper).insert(saved.capture());
        assertEquals("001234",saved.getValue().get("orderNo"));
        assertEquals(7L,saved.getValue().get("actorId"));
        assertEquals("录入人",saved.getValue().get("actorName"));
        assertEquals("0.07",saved.getValue().get("deductionRate").toString());
    }

    @Test void duplicateOrderReturnsActionableError() {
        login();
        when(mapper.shop(10L,7L,false)).thenReturn(Map.of("shopId",10L,"shopName","测试店铺"));
        when(mapper.insert(anyMap())).thenThrow(new DuplicateKeyException("duplicate"));
        ServiceException error=assertThrows(ServiceException.class,() -> service.create(input()));
        assertTrue(error.getMessage().contains("订单号已存在"));
    }

    @Test void importsBothExcelStatusWordingVariantsWithCurrentUser() throws Exception {
        login();
        when(mapper.shops(7L,false)).thenReturn(List.of(Map.of("shopId",10L,"shopName","测试店铺")));
        when(mapper.shop(10L,7L,false)).thenReturn(Map.of("shopId",10L,"shopName","测试店铺"));
        byte[] bytes;
        try(XSSFWorkbook workbook=new XSSFWorkbook(); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            var sheet=workbook.createSheet();
            var header=sheet.createRow(0);
            String[] labels={"店铺","订单状态","订单号","客服姓名","运费金额","划扣方式"};
            for(int i=0;i<labels.length;i++) header.createCell(i).setCellValue(labels[i]);
            var first=sheet.createRow(1);
            first.createCell(0).setCellValue("测试店铺"); first.createCell(1).setCellValue("发货已签收");
            first.createCell(2).setCellValue("00123"); first.createCell(4).setCellValue(57.5);
            first.createCell(5).setCellValue("支付宝个人账户9%");
            var second=sheet.createRow(2);
            second.createCell(0).setCellValue("测试店铺"); second.createCell(1).setCellValue("发货未签收");
            second.createCell(2).setCellValue("00124"); second.createCell(4).setCellValue(80);
            second.createCell(5).setCellValue("订单打款7%");
            workbook.write(output); bytes=output.toByteArray();
        }
        var file=new MockMultipartFile("file","orders.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes);
        assertEquals(2,service.importFile(file));
        org.mockito.ArgumentCaptor<Map<String,Object>> captured=org.mockito.ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(mapper,org.mockito.Mockito.times(2)).insert(captured.capture());
        assertEquals("SIGNED",captured.getAllValues().get(0).get("deliveryStatus"));
        assertEquals("SHIPPED_UNSIGNED",captured.getAllValues().get(1).get("deliveryStatus"));
        assertEquals("0.09",captured.getAllValues().get(0).get("deductionRate").toString());
        assertEquals(7L,captured.getAllValues().get(1).get("actorId"));
    }

    @Test void dashboardUsesScopedFiltersForTotalsAndDailyTrend() {
        login();
        when(mapper.summary(anyMap())).thenReturn(Map.of("total",2L,"freightAmount",new java.math.BigDecimal("137.50")));
        when(mapper.daily(anyMap())).thenReturn(List.of(Map.of("day","2026-11-23","orderCount",2,"freightAmount",137.5)));
        Map<String,Object> result=service.dashboard(Map.of("beginDate","2026-11-01","endDate","2026-11-23"));
        assertEquals(2L,result.get("total"));
        assertEquals(1,((List<?>)result.get("daily")).size());
        org.mockito.ArgumentCaptor<Map<String,Object>> scoped=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).daily(scoped.capture());
        assertEquals(7L,scoped.getValue().get("scopeUserId"));
    }
}
