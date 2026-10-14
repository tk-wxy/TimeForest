package com.timeforest.ai_agent_core.dto;

import java.time.LocalDateTime;

public record UserMeResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        long sessionCount,
        long roleCount
) {}
