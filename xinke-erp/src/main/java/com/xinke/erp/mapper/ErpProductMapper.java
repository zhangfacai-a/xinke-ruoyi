package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.ErpProduct;

@Mapper
public interface ErpProductMapper
{
    List<ErpProduct> selectProductList(ErpProduct product);
    ErpProduct selectProductById(Long productId);
    int insertProduct(ErpProduct product);
    int updateProduct(ErpProduct product);
    int deleteProductByIds(Long[] productIds);
}
