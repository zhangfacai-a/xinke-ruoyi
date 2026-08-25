package com.xinke.erp.domain.audience;

import java.util.List;

public class AudienceRankBatchDetail
{
    private AudienceRankBatch batch;
    private List<AudienceRankSnapshot> snapshots;

    public AudienceRankBatch getBatch() { return batch; }
    public void setBatch(AudienceRankBatch batch) { this.batch = batch; }
    public List<AudienceRankSnapshot> getSnapshots() { return snapshots; }
    public void setSnapshots(List<AudienceRankSnapshot> snapshots) { this.snapshots = snapshots; }
}
