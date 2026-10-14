package com.timeforest.ai_agent_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_roles")
public class AiRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "avatar_emoji", length = 10)
    private String avatarEmoji = "🤖";

    /** null = 系统内置角色（所有用户可见），non-null = 用户自定义角色 */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AiRole() {}

    public AiRole(String name, String description, String systemPrompt, String avatarEmoji) {
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.avatarEmoji = avatarEmoji;
    }

    public AiRole(Long userId, String name, String avatarEmoji, String systemPrompt) {
        this.userId = userId;
        this.name = name;
        this.avatarEmoji = avatarEmoji;
        this.systemPrompt = systemPrompt;
        this.description = "";
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getAvatarEmoji() { return avatarEmoji; }
    public void setAvatarEmoji(String avatarEmoji) { this.avatarEmoji = avatarEmoji; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
