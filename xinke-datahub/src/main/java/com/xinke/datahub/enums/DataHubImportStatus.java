package com.xinke.datahub.enums;

public enum DataHubImportStatus
{
    PARSING,
    PENDING_CONFIRM,
    QUEUED,
    STAGING,
    VALIDATING,
    COMMITTING,
    SUCCESS,
    VALIDATION_FAILED,
    FAILED,
    RECOVERING,
    MANUAL_REQUIRED
}
