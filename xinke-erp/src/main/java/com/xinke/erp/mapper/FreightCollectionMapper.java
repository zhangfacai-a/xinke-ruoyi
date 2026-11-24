package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FreightCollectionMapper {
    List<Map<String,Object>> shops(@Param("userId") Long userId, @Param("admin") boolean admin);
    Map<String,Object> shop(@Param("shopId") Long shopId, @Param("userId") Long userId, @Param("admin") boolean admin);
    int isManager(@Param("shopId") Long shopId, @Param("userId") Long userId);
    List<Map<String,Object>> list(Map<String,Object> query);
    Map<String,Object> summary(Map<String,Object> query);
    List<Map<String,Object>> daily(Map<String,Object> query);
    Map<String,Object> get(@Param("id") Long id);
    int insert(Map<String,Object> value);
    int update(Map<String,Object> value);
}
