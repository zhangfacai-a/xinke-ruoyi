package com.xinke.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.domain.ErpOrderItem;
import com.xinke.erp.mapper.ErpOrderMapper;
import com.xinke.erp.service.IErpOrderService;

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
