package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {

    Optional<BlogLike> findByBlogIdAndUserId(Long blogId, Long userId);

    boolean existsByBlogIdAndUserId(Long blogId, Long userId);

    void deleteByBlogIdAndUserId(Long blogId, Long userId);

    void deleteByBlogId(Long blogId);
}
