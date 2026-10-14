package com.timeforest.ai_agent_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blog_favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"blog_id", "user_id"}))
public class BlogFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id", nullable = false)
    private Long blogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BlogFavorite() {}

    public BlogFavorite(Long blogId, Long userId) {
        this.blogId = blogId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getBlogId() { return blogId; }
    public Long getUserId() { return userId; }
}
