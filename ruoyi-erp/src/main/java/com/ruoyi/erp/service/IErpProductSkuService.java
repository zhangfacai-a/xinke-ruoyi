package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpProductSku;

public interface IErpProductSkuService
{
    List<ErpProductSku> selectSkuList(ErpProductSku sku);
    ErpProductSku selectSkuById(Long skuId);
    int insertSku(ErpProductSku sku);
    int updateSku(ErpProductSku sku);
    int deleteSkuByIds(Long[] skuIds);
}
