package com.timeforest.ai_agent_core.dto;

import java.time.LocalDateTime;

public record BlogCardResponse(
        Long id,
        String title,
        String subtitle,
        String coverImageUrl,
        String coverPosterText,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long commentCount,
        String authorName,
        LocalDateTime publishedAt,
        boolean liked,
        boolean favorited
) {}
