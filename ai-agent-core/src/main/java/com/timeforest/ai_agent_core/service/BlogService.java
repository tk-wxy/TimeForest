package com.timeforest.ai_agent_core.service;

import com.timeforest.ai_agent_core.dto.*;
import com.timeforest.ai_agent_core.entity.*;
import com.timeforest.ai_agent_core.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogFavoriteRepository blogFavoriteRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final UserRepository userRepository;

    public BlogService(BlogPostRepository blogPostRepository,
                       BlogLikeRepository blogLikeRepository,
                       BlogFavoriteRepository blogFavoriteRepository,
                       BlogCommentRepository blogCommentRepository,
                       UserRepository userRepository) {
        this.blogPostRepository = blogPostRepository;
        this.blogLikeRepository = blogLikeRepository;
        this.blogFavoriteRepository = blogFavoriteRepository;
        this.blogCommentRepository = blogCommentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BlogDetailResponse createDraft(Long userId) {
        BlogPost post = blogPostRepository.save(new BlogPost(userId));
        return toDetail(post, userId, false);
    }

    public BlogPageResponse listPublished(String q, int page, int size, Long viewerId) {
        Page<BlogPost> result = blogPostRepository.searchPublished(
                blankToNull(q), PageRequest.of(page, Math.min(size, 50)));
        return toPage(result, viewerId);
    }

    public List<BlogCardResponse> listMine(Long userId, String status) {
        BlogStatus blogStatus = "PUBLISHED".equalsIgnoreCase(status)
                ? BlogStatus.PUBLISHED : BlogStatus.DRAFT;
        return blogPostRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, blogStatus)
                .stream()
                .map(p -> toCard(p, userId))
                .toList();
    }

    public BlogPageResponse listFavorites(Long userId, int page, int size) {
        Page<BlogFavorite> favPage = blogFavoriteRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)));

        List<BlogCardResponse> items = new ArrayList<>();
        for (BlogFavorite fav : favPage.getContent()) {
            Optional<BlogPost> opt = blogPostRepository.findById(fav.getBlogId());
            if (opt.isPresent() && opt.get().getStatus() == BlogStatus.PUBLISHED) {
                items.add(toCard(opt.get(), userId));
            }
        }

        return new BlogPageResponse(items, favPage.getNumber(), favPage.getSize(),
                favPage.getTotalElements(), favPage.getTotalPages());
    }

    @Transactional
    public BlogDetailResponse getDetail(Long id, Long viewerId) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));

        if (post.getStatus() == BlogStatus.DRAFT) {
            if (viewerId == null || !post.getUserId().equals(viewerId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
            }
            return toDetail(post, viewerId, false);
        }

        post.setViewCount(post.getViewCount() + 1);
        blogPostRepository.save(post);
        return toDetail(post, viewerId, true);
    }

    @Transactional
    public BlogDetailResponse updateDraft(Long id, Long userId, UpdateBlogRequest request) {
        BlogPost post = requireOwnedDraft(id, userId);
        if (request.title() != null && !request.title().isBlank()) {
            post.setTitle(request.title().trim());
        }
        if (request.content() != null) {
            post.setContent(request.content());
        }
        return toDetail(blogPostRepository.save(post), userId, false);
    }

    @Transactional
    public BlogDetailResponse publish(Long id, Long userId, PublishBlogRequest request) {
        BlogPost post = requireOwnedDraft(id, userId);
        if (request.subtitle() == null || request.subtitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写副标题");
        }
        post.setSubtitle(request.subtitle().trim());
        String coverUrl = trimOrNull(request.coverImageUrl());
        String posterText = trimOrNull(request.coverPosterText());
        if (coverUrl == null && posterText == null) {
            posterText = post.getTitle();
        }
        post.setCoverImageUrl(coverUrl);
        post.setCoverPosterText(posterText);
        post.setStatus(BlogStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        return toDetail(blogPostRepository.save(post), userId, false);
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        BlogPost post = blogPostRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        blogLikeRepository.deleteByBlogId(id);
        blogFavoriteRepository.deleteByBlogId(id);
        blogCommentRepository.deleteByBlogId(id);
        blogPostRepository.delete(post);
    }

    @Transactional
    public ToggleResponse toggleLike(Long blogId, Long userId) {
        BlogPost post = requirePublished(blogId);
        var existing = blogLikeRepository.findByBlogIdAndUserId(blogId, userId);
        if (existing.isPresent()) {
            blogLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            blogPostRepository.save(post);
            return new ToggleResponse(false, post.getLikeCount());
        }
        blogLikeRepository.save(new BlogLike(blogId, userId));
        post.setLikeCount(post.getLikeCount() + 1);
        blogPostRepository.save(post);
        return new ToggleResponse(true, post.getLikeCount());
    }

    @Transactional
    public ToggleResponse toggleFavorite(Long blogId, Long userId) {
        BlogPost post = requirePublished(blogId);
        var existing = blogFavoriteRepository.findByBlogIdAndUserId(blogId, userId);
        if (existing.isPresent()) {
            blogFavoriteRepository.delete(existing.get());
            post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
            blogPostRepository.save(post);
            return new ToggleResponse(false, post.getFavoriteCount());
        }
        blogFavoriteRepository.save(new BlogFavorite(blogId, userId));
        post.setFavoriteCount(post.getFavoriteCount() + 1);
        blogPostRepository.save(post);
        return new ToggleResponse(true, post.getFavoriteCount());
    }

    public List<BlogCommentResponse> listComments(Long blogId) {
        requirePublished(blogId);
        Map<Long, String> names = loadUsernames(
                blogCommentRepository.findByBlogIdOrderByCreatedAtAsc(blogId)
                        .stream().map(BlogComment::getUserId).collect(Collectors.toSet()));
        return blogCommentRepository.findByBlogIdOrderByCreatedAtAsc(blogId).stream()
                .map(c -> new BlogCommentResponse(
                        c.getId(), c.getContent(), names.getOrDefault(c.getUserId(), "用户"),
                        c.getUserId(), false, c.getCreatedAt()))
                .toList();
    }

    @Transactional
    public BlogCommentResponse addComment(Long blogId, Long userId, CreateCommentRequest request) {
        requirePublished(blogId);
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论不能为空");
        }
        BlogComment comment = blogCommentRepository.save(
                new BlogComment(blogId, userId, request.content().trim()));
        BlogPost post = blogPostRepository.findById(blogId).orElseThrow();
        post.setCommentCount(post.getCommentCount() + 1);
        blogPostRepository.save(post);
        String name = userRepository.findById(userId).map(User::getUsername).orElse("用户");
        return new BlogCommentResponse(comment.getId(), comment.getContent(), name,
                userId, true, comment.getCreatedAt());
    }

    @Transactional
    public void deleteComment(Long blogId, Long commentId, Long userId) {
        BlogComment comment = blogCommentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
        if (!comment.getBlogId().equals(blogId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        blogCommentRepository.delete(comment);
        blogPostRepository.findById(blogId).ifPresent(post -> {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            blogPostRepository.save(post);
        });
    }

    public List<BlogCommentResponse> listCommentsForOwner(Long blogId, Long viewerId) {
        BlogPost post = blogPostRepository.findById(blogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        if (post.getStatus() != BlogStatus.PUBLISHED && !post.getUserId().equals(viewerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        return blogCommentRepository.findByBlogIdOrderByCreatedAtAsc(blogId).stream()
                .map(c -> new BlogCommentResponse(
                        c.getId(), c.getContent(),
                        userRepository.findById(c.getUserId()).map(User::getUsername).orElse("用户"),
                        c.getUserId(), viewerId != null && c.getUserId().equals(viewerId),
                        c.getCreatedAt()))
                .toList();
    }

    private BlogPost requireOwnedDraft(Long id, Long userId) {
        BlogPost post = blogPostRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在"));
        if (post.getStatus() != BlogStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅草稿可编辑");
        }
        return post;
    }

    private BlogPost requirePublished(Long blogId) {
        BlogPost post = blogPostRepository.findById(blogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        if (post.getStatus() != BlogStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        return post;
    }

    private BlogPageResponse toPage(Page<BlogPost> page, Long viewerId) {
        List<BlogCardResponse> items = page.getContent().stream()
                .map(p -> toCard(p, viewerId))
                .toList();
        return new BlogPageResponse(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private BlogCardResponse toCard(BlogPost post, Long viewerId) {
        String author = userRepository.findById(post.getUserId()).map(User::getUsername).orElse("用户");
        boolean liked = viewerId != null && blogLikeRepository.existsByBlogIdAndUserId(post.getId(), viewerId);
        boolean favorited = viewerId != null && blogFavoriteRepository.existsByBlogIdAndUserId(post.getId(), viewerId);
        return new BlogCardResponse(
                post.getId(), post.getTitle(), post.getSubtitle(),
                post.getCoverImageUrl(), post.getCoverPosterText(),
                post.getViewCount(), post.getLikeCount(), post.getFavoriteCount(), post.getCommentCount(),
                author, post.getPublishedAt(), liked, favorited);
    }

    private BlogDetailResponse toDetail(BlogPost post, Long viewerId, boolean countedView) {
        String author = userRepository.findById(post.getUserId()).map(User::getUsername).orElse("用户");
        boolean owner = viewerId != null && post.getUserId().equals(viewerId);
        boolean liked = viewerId != null && blogLikeRepository.existsByBlogIdAndUserId(post.getId(), viewerId);
        boolean favorited = viewerId != null && blogFavoriteRepository.existsByBlogIdAndUserId(post.getId(), viewerId);
        return new BlogDetailResponse(
                post.getId(), post.getTitle(), post.getSubtitle(), post.getContent(),
                post.getCoverImageUrl(), post.getCoverPosterText(), post.getStatus(),
                post.getViewCount(), post.getLikeCount(), post.getFavoriteCount(), post.getCommentCount(),
                author, post.getUserId(), owner, liked, favorited,
                post.getPublishedAt(), post.getUpdatedAt());
    }

    private Map<Long, String> loadUsernames(java.util.Set<Long> ids) {
        return ids.stream().collect(Collectors.toMap(
                id -> id,
                id -> userRepository.findById(id).map(User::getUsername).orElse("用户")));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String trimOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
