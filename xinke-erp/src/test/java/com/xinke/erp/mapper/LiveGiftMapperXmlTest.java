package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LiveGiftMapperXmlTest {
 @Test void subjectMappingsExposeIndependentShopRoles() throws Exception {
  String resource="mapper/erp/LiveGiftMapper.xml";Configuration config=new Configuration();
  try(InputStream stream=getClass().getClassLoader().getResourceAsStream(resource)){
   assertNotNull(stream);new XMLMapperBuilder(stream,config,resource,config.getSqlFragments()).parse();
  }
  String sql=config.getMappedStatement(LiveGiftMapper.class.getName()+".selectSubjectMappings")
      .getBoundSql(Map.of("subjectType","SHOP","limit",500)).getSql();
  assertTrue(sql.contains("role_code='operator'"));
  assertTrue(sql.contains("role_code='customer_service'"));
  assertTrue(sql.contains("operatorNames"));
  assertTrue(sql.contains("customerServiceNames"));
 }
}
