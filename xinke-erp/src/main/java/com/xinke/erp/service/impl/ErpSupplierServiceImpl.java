package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpSupplier;
import com.xinke.erp.mapper.ErpSupplierMapper;
import com.xinke.erp.service.IErpSupplierService;

@Service
public class ErpSupplierServiceImpl implements IErpSupplierService
{
    private static final BigDecimal DEFAULT_SCORE = new BigDecimal("80.00");

    @Autowired
    private ErpSupplierMapper supplierMapper;

    @Override
    public List<ErpSupplier> selectSupplierList(ErpSupplier supplier)
    {
        return supplierMapper.selectSupplierList(supplier);
    }

    @Override
    public ErpSupplier selectSupplierById(Long supplierId)
    {
        return supplierMapper.selectSupplierById(supplierId);
    }

    @Override
    public int insertSupplier(ErpSupplier supplier)
    {
        normalizeAndValidate(supplier);
        return supplierMapper.insertSupplier(supplier);
    }

    @Override
    public int updateSupplier(ErpSupplier supplier)
    {
        normalizeAndValidate(supplier);
        return supplierMapper.updateSupplier(supplier);
    }

    @Override
    public int deleteSupplierByIds(Long[] supplierIds)
    {
        for (Long supplierId : supplierIds)
        {
            ErpSupplier supplier = supplierMapper.selectSupplierById(supplierId);
            if (supplier != null && supplier.getPurchaseOrderCount() != null && supplier.getPurchaseOrderCount() > 0)
            {
                throw new ServiceException("供应商已有采购记录，请停用而不是删除：" + supplier.getSupplierName());
            }
        }
        return supplierMapper.deleteSupplierByIds(supplierIds);
    }

    private void normalizeAndValidate(ErpSupplier supplier)
    {
        if (supplier.getSettlementType() == null || supplier.getSettlementType().isBlank()) supplier.setSettlementType("monthly");
        if (supplier.getPaymentDays() == null) supplier.setPaymentDays(30);
        if (supplier.getSupplierLevel() == null || supplier.getSupplierLevel().isBlank()) supplier.setSupplierLevel("B");
        if (supplier.getLeadTimeDays() == null) supplier.setLeadTimeDays(7);
        if (supplier.getMinOrderAmount() == null) supplier.setMinOrderAmount(BigDecimal.ZERO);
        if (supplier.getQualityScore() == null) supplier.setQualityScore(DEFAULT_SCORE);
        if (supplier.getDeliveryScore() == null) supplier.setDeliveryScore(DEFAULT_SCORE);
        if (supplier.getServiceScore() == null) supplier.setServiceScore(DEFAULT_SCORE);
        if (supplier.getPreferredFlag() == null) supplier.setPreferredFlag("0");
        if (supplier.getStatus() == null) supplier.setStatus("0");
        if (supplier.getPaymentDays() < 0 || supplier.getLeadTimeDays() < 0 || supplier.getMinOrderAmount().signum() < 0)
        {
            throw new ServiceException("账期、供货周期和起订金额不能为负数");
        }
        validateScore("质量评分", supplier.getQualityScore());
        validateScore("交付评分", supplier.getDeliveryScore());
        validateScore("服务评分", supplier.getServiceScore());
    }

    private void validateScore(String label, BigDecimal score)
    {
        if (score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0)
        {
            throw new ServiceException(label + "必须在0到100之间");
        }
    }
}
