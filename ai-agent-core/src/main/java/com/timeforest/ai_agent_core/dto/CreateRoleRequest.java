package com.timeforest.ai_agent_core.dto;

public record CreateRoleRequest(
        String name,
        String introText,   // 角色介绍文本，兼作系统提示词 + 知识库内容
        String avatarEmoji
) {}
