package com.timeforest.ai_agent_core.controller;

import com.timeforest.ai_agent_core.dto.UserMeResponse;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.repository.AiRoleRepository;
import com.timeforest.ai_agent_core.repository.ChatSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ChatSessionRepository sessionRepository;
    private final AiRoleRepository roleRepository;

    public UserController(ChatSessionRepository sessionRepository, AiRoleRepository roleRepository) {
        this.sessionRepository = sessionRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal User user) {
        long sessionCount = sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).size();
        long roleCount = roleRepository.findAll().stream()
                .filter(r -> r.isActive() && user.getId().equals(r.getUserId()))
                .count();

        return ResponseEntity.ok(new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                sessionCount,
                roleCount
        ));
    }
}
