package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpOrderItem;

@Mapper
public interface ErpOrderMapper
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);
}
