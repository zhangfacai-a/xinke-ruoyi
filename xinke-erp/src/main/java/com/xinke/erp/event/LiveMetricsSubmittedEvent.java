package com.xinke.erp.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Published inside the upload transaction and handled only after the data commits. */
public final class LiveMetricsSubmittedEvent
{
    private final Map<String, Object> shift;
    private final Map<String, Object> submission;

    public LiveMetricsSubmittedEvent(Map<String, Object> shift, Map<String, Object> submission)
    {
        this.shift = Collections.unmodifiableMap(new LinkedHashMap<>(shift));
        this.submission = Collections.unmodifiableMap(new LinkedHashMap<>(submission));
    }

    public Map<String, Object> getShift()
    {
        return shift;
    }

    public Map<String, Object> getSubmission()
    {
        return submission;
    }
}
