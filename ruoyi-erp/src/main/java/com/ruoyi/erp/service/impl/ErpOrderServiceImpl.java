package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpOrderItem;
import com.ruoyi.erp.mapper.ErpOrderMapper;
import com.ruoyi.erp.service.IErpOrderService;

@Service
public class ErpOrderServiceImpl implements IErpOrderService
{
    @Autowired
    private ErpOrderMapper orderMapper;

    @Override
    public List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem) { return orderMapper.selectOrderItemList(orderItem); }
    @Override
    public ErpOrderItem selectOrderItemById(Long orderItemId) { return orderMapper.selectOrderItemById(orderItemId); }
}
