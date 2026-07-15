package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpProduct;

public interface IErpProductService
{
    List<ErpProduct> selectProductList(ErpProduct product);
    ErpProduct selectProductById(Long productId);
    int insertProduct(ErpProduct product);
    int updateProduct(ErpProduct product);
    int deleteProductByIds(Long[] productIds);
}
