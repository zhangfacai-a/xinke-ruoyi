package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.ErpSupplier;

@Mapper
public interface ErpSupplierMapper
{
    List<ErpSupplier> selectSupplierList(ErpSupplier supplier);
    ErpSupplier selectSupplierById(Long supplierId);
    int insertSupplier(ErpSupplier supplier);
    int updateSupplier(ErpSupplier supplier);
    int deleteSupplierByIds(Long[] supplierIds);
}
