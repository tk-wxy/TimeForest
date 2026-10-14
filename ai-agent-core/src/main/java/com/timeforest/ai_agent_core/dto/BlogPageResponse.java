package com.timeforest.ai_agent_core.dto;

import java.util.List;

public record BlogPageResponse(
        List<BlogCardResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
