package com.xinke.erp.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.xinke.erp.mapper.BiLiveAnchorMapper;

@Service
public class BiLiveAnchorSyncService
{
    private final BiLiveAnchorMapper mapper;

    public BiLiveAnchorSyncService(BiLiveAnchorMapper mapper){this.mapper=mapper;}

    public int syncSegment(Long segmentId){return segmentId==null?0:mapper.upsertSegment(segmentId);}
    public int syncRecent(){int changed=mapper.upsertRecentSegments();changed+=mapper.markMissingDeleted();return changed;}
    public int markDeleted(Long segmentId){return segmentId==null?0:mapper.markDeleted(segmentId);}
    public Map<String,Object> reconcile(){return mapper.reconcile();}
}
