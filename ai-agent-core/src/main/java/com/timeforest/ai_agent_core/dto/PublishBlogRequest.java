package com.timeforest.ai_agent_core.dto;

public record PublishBlogRequest(
        String subtitle,
        String coverImageUrl,
        String coverPosterText
) {}
