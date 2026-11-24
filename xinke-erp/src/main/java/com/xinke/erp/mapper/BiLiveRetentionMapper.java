package com.xinke.erp.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BiLiveRetentionMapper {
    int upsertChanged();
    int markMissingDeleted();
    Map<String,Object> reconcile();
}
