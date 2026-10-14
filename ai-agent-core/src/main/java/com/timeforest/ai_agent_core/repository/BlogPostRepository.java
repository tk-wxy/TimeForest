package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.BlogPost;
import com.timeforest.ai_agent_core.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findByIdAndUserId(Long id, Long userId);

    List<BlogPost> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, BlogStatus status);

    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.status = 'PUBLISHED'
            AND (:q IS NULL OR :q = '' OR
                 LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                 LOWER(b.subtitle) LIKE LOWER(CONCAT('%', :q, '%')) OR
                 LOWER(b.content) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY b.publishedAt DESC
            """)
    Page<BlogPost> searchPublished(@Param("q") String q, Pageable pageable);

    Page<BlogPost> findByStatusOrderByPublishedAtDesc(BlogStatus status, Pageable pageable);
}
