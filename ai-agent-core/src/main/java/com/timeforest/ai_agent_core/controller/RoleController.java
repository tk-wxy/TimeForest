package com.timeforest.ai_agent_core.controller;

import com.timeforest.ai_agent_core.dto.CreateRoleRequest;
import com.timeforest.ai_agent_core.dto.IngestRoleRequest;
import com.timeforest.ai_agent_core.dto.RoleResponse;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** 获取当前用户可见的角色列表（系统角色 + 自建角色） */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roleService.listRoles(user.getId()));
    }

    /** 创建自定义角色（name + introText + avatarEmoji） */
    @PostMapping
    public ResponseEntity<?> createRole(
            @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal User user) {
        try {
            RoleResponse created = roleService.createRole(user.getId(), request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 删除自建角色（软删除，系统角色不可删） */
    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> deleteRole(
            @PathVariable Long roleId,
            @AuthenticationPrincipal User user) {
        try {
            roleService.deleteRole(roleId, user.getId());
            return ResponseEntity.ok(Map.of("message", "角色已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /** 为已有角色追加知识文本 */
    @PostMapping("/{roleId}/ingest")
    public ResponseEntity<?> ingest(
            @PathVariable Long roleId,
            @RequestBody IngestRoleRequest request,
            @AuthenticationPrincipal User user) {
        try {
            roleService.ingestForRole(roleId, request);
            return ResponseEntity.ok(Map.of("message", "知识入库成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
