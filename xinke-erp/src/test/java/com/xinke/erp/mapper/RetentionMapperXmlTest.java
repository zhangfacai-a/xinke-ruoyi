package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class RetentionMapperXmlTest {
 @Test void registersBoundedAndAtomicStatements() {
  String resource="mapper/erp/RetentionMapper.xml"; Configuration c=new Configuration();
  try(InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)){
   assertNotNull(in);new XMLMapperBuilder(in,c,resource,c.getSqlFragments()).parse();
  }catch(Exception e){throw new AssertionError("RetentionMapper.xml cannot be loaded",e);}
  String ns=RetentionMapper.class.getName()+".";
  String update=c.getMappedStatement(ns+"update").getBoundSql(Map.of()).getSql();
  assertTrue(update.contains("and version=?"));
  assertTrue(update.contains("version=version+1"));
  assertTrue(update.contains("first_followed_at=case"));
  assertTrue(update.contains("completed_at=case"));
  String list=c.getMappedStatement(ns+"list").getBoundSql(Map.of("limit",20,"offset",0,"scopeUserId",2L)).getSql();
  assertTrue(list.contains("limit ? offset ?"));
  assertTrue(list.contains("o.owner_id=? or exists"));
  assertTrue(list.contains("customer_admin"));
  String resultFilter=c.getMappedStatement(ns+"list").getBoundSql(Map.of("limit",20,"offset",0,"resultType","REFERRAL_SUCCESS","addressStatus","MISSING")).getSql();
  assertTrue(resultFilter.contains("o.referral_success=1"));
  assertTrue(resultFilter.contains("trim(coalesce(o.address,''))=''"));
  String dashboard=c.getMappedStatement(ns+"dashboard").getBoundSql(Map.of()).getSql();
  assertTrue(dashboard.contains("timestampdiff(second,o.created_at,o.completed_at)"));
  assertTrue(!dashboard.contains("timestampdiff(second,o.first_followed_at,o.completed_at)"));
  assertTrue(c.hasStatement(ns+"historyInsert"));
  assertTrue(c.getMappedStatement(ns+"updateOwner").getBoundSql(Map.of()).getSql().contains("progress<>'COMPLETED'"));
  assertTrue(c.hasStatement(ns+"insertAttachment"));
  assertTrue(c.getMappedStatement(ns+"deleteOrder").getBoundSql(Map.of()).getSql().contains("where id=? and version=?"));
  assertTrue(c.getMappedStatement(ns+"deleteHistory").getBoundSql(Map.of()).getSql().contains("where order_id=?"));
 }
}
