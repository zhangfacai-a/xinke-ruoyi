package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.mapper.ErpPurchaseOrderMapper;
import com.xinke.erp.service.IErpPurchaseOrderService;

@Service
public class ErpPurchaseOrderServiceImpl implements IErpPurchaseOrderService
{
    @Autowired
    private ErpPurchaseOrderMapper purchaseOrderMapper;

    @Override
    public List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder)
    {
        return purchaseOrderMapper.selectPurchaseOrderList(purchaseOrder);
    }

    @Override
    public ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId)
    {
        return purchaseOrderMapper.selectPurchaseOrderById(purchaseId);
    }

    @Override
    public int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder)
    {
        return purchaseOrderMapper.insertPurchaseOrder(purchaseOrder);
    }

    @Override
    public int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder)
    {
        return purchaseOrderMapper.updatePurchaseOrder(purchaseOrder);
    }

    @Override
    public int deletePurchaseOrderByIds(Long[] purchaseIds)
    {
        return purchaseOrderMapper.deletePurchaseOrderByIds(purchaseIds);
    }
}
