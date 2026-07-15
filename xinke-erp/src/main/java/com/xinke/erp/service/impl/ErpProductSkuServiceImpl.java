package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpProductSku;
import com.xinke.erp.mapper.ErpProductSkuMapper;
import com.xinke.erp.service.IErpProductSkuService;

@Service
public class ErpProductSkuServiceImpl implements IErpProductSkuService
{
    @Autowired
    private ErpProductSkuMapper skuMapper;

    @Override
    public List<ErpProductSku> selectSkuList(ErpProductSku sku) { return skuMapper.selectSkuList(sku); }
    @Override
    public ErpProductSku selectSkuById(Long skuId) { return skuMapper.selectSkuById(skuId); }
    @Override
    public int insertSku(ErpProductSku sku) { return skuMapper.insertSku(sku); }
    @Override
    public int updateSku(ErpProductSku sku) { return skuMapper.updateSku(sku); }
    @Override
    public int deleteSkuByIds(Long[] skuIds) { return skuMapper.deleteSkuByIds(skuIds); }
}
