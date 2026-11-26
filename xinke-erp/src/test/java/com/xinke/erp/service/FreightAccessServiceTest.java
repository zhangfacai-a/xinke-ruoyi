package com.xinke.erp.service;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginUser;
import com.xinke.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FreightAccessServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final FreightAccessService service=new FreightAccessService(jdbc);
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private void login(Set<String> permissions) {
        SysUser user=new SysUser();user.setUserId(7L);
        var login=new LoginUser(7L,1L,user,permissions);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
    }
    private void policy(String actions,String view,String operate) {
        when(jdbc.queryForList(anyString(),eq(7L))).thenReturn(List.of(Map.of(
            "actions",actions,"view_scope",view,"operate_scope",operate,"view_shop_ids","10,20","operate_shop_ids","20","version",1L)));
    }
    @Test void personalReadOnlyPolicyOverridesInheritedWriteRole() {
        login(Set.of("live:freight:access","live:freight:viewAll"));
        policy("view,dashboard","ALL","OWN");
        assertTrue(service.allowed("view"));assertTrue(service.allowed("dashboard"));
        for(String action:List.of("create","edit","import","export")) {
            assertFalse(service.allowed(action));assertEquals(403,assertThrows(ServiceException.class,()->service.require(action)).getCode());
        }
    }
    @Test void viewAllDoesNotExpandSelectedOperationScope() {
        login(Set.of("live:freight:entry"));policy("view,create,edit","ALL","SELECTED");
        var p=service.current();
        assertTrue(service.canOperateShop(p,20L,7L));assertFalse(service.canOperateShop(p,10L,7L));
        assertFalse(service.canEdit(p,Map.of("shopId",10L,"createdByUserId",7L),7L));
        assertTrue(service.canEdit(p,Map.of("shopId",20L,"createdByUserId",99L),7L));
    }
    @Test void readScopesRemainSeparateAndEmptyAssignedScopeIsExplicit() {
        var p=new FreightAccessService.Policy(Set.of("view"),"SELECTED","ALL",List.of(10L),List.of(),1);
        Map<String,Object> q=new HashMap<>();service.applyReadScope(q,p,7L);
        assertEquals(List.of(10L),q.get("policyShopIds"));
        q.clear();p=new FreightAccessService.Policy(Set.of("view"),"OWN","ALL",List.of(),List.of(),1);
        service.applyReadScope(q,p,7L);assertEquals(7L,q.get("policyOwnerId"));assertFalse(q.containsKey("policyShopIds"));
        q.clear();p=new FreightAccessService.Policy(Set.of("view"),"ASSIGNED","ALL",List.of(),List.of(),1);
        service.applyReadScope(q,p,7L);assertEquals(List.of(),q.get("policyShopIds"));
    }
    @Test void absentPolicyKeepsLegacyViewerReadOnly() {
        login(Set.of("live:freight:viewAll"));
        assertTrue(service.allowed("view"));assertFalse(service.allowed("create"));assertFalse(service.allowed("export"));
    }
    @Test void disablingAllActionsOverridesRolesImmediately() {
        login(Set.of("live:freight:access"));policy("","OWN","OWN");
        assertFalse(service.allowed("view"));assertFalse(service.allowed("create"));
    }
    @Test void rejectsMalformedConfigurationAndEmptySelectedScope() {
        Map<String,Object> body=new HashMap<>(Map.of("actions",List.of("edit"),"viewScope","ALL","operateScope","OWN","viewShopIds",List.of(),"operateShopIds",List.of()));
        assertThrows(ServiceException.class,()->PersonnelFreightService.validated(body));
        body.put("actions",List.of("view"));body.put("operateScope","SELECTED");
        assertThrows(ServiceException.class,()->PersonnelFreightService.validated(body));
        body.put("operateShopIds",List.of(20));
        assertEquals(List.of(20L),PersonnelFreightService.validated(body).operateShopIds());
        body.put("version",-1);assertThrows(ServiceException.class,()->PersonnelFreightService.validated(body));
    }
}
