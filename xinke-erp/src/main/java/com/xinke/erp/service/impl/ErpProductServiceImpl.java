package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpProduct;
import com.xinke.erp.mapper.ErpProductMapper;
import com.xinke.erp.service.IErpProductService;

@Service
public class ErpProductServiceImpl implements IErpProductService
{
    @Autowired
    private ErpProductMapper productMapper;

    @Override
    public List<ErpProduct> selectProductList(ErpProduct product) { return productMapper.selectProductList(product); }
    @Override
    public ErpProduct selectProductById(Long productId) { return productMapper.selectProductById(productId); }
    @Override
    public int insertProduct(ErpProduct product) { return productMapper.insertProduct(product); }
    @Override
    public int updateProduct(ErpProduct product) { return productMapper.updateProduct(product); }
    @Override
    public int deleteProductByIds(Long[] productIds) { return productMapper.deleteProductByIds(productIds); }
}
