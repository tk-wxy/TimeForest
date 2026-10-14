package com.timeforest.ai_agent_core.controller;

import com.timeforest.ai_agent_core.dto.CreateSessionRequest;
import com.timeforest.ai_agent_core.dto.MessageResponse;
import com.timeforest.ai_agent_core.dto.SessionResponse;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) CreateSessionRequest request) {
        Long roleId = request != null ? request.roleId() : null;
        return ResponseEntity.ok(sessionService.createSession(user.getId(), roleId));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sessionService.listSessions(user.getId()));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {
        sessionService.deleteSession(sessionId, user.getId());
        return ResponseEntity.ok(Map.of("message", "会话已删除"));
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {
        try {
            List<MessageResponse> messages = sessionService.getMessages(sessionId, user.getId());
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
