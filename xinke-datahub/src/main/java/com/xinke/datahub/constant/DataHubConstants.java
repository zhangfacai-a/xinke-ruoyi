package com.xinke.datahub.constant;

public final class DataHubConstants
{
    public static final int ACCESS_READ = 1;
    public static final int ACCESS_IMPORT = 2;
    public static final int ACCESS_MANAGE = 4;
    public static final int ACCESS_EDIT = 8;
    public static final int ACCESS_ALL = ACCESS_READ | ACCESS_IMPORT | ACCESS_MANAGE | ACCESS_EDIT;

    public static final String SUBJECT_USER = "USER";
    public static final String SUBJECT_ROLE = "ROLE";

    private DataHubConstants()
    {
    }
}
