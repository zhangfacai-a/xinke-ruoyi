package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ErpFlowMapper
{
    List<Map<String, Object>> selectFlowList(@Param("tableName") String tableName,
                                             @Param("noColumn") String noColumn,
                                             @Param("statusColumn") String statusColumn,
                                             @Param("query") Map<String, Object> query);

    Map<String, Object> selectFlowById(@Param("tableName") String tableName,
                                       @Param("pkColumn") String pkColumn,
                                       @Param("id") Long id);

    Map<String, Object> selectFlowByNo(@Param("tableName") String tableName,
                                       @Param("noColumn") String noColumn,
                                       @Param("bizNo") String bizNo);

    int insertFlow(@Param("tableName") String tableName, @Param("data") Map<String, Object> data);

    int updateFlow(@Param("tableName") String tableName,
                   @Param("pkColumn") String pkColumn,
                   @Param("id") Long id,
                   @Param("data") Map<String, Object> data);

    int updateFlowStatus(@Param("tableName") String tableName,
                         @Param("noColumn") String noColumn,
                         @Param("statusColumn") String statusColumn,
                         @Param("bizNo") String bizNo,
                         @Param("status") String status,
                         @Param("username") String username);

    int deleteFlowByIds(@Param("tableName") String tableName,
                        @Param("pkColumn") String pkColumn,
                        @Param("ids") Long[] ids);

    Map<String, Object> selectInventoryBalance(@Param("warehouseId") Long warehouseId, @Param("skuId") Long skuId);

    int insertInventoryBalance(Map<String, Object> data);

    int updateInventoryBalance(Map<String, Object> data);

    int insertInventoryMovement(Map<String, Object> data);

    int insertCostLayer(Map<String, Object> data);
}
