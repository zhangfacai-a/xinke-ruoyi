package com.xinke.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.xinke.erp.domain.ErpInventoryBalance;

@Mapper
public interface ErpInventoryMapper
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
}
