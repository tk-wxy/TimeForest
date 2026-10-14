package com.timeforest.ai_agent_core.dto;

public record AddSiteRequest(
        String url,
        String name,
        String description,
        String iconUrl
) {}