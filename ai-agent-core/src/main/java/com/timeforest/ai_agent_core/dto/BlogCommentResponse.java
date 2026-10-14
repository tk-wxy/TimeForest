package com.timeforest.ai_agent_core.dto;

import java.time.LocalDateTime;

public record BlogCommentResponse(
        Long id,
        String content,
        String authorName,
        Long authorId,
        boolean owner,
        LocalDateTime createdAt
) {}
