package com.xinke.datahub.domain.dto;

import com.xinke.datahub.constant.DataHubConstants;

public class DataHubAccess
{
    private final int accessMask;

    public DataHubAccess(int accessMask)
    {
        this.accessMask = accessMask;
    }

    public int getAccessMask() { return accessMask; }
    public boolean isCanRead() { return (accessMask & DataHubConstants.ACCESS_READ) != 0; }
    public boolean isCanImport() { return (accessMask & DataHubConstants.ACCESS_IMPORT) != 0; }
    public boolean isCanManage() { return (accessMask & DataHubConstants.ACCESS_MANAGE) != 0; }
    public boolean isCanEdit() { return (accessMask & DataHubConstants.ACCESS_EDIT) != 0; }
}
