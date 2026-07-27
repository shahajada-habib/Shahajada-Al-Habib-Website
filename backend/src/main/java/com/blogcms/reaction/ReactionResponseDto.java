package com.blogcms.reaction;

public class ReactionResponseDto {

    private Long newsId;
    private long likeCount;
    private long dislikeCount;
    private String reactionType;

    public ReactionResponseDto(Long newsId, long likeCount, long dislikeCount, String reactionType) {
        this.newsId = newsId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.reactionType = reactionType;
    }

    public Long getNewsId() {
        return newsId;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public String getReactionType() {
        return reactionType;
    }
}
