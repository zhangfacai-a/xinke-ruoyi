package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpShop;

public interface IErpShopService
{
    List<ErpShop> selectShopList(ErpShop shop);
    ErpShop selectShopById(Long shopId);
    int insertShop(ErpShop shop);
    int updateShop(ErpShop shop);
    int deleteShopByIds(Long[] shopIds);
}
