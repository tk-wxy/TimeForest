package com.timeforest.ai_agent_core.service;

import com.timeforest.ai_agent_core.dto.CreateRoleRequest;
import com.timeforest.ai_agent_core.dto.IngestRoleRequest;
import com.timeforest.ai_agent_core.dto.RoleResponse;
import com.timeforest.ai_agent_core.entity.AiRole;
import com.timeforest.ai_agent_core.repository.AiRoleRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoleService {

    private final AiRoleRepository roleRepository;
    private final VectorStore vectorStore;

    public RoleService(AiRoleRepository roleRepository, VectorStore vectorStore) {
        this.roleRepository = roleRepository;
        this.vectorStore = vectorStore;
    }

    /** 列出当前用户可访问的角色（系统角色 + 用户自建角色） */
    public List<RoleResponse> listRoles(Long userId) {
        return roleRepository.findAccessibleByUserId(userId)
                .stream()
                .map(r -> toResponse(r, userId))
                .toList();
    }

    /**
     * 创建用户自定义角色。
     * introText 同时作为系统提示词和知识库内容入库。
     */
    public RoleResponse createRole(Long userId, CreateRoleRequest req) {
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("角色名不能为空");
        if (req.introText() == null || req.introText().isBlank()) throw new IllegalArgumentException("角色文本不能为空");

        String emoji = (req.avatarEmoji() == null || req.avatarEmoji().isBlank()) ? "🤖" : req.avatarEmoji();

        // 系统提示词由角色名 + introText 构成
        String systemPrompt = buildSystemPrompt(req.name(), req.introText());

        AiRole role = new AiRole(userId, req.name(), emoji, systemPrompt);
        role.setDescription(truncate(req.introText(), 80));
        roleRepository.save(role);

        // 将 introText 作为知识内容写入向量库（带 role_id 元数据）
        ingestText(role.getId(), req.introText());

        return toResponse(role, userId);
    }

    /** 删除用户自建角色（不能删系统角色） */
    public void deleteRole(Long roleId, Long userId) {
        AiRole role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在或无权删除"));
        role.setActive(false);
        roleRepository.save(role);
    }

    public AiRole getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + id));
    }

    /** 为已有角色追加知识文本（管理接口） */
    public void ingestForRole(Long roleId, IngestRoleRequest request) {
        AiRole role = getRole(roleId);
        if (request.text() == null || request.text().isBlank()) throw new IllegalArgumentException("知识内容不能为空");
        ingestText(role.getId(), request.text());
    }

    // ── Helpers ──────────────────────────────────────────

    private void ingestText(Long roleId, String text) {
        Document doc = new Document(text, Map.of("role_id", String.valueOf(roleId)));
        TokenTextSplitter splitter = new TokenTextSplitter(1000, 200, 5, 10000, true);
        List<Document> chunks = splitter.apply(List.of(doc));
        List<Document> tagged = chunks.stream()
                .map(c -> new Document(c.getText(), Map.of("role_id", String.valueOf(roleId))))
                .toList();
        vectorStore.add(tagged);
        System.out.println("====== 角色 [" + roleId + "] 知识入库完成，共 " + tagged.size() + " 片段 ======");
    }

    private String buildSystemPrompt(String name, String introText) {
        return "你是「" + name + "」。\n\n" +
                "以下是你的身份设定与知识背景：\n" + introText + "\n\n" +
                "请始终以上述身份进行对话，回答时尽量使用 Markdown 格式。\n" +
                "如果问题超出你的知识范围，请坦诚说明，不要捏造信息。";
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public RoleResponse toResponse(AiRole role, Long currentUserId) {
        boolean isOwner = role.getUserId() != null && role.getUserId().equals(currentUserId);
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.getAvatarEmoji(), isOwner);
    }
}
