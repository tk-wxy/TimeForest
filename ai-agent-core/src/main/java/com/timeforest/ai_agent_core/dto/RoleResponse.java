package com.timeforest.ai_agent_core.dto;

public record RoleResponse(
        Long id,
        String name,
        String description,
        String avatarEmoji,
        boolean isOwner      // true = 用户自建，可删除；false = 系统角色
) {}
