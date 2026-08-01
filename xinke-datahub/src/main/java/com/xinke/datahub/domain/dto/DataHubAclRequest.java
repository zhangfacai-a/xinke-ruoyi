package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubAclRequest
{
    private List<DataHubAclEntryRequest> entries = new ArrayList<>();

    public List<DataHubAclEntryRequest> getEntries() { return entries; }
    public void setEntries(List<DataHubAclEntryRequest> entries) { this.entries = entries; }
}
