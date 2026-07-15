package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpSupplier;
import com.xinke.erp.mapper.ErpSupplierMapper;
import com.xinke.erp.service.IErpSupplierService;

@Service
public class ErpSupplierServiceImpl implements IErpSupplierService
{
    @Autowired
    private ErpSupplierMapper supplierMapper;

    @Override
    public List<ErpSupplier> selectSupplierList(ErpSupplier supplier) { return supplierMapper.selectSupplierList(supplier); }
    @Override
    public ErpSupplier selectSupplierById(Long supplierId) { return supplierMapper.selectSupplierById(supplierId); }
    @Override
    public int insertSupplier(ErpSupplier supplier) { return supplierMapper.insertSupplier(supplier); }
    @Override
    public int updateSupplier(ErpSupplier supplier) { return supplierMapper.updateSupplier(supplier); }
    @Override
    public int deleteSupplierByIds(Long[] supplierIds) { return supplierMapper.deleteSupplierByIds(supplierIds); }
}
