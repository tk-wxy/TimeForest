package com.timeforest.ai_agent_core.dto;

import java.time.LocalDateTime;

public record SessionResponse(
        String id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        RoleResponse role
) {}
