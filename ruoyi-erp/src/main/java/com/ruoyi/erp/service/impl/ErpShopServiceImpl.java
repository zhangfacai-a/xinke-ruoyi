package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpShop;
import com.ruoyi.erp.mapper.ErpShopMapper;
import com.ruoyi.erp.service.IErpShopService;

@Service
public class ErpShopServiceImpl implements IErpShopService
{
    @Autowired
    private ErpShopMapper shopMapper;

    @Override
    public List<ErpShop> selectShopList(ErpShop shop) { return shopMapper.selectShopList(shop); }
    @Override
    public ErpShop selectShopById(Long shopId) { return shopMapper.selectShopById(shopId); }
    @Override
    public int insertShop(ErpShop shop) { return shopMapper.insertShop(shop); }
    @Override
    public int updateShop(ErpShop shop) { return shopMapper.updateShop(shop); }
    @Override
    public int deleteShopByIds(Long[] shopIds) { return shopMapper.deleteShopByIds(shopIds); }
}
