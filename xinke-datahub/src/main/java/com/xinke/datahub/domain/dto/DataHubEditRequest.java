package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubEditRequest
{
    private Long baseVersionId;
    private List<DataHubRowMutation> mutations = new ArrayList<>();

    public Long getBaseVersionId() { return baseVersionId; }
    public void setBaseVersionId(Long baseVersionId) { this.baseVersionId = baseVersionId; }
    public List<DataHubRowMutation> getMutations() { return mutations; }
    public void setMutations(List<DataHubRowMutation> mutations) { this.mutations = mutations; }
}
