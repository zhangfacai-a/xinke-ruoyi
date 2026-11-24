package com.xinke.erp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginUser;
import com.xinke.erp.service.RetentionService;

class RetentionControllerTest {
 private RetentionService service; private RetentionController controller;
 @BeforeEach void setup(){service=mock(RetentionService.class);controller=new RetentionController(service);SysUser user=new SysUser();user.setUserName("operator");user.setNickName("王修晨");LoginUser login=new LoginUser(2L,1L,user,Set.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 @Test void listPassesFilters(){when(service.list(anyMap())).thenReturn(Map.of("rows",List.of(Map.of("id",9)),"total",1));Map<String,Object> q=Map.of("shopId","3","progress","PENDING");AjaxResult result=controller.list(q);assertEquals(200,result.get("code"));assertEquals(1,((Map<?,?>)result.get("data")).get("total"));verify(service).list(q);}
 @Test void creationUsesAuthenticatedName(){when(service.create(anyMap(),eq(2L),eq("王修晨"))).thenReturn(9L);Map<String,Object> value=Map.of("orderNo","6955000000000000001");AjaxResult result=controller.create(value);assertEquals(9L,((Map<?,?>)result.get("data")).get("id"));verify(service).create(value,2L,"王修晨");}
 @Test void batchOperationsUseAuthenticatedActor(){Map<String,Object> value=Map.of("shopId",1L,"orderNos",List.of("A001"));when(service.batchCreate(value,2L,"王修晨")).thenReturn(Map.of("createdCount",1));controller.batchCreate(value);verify(service).batchCreate(value,2L,"王修晨");Map<String,Object> assign=Map.of("ownerId",3L,"orders",List.of(Map.of("id",9L,"version",0)));controller.batchOwner(assign);verify(service).batchAssign(assign,2L,"王修晨");}
 @Test void updateUsesCurrentActor(){Map<String,Object> value=Map.of("version",0,"isFollowup",true);controller.update(9L,value);verify(service).update(9L,value,2L,"王修晨");}
 @Test void deletionPassesVersion(){assertEquals(200,controller.delete(9L,3L).get("code"));verify(service).delete(9L,3L);}
 @Test void exportWritesCsv() throws Exception {when(service.export(anyMap())).thenReturn(List.of(Map.of("orderNo","6955000000000000001","referralSuccess",1)));HttpServletResponse response=mock(HttpServletResponse.class);StringWriter buffer=new StringWriter();when(response.getWriter()).thenReturn(new PrintWriter(buffer));controller.export(Map.of("beginDate","2026-10-01"),response);verify(response).setContentType("text/csv;charset=UTF-8");assertTrue(buffer.toString().contains("'6955000000000000001"));assertTrue(buffer.toString().contains("\"是\""));}
}
