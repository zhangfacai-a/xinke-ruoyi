package com.xinke.datahub.naming;

public class GeneratedName
{
    private final String identifier;
    private final String source;
    private final boolean needsReview;

    public GeneratedName(String identifier, String source, boolean needsReview)
    {
        this.identifier = identifier;
        this.source = source;
        this.needsReview = needsReview;
    }

    public String getIdentifier() { return identifier; }
    public String getSource() { return source; }
    public boolean isNeedsReview() { return needsReview; }
}
