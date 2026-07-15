package com.xinke.erp.domain;

public class DyViewerCommentPayload
{
    private String id;
    private String secUid;
    private String dedupeKey;
    private String nickname;
    private String content;
    private String level;
    private Long capturedAt;
    private String source;
    private String pageUrl;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSecUid() { return secUid; }
    public void setSecUid(String secUid) { this.secUid = secUid; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Long getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Long capturedAt) { this.capturedAt = capturedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
}
