package com.timeforest.ai_agent_core;

import com.timeforest.ai_agent_core.dto.AskRequest;
import com.timeforest.ai_agent_core.entity.AiRole;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.service.RagAgentService;
import com.timeforest.ai_agent_core.service.RoleService;
import com.timeforest.ai_agent_core.service.SessionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class AiController {

    private final RagAgentService ragService;
    private final SessionService sessionService;
    private final RoleService roleService;

    public AiController(RagAgentService ragService,
                        SessionService sessionService,
                        RoleService roleService) {
        this.ragService = ragService;
        this.sessionService = sessionService;
        this.roleService = roleService;
    }

    /** 手动触发 knowledge.txt 入库（兼容旧接口，默认写入 roleId=4 知识达人）*/
    @GetMapping("/ingest")
    public String ingest(@RequestParam(defaultValue = "4") Long roleId) {
        roleService.getRole(roleId); // 验证角色存在
        // 直接触发 RoleService 的 ingest（由 knowledge.txt 内容决定）
        return "请使用 POST /api/roles/" + roleId + "/ingest 接口，携带 text 字段入库。";
    }

    /**
     * 流式问答（POST），需 JWT。
     * 角色由会话的 role_id 决定，无需前端额外传递。
     */
    @PostMapping(value = "/api/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> askStream(
            @RequestBody AskRequest request,
            @AuthenticationPrincipal User user) {

        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.sessionId() == null || !sessionService.validateOwnership(request.sessionId(), user.getId())) {
            return ResponseEntity.status(403).body(Flux.just("无权访问此会话"));
        }

        // 查出会话绑定的角色
        AiRole role = sessionService.getRoleForSession(request.sessionId());

        sessionService.updateSessionTitle(request.sessionId(), request.question());

        Flux<String> stream = ragService.askStream(request.question(), request.sessionId(), role)
                .doOnComplete(() -> sessionService.touchSession(request.sessionId()));

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }
}
