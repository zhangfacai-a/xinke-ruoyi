package com.xinke.erp.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.SecurityUtils;

/** A personal policy overrides freight roles only; absent policies retain legacy behavior. */
@Service("freightAccess")
public class FreightAccessService {
    public static final Set<String> ACTIONS=Set.of("view","dashboard","create","edit","import","export");
    public record Policy(Set<String> actions,String viewScope,String operateScope,List<Long> viewShopIds,List<Long> operateShopIds,long version) {}
    private final JdbcTemplate jdbc;
    public FreightAccessService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Policy policy(Long id) {
        var rows=jdbc.queryForList("select * from live_personnel_freight_policy where user_id=?",id);
        if(rows.isEmpty()) return null;
        var row=rows.get(0);
        String actions=Objects.toString(row.get("actions"),"");
        return new Policy(actions.isBlank()?Set.of():Set.copyOf(Arrays.asList(actions.split(","))),
            row.get("view_scope").toString(),row.get("operate_scope").toString(),
            ids(row.get("view_shop_ids")),ids(row.get("operate_shop_ids")),((Number)row.get("version")).longValue());
    }
    public static List<Long> ids(Object value) {
        if(value==null || value.toString().isBlank()) return List.of();
        return Arrays.stream(value.toString().split(",")).map(Long::valueOf).distinct().sorted().toList();
    }
    public boolean allowed(String action) {
        if(!ACTIONS.contains(action)) return false;
        if(SecurityUtils.isAdmin()) return true;
        var p=policy(SecurityUtils.getUserId());
        if(p!=null) return p.actions().contains(action);
        return SecurityUtils.hasPermi("live:freight:access") ||
            (Set.of("view","dashboard").contains(action) && SecurityUtils.hasPermi("live:freight:viewAll"));
    }
    public void require(String action) { if(!allowed(action)) throw new ServiceException("没有此项运费操作权限",403); }
    public Policy current() { return SecurityUtils.isAdmin()?null:policy(SecurityUtils.getUserId()); }
    public List<Long> assigned(Long id) {
        return jdbc.queryForList("select distinct subject_id from live_subject_user_map where subject_type='SHOP' and user_id=? and role_code in ('customer_service','customer_admin')",Long.class,id);
    }
    public List<Long> scopeShops(Policy p,boolean operate,Long id) {
        String scope=operate?p.operateScope():p.viewScope();
        return "SELECTED".equals(scope)?(operate?p.operateShopIds():p.viewShopIds()):assigned(id);
    }
    public boolean canOperateShop(Policy p,Long shop,Long id) {
        return "ALL".equals(p.operateScope()) || scopeShops(p,true,id).contains(shop);
    }
    public boolean canEdit(Policy p,Map<String,Object> row,Long id) {
        if(!p.actions().contains("edit")) return false;
        return "OWN".equals(p.operateScope()) ? id.equals(((Number)row.get("createdByUserId")).longValue())
            : canOperateShop(p,((Number)row.get("shopId")).longValue(),id);
    }
    public void applyReadScope(Map<String,Object> query,Policy p,Long id) {
        if("OWN".equals(p.viewScope())) query.put("policyOwnerId",id);
        else if(!"ALL".equals(p.viewScope())) query.put("policyShopIds",scopeShops(p,false,id));
    }
    public Map<String,Object> capabilities() {
        Long id=SecurityUtils.getUserId();
        var p=current();
        Map<String,Object> result=new LinkedHashMap<>();
        for(String action:ACTIONS) result.put(action,allowed(action));
        result.put("managed",p!=null);
        result.put("operateScope",p!=null?p.operateScope():"LEGACY");
        result.put("operateShopIds",p!=null&&!"ALL".equals(p.operateScope())?scopeShops(p,true,id):List.of());
        return result;
    }
}
