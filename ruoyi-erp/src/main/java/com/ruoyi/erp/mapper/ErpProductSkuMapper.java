package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpProductSku;

@Mapper
public interface ErpProductSkuMapper
{
    List<ErpProductSku> selectSkuList(ErpProductSku sku);
    ErpProductSku selectSkuById(Long skuId);
    int insertSku(ErpProductSku sku);
    int updateSku(ErpProductSku sku);
    int deleteSkuByIds(Long[] skuIds);
}
