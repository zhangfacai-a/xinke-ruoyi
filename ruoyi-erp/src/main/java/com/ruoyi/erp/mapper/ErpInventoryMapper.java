package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.erp.domain.ErpInventoryBalance;

@Mapper
public interface ErpInventoryMapper
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
}
