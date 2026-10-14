package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

    List<BlogComment> findByBlogIdOrderByCreatedAtAsc(Long blogId);

    Optional<BlogComment> findByIdAndUserId(Long id, Long userId);

    void deleteByBlogId(Long blogId);
}
