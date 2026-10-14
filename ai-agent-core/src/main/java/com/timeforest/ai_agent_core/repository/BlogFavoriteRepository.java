package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.BlogFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogFavoriteRepository extends JpaRepository<BlogFavorite, Long> {

    Optional<BlogFavorite> findByBlogIdAndUserId(Long blogId, Long userId);

    boolean existsByBlogIdAndUserId(Long blogId, Long userId);

    void deleteByBlogIdAndUserId(Long blogId, Long userId);

    void deleteByBlogId(Long blogId);

    Page<BlogFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
