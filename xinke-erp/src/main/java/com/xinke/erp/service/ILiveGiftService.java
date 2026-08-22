package com.xinke.erp.service;
import java.util.List;import java.util.Map;import com.xinke.erp.domain.LiveGiftBatchSaveRequest;import com.xinke.erp.domain.LiveGiftCostImportRow;import com.xinke.erp.domain.LiveGiftSaveRequest;
public interface ILiveGiftService {
 List<Map<String,Object>> listLiveUserOptions(Map<String,Object> q);
 List<Map<String,Object>> listShopOptions();
 List<Map<String,Object>> listShops(Map<String,Object> q); Map<String,Object> getShop(Long id); void saveShop(Map<String,Object> v,String u);
 List<Map<String,Object>> listRooms(Map<String,Object> q); Map<String,Object> getRoom(Long id); void saveRoom(Map<String,Object> v,String u);
 List<Map<String,Object>> listSubjectMappings(Map<String,Object> q); void saveSubjectMapping(Map<String,Object> v,String u);
 List<Map<String,Object>> listDaily(Map<String,Object> q); Map<String,Object> getDaily(Long id); void saveDaily(Map<String,Object> v,String u);
 List<Map<String,Object>> listGifts(Map<String,Object> q); Map<String,Object> getGift(Long id); void saveGift(Map<String,Object> v,String u); void addCost(Map<String,Object> v,String u); void saveGiftPreference(Map<String,Object> v, Long userId, String username);
 Map<String,Object> importGiftCosts(List<LiveGiftCostImportRow> rows,String u);
 Map<String,Object> getOrder(String orderNo); void saveOrderGift(LiveGiftSaveRequest r,String u);
 Map<String,Object> batchSaveOrderGifts(LiveGiftBatchSaveRequest r,String u);
 List<Map<String,Object>> listQuickTemplates(Long userId); Map<String,Object> getQuickTemplate(Long id,Long userId);
 void saveQuickTemplate(Map<String,Object> v,Long userId,String u); void deleteQuickTemplate(Long id,Long userId);
 List<Map<String,Object>> ledger(Map<String,Object> q); Map<String,Object> summary(Map<String,Object> q);
 Map<String,Object> syncDingTalk(String u);
}
