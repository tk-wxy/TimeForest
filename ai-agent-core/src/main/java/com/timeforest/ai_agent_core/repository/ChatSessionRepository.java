package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSession> findByIdAndUserId(String id, Long userId);

    void deleteByIdAndUserId(String id, Long userId);
}
