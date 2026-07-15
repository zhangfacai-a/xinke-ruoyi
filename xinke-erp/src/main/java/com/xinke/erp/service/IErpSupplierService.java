package com.xinke.erp.service;

import java.util.List;
import com.xinke.erp.domain.ErpSupplier;

public interface IErpSupplierService
{
    List<ErpSupplier> selectSupplierList(ErpSupplier supplier);
    ErpSupplier selectSupplierById(Long supplierId);
    int insertSupplier(ErpSupplier supplier);
    int updateSupplier(ErpSupplier supplier);
    int deleteSupplierByIds(Long[] supplierIds);
}
