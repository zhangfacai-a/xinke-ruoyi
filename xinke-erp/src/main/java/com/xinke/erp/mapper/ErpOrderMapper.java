package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.ErpOrderItem;

@Mapper
public interface ErpOrderMapper
{
    List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem);
    ErpOrderItem selectOrderItemById(Long orderItemId);
}
