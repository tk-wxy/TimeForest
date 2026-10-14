package com.timeforest.ai_agent_core.controller;

import com.timeforest.ai_agent_core.dto.*;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    private Long userId(@AuthenticationPrincipal User user) {
        return user == null ? null : user.getId();
    }

    /* ═══════════ 发布列表 ═══════════ */
    @GetMapping("/blogs")
    public BlogPageResponse listPublished(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User user) {
        return blogService.listPublished(q, page, size, userId(user));
    }

    /* ═══════════ 我的草稿/已发布 ═══════════ */
    @GetMapping("/blogs/mine")
    public List<BlogCardResponse> listMine(
            @RequestParam(defaultValue = "draft") String status,
            @AuthenticationPrincipal User user) {
        if (user == null) return List.of();
        return blogService.listMine(user.getId(), status);
    }

    /* ═══════════ 我的收藏 ═══════════ */
    @GetMapping("/blogs/favorites")
    public BlogPageResponse listFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return new BlogPageResponse(List.of(), 0, size, 0, 0);
        }
        return blogService.listFavorites(user.getId(), page, size);
    }

    /* ═══════════ 创建草稿 ═══════════ */
    @PostMapping("/blogs")
    public BlogDetailResponse createDraft(@AuthenticationPrincipal User user) {
        return blogService.createDraft(user.getId());
    }

    /* ═══════════ 阅读详情 ═══════════ */
    @GetMapping("/blogs/{id}")
    public BlogDetailResponse getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return blogService.getDetail(id, userId(user));
    }

    /* ═══════════ 更新草稿 ═══════════ */
    @PutMapping("/blogs/{id}")
    public BlogDetailResponse updateDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody UpdateBlogRequest request) {
        return blogService.updateDraft(id, user.getId(), request);
    }

    /* ═══════════ 发布 ═══════════ */
    @PutMapping("/blogs/{id}/publish")
    public BlogDetailResponse publish(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody PublishBlogRequest request) {
        return blogService.publish(id, user.getId(), request);
    }

    /* ═══════════ 删除 ═══════════ */
    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        blogService.deletePost(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    /* ═══════════ 点赞/取消点赞 ═══════════ */
    @PostMapping("/blogs/{id}/like")
    public ToggleResponse toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return blogService.toggleLike(id, user.getId());
    }

    /* ═══════════ 收藏/取消收藏 ═══════════ */
    @PostMapping("/blogs/{id}/favorite")
    public ToggleResponse toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return blogService.toggleFavorite(id, user.getId());
    }

    /* ═══════════ 评论列表 ═══════════ */
    @GetMapping("/blogs/{id}/comments")
    public List<BlogCommentResponse> listComments(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return blogService.listCommentsForOwner(id, userId(user));
    }

    /* ═══════════ 发表评论 ═══════════ */
    @PostMapping("/blogs/{id}/comments")
    public BlogCommentResponse addComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody CreateCommentRequest request) {
        return blogService.addComment(id, user.getId(), request);
    }

    /* ═══════════ 删除评论 ═══════════ */
    @DeleteMapping("/blogs/{blogId}/comments/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long blogId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        blogService.deleteComment(blogId, commentId, user.getId());
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
