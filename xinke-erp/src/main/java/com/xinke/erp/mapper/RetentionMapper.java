package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetentionMapper {
 List<Map<String,Object>> shops();
 List<Map<String,Object>> staff();
 int validShop(@Param("id") Long id);
 List<Long> adminShops(@Param("userId") Long userId);
 int isShopAdmin(@Param("shopId") Long shopId,@Param("userId") Long userId);
 int validAssignee(@Param("shopId") Long shopId,@Param("userId") Long userId);
 List<String> existingOrderNos(@Param("shopId") Long shopId,@Param("orderNos") List<String> orderNos);
 String userName(@Param("id") Long id);
 List<Map<String,Object>> list(Map<String,Object> query);
 long count(Map<String,Object> query);
 Map<String,Object> dashboard(Map<String,Object> query);
 Map<String,Object> get(@Param("id") Long id);
 List<Map<String,Object>> history(@Param("id") Long id);
 List<Map<String,Object>> attachments(@Param("id") Long id);
 Map<String,Object> attachment(@Param("id") Long id);
 int insertAttachment(Map<String,Object> value);
 int deleteAttachment(@Param("id") Long id);
 int deleteAttachments(@Param("orderId") Long orderId);
 int insert(Map<String,Object> value);
 int update(Map<String,Object> value);
 int updateOwner(Map<String,Object> value);
 int deleteOrder(@Param("id") Long id,@Param("version") Long version);
 int deleteHistory(@Param("id") Long id);
 int historyInsert(Map<String,Object> value);
}
