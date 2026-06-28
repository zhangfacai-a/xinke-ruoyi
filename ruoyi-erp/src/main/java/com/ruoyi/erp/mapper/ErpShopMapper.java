package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpShop;

@Mapper
public interface ErpShopMapper
{
    List<ErpShop> selectShopList(ErpShop shop);
    ErpShop selectShopById(Long shopId);
    int insertShop(ErpShop shop);
    int updateShop(ErpShop shop);
    int deleteShopByIds(Long[] shopIds);
}
