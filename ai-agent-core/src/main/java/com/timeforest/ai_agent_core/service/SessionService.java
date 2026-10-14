package com.timeforest.ai_agent_core.service;

import com.timeforest.ai_agent_core.dto.MessageResponse;
import com.timeforest.ai_agent_core.dto.RoleResponse;
import com.timeforest.ai_agent_core.dto.SessionResponse;
import com.timeforest.ai_agent_core.entity.AiRole;
import com.timeforest.ai_agent_core.entity.ChatSession;
import com.timeforest.ai_agent_core.repository.AiRoleRepository;
import com.timeforest.ai_agent_core.repository.ChatSessionRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final AiRoleRepository aiRoleRepository;
    private final ChatMemoryRepository chatMemoryRepository;

    public SessionService(ChatSessionRepository chatSessionRepository,
                          AiRoleRepository aiRoleRepository,
                          ChatMemoryRepository chatMemoryRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.aiRoleRepository = aiRoleRepository;
        this.chatMemoryRepository = chatMemoryRepository;
    }

    public SessionResponse createSession(Long userId, Long roleId) {
        // 若未指定角色，默认使用第一个角色
        Long resolvedRoleId = roleId;
        if (resolvedRoleId == null) {
            resolvedRoleId = aiRoleRepository.findAccessibleByUserId(userId)
                    .stream().findFirst().map(AiRole::getId).orElse(null);
        }
        ChatSession session = new ChatSession(UUID.randomUUID().toString(), userId, resolvedRoleId, "新对话");
        chatSessionRepository.save(session);
        return toResponse(session);
    }

    public List<SessionResponse> listSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .ifPresent(session -> {
                    chatMemoryRepository.deleteByConversationId(sessionId);
                    chatSessionRepository.delete(session);
                });
    }

    public boolean validateOwnership(String sessionId, Long userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId).isPresent();
    }

    /** 根据会话 ID 查出对应的 AiRole 实体，供 RagAgentService 使用 */
    public AiRole getRoleForSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("会话不存在"));
        if (session.getRoleId() != null) {
            return aiRoleRepository.findById(session.getRoleId())
                    .orElseThrow(() -> new IllegalStateException("会话角色已删除"));
        }
        return aiRoleRepository.findAccessibleByUserId(session.getUserId())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("找不到可用角色"));
    }

    public List<MessageResponse> getMessages(String sessionId, Long userId) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或无权访问"));

        return chatMemoryRepository.findByConversationId(sessionId)
                .stream()
                .map(msg -> {
                    String role = msg.getMessageType() == MessageType.USER ? "user" : "assistant";
                    return new MessageResponse(role, msg.getText());
                })
                .toList();
    }

    public void updateSessionTitle(String sessionId, String firstQuestion) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            if ("新对话".equals(session.getTitle())) {
                String title = firstQuestion.length() > 30
                        ? firstQuestion.substring(0, 30) + "…"
                        : firstQuestion;
                session.setTitle(title);
                session.setUpdatedAt(LocalDateTime.now());
                chatSessionRepository.save(session);
            }
        });
    }

    public void touchSession(String sessionId) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
        });
    }

    private SessionResponse toResponse(ChatSession s) {
        RoleResponse roleResp = null;
        if (s.getRoleId() != null) {
            roleResp = aiRoleRepository.findById(s.getRoleId())
                    .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.getAvatarEmoji(), false))
                    .orElse(null);
        }
        return new SessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt(), roleResp);
    }
}
