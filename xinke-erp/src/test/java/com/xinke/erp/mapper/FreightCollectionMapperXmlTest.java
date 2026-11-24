package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class FreightCollectionMapperXmlTest {
    @Test void registersScopedQueriesAndVersionedUpdate() {
        String resource="mapper/erp/FreightCollectionMapper.xml";
        Configuration config=new Configuration();
        try(InputStream input=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input,config,resource,config.getSqlFragments()).parse();
        } catch(Exception ex) { throw new AssertionError("Freight mapper cannot be loaded",ex); }
        String prefix=FreightCollectionMapper.class.getName()+".";
        String list=config.getMappedStatement(prefix+"list").getBoundSql(Map.of("limit",20,"offset",0,"scopeUserId",7L)).getSql();
        assertTrue(list.contains("created_by_user_id=? or exists"));
        assertTrue(list.contains("customer_admin"));
        assertTrue(list.contains("limit ? offset ?"));
        String update=config.getMappedStatement(prefix+"update").getBoundSql(Map.of()).getSql();
        assertTrue(update.contains("where id=? and version=?"));
        assertTrue(update.contains("version=version+1"));
        String dashboard=config.getMappedStatement(prefix+"daily").getBoundSql(Map.of("scopeUserId",7L)).getSql();
        assertTrue(dashboard.contains("created_by_user_id=? or exists"));
        assertTrue(dashboard.contains("group by date(f.created_at)"));
        String summary=config.getMappedStatement(prefix+"summary").getBoundSql(Map.of("scopeUserId",7L)).getSql();
        assertTrue(summary.contains("customer_admin"));
        assertTrue(summary.contains("alipay" ) || summary.contains("ALIPAY_PERSONAL"));
    }
}
