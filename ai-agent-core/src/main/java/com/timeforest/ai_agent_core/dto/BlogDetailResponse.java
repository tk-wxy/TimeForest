package com.timeforest.ai_agent_core.dto;

import com.timeforest.ai_agent_core.entity.BlogStatus;

import java.time.LocalDateTime;

public record BlogDetailResponse(
        Long id,
        String title,
        String subtitle,
        String content,
        String coverImageUrl,
        String coverPosterText,
        BlogStatus status,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long commentCount,
        String authorName,
        Long authorId,
        boolean owner,
        boolean liked,
        boolean favorited,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {}
