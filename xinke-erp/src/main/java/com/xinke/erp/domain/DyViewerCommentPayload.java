package com.xinke.erp.domain;

import jakarta.validation.constraints.Size;

public class DyViewerCommentPayload
{
    @Size(max = 128, message = "评论ID长度不能超过128")
    private String id;
    @Size(max = 256, message = "secUid长度不能超过256")
    private String secUid;
    @Size(max = 512, message = "评论去重键长度不能超过512")
    private String dedupeKey;
    @Size(max = 128, message = "昵称长度不能超过128")
    private String nickname;
    @Size(max = 2000, message = "评论内容长度不能超过2000")
    private String content;
    @Size(max = 32, message = "等级长度不能超过32")
    private String level;
    private Long capturedAt;
    @Size(max = 64, message = "评论来源长度不能超过64")
    private String source;
    @Size(max = 500, message = "pageUrl长度不能超过500")
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
