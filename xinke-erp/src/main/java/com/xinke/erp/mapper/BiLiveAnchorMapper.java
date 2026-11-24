package com.xinke.erp.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BiLiveAnchorMapper
{
    int upsertSegment(@Param("segmentId") Long segmentId);
    int upsertRecentSegments();
    int markDeleted(@Param("segmentId") Long segmentId);
    int markMissingDeleted();
    Map<String,Object> reconcile();
}
