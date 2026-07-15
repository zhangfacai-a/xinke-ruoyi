package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpProductSku;

public interface IErpProductSkuService
{
    List<ErpProductSku> selectSkuList(ErpProductSku sku);
    ErpProductSku selectSkuById(Long skuId);
    int insertSku(ErpProductSku sku);
    int updateSku(ErpProductSku sku);
    int deleteSkuByIds(Long[] skuIds);
}
