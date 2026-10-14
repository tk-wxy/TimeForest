package com.timeforest.ai_agent_core.dto;

public record SiteCardResponse(
        Long id,
        String url,
        String name,
        String description,
        String iconUrl,
        boolean mine
) {}