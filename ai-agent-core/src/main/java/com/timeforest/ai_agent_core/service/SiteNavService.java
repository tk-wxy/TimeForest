package com.timeforest.ai_agent_core.service;

import com.timeforest.ai_agent_core.dto.AddSiteRequest;
import com.timeforest.ai_agent_core.dto.SiteCardResponse;
import com.timeforest.ai_agent_core.entity.SiteNav;
import com.timeforest.ai_agent_core.repository.SiteNavRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SiteNavService {

    private final SiteNavRepository siteNavRepository;
    private final ChatClient chatClient;

    public SiteNavService(SiteNavRepository siteNavRepository,
                          ChatClient.Builder chatClientBuilder) {
        this.siteNavRepository = siteNavRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public List<SiteCardResponse> listAll(Long viewerId) {
        return siteNavRepository.findAll().stream()
                .map(site -> toCard(site, viewerId))
                .toList();
    }

    @Transactional
    public SiteCardResponse addSite(Long userId, AddSiteRequest request) {
        if (request.url() == null || request.url().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "网址不能为空");
        }

        String url = normalizeUrl(request.url());

        // Step 1: 本地默认填充
        String name = defaultName(url);
        String iconUrl = defaultIcon();
        String description = "";

        // Step 2: AI 检索（优先级高，覆盖本地默认）
        if (request.name() != null && !request.name().isBlank()) {
            // 用户手动填写了网站名，优先使用
            name = request.name().trim();
        }
        if (request.iconUrl() != null && !request.iconUrl().isBlank()) {
            // 用户手动填写了图标
            iconUrl = request.iconUrl().trim();
        }
        if (request.description() != null && !request.description().isBlank()) {
            description = request.description().trim();
        }

        // AI 自动检索：只要用户没有手动填写全部三个字段，就尝试用 AI 填充
        boolean needAi = (request.name() == null || request.name().isBlank())
                || (request.iconUrl() == null || request.iconUrl().isBlank())
                || (request.description() == null || request.description().isBlank());

        if (needAi) {
            SiteMetadata aiMeta = fetchMetadataByAI(url);
            if (aiMeta != null) {
                if (aiMeta.name != null && !aiMeta.name.isBlank()
                        && (request.name() == null || request.name().isBlank())) {
                    name = aiMeta.name;
                }
                if (aiMeta.iconUrl != null && !aiMeta.iconUrl.isBlank()
                        && (request.iconUrl() == null || request.iconUrl().isBlank())) {
                    iconUrl = aiMeta.iconUrl;
                }
                if (aiMeta.description != null && !aiMeta.description.isBlank()
                        && (request.description() == null || request.description().isBlank())) {
                    description = aiMeta.description;
                }
            }
        }

        SiteNav site = new SiteNav();
        site.setUrl(url);
        site.setName(name);
        site.setIconUrl(iconUrl);
        site.setDescription(description);
        site.setCreatedBy(userId);
        site = siteNavRepository.save(site);

        return toCard(site, userId);
    }

    @Transactional
    public void deleteSite(Long id, Long userId) {
        SiteNav site = siteNavRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "网站不存在"));
        // 共用网站墙：任何人都可以删除
        siteNavRepository.delete(site);
    }

    private SiteCardResponse toCard(SiteNav site, Long viewerId) {
        return new SiteCardResponse(
                site.getId(),
                site.getUrl(),
                site.getName(),
                site.getDescription(),
                site.getIconUrl(),
                viewerId != null && site.getCreatedBy().equals(viewerId)
        );
    }

    /**
     * 本地默认：从网址提取域名作为网站名
     */
    private String defaultName(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null || host.isBlank()) return "未知网站";
            // 去掉 www. 前缀
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "未知网站";
        }
    }

    /**
     * 本地默认图标：返回空字符串，前端用默认 globe 图标
     */
    private String defaultIcon() {
        return "";
    }

    /**
     * 标准化网址：补全协议
     */
    private String normalizeUrl(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * 通过 AI 检索网站元数据（名称、图标、简介）
     * 返回 null 表示 AI 检索失败，回退到本地默认
     */
    private SiteMetadata fetchMetadataByAI(String url) {
        try {
            String prompt = """
                    你是一个网站信息检索助手。请根据以下网址，检索并返回该网站的基本信息。
                    请严格按照以下 JSON 格式返回，不要包含任何其他内容：

                    {
                      "name": "网站名称",
                      "iconUrl": "网站图标 URL（favicon）",
                      "description": "网站一句话简介"
                    }

                    如果找不到图标 URL，iconUrl 字段返回空字符串 ""。
                    如果找不到简介，description 字段返回空字符串 ""。
                    网站名称务必返回最有代表性的官方名称。

                    网址：%s
                    """.formatted(url);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) return null;

            // 提取 JSON
            String json = extractJson(response);
            if (json == null) return null;

            return parseMetadata(json, url);
        } catch (Exception e) {
            // AI 检索失败，静默回退到本地默认
            return null;
        }
    }

    /**
     * 从 AI 回复中提取 JSON 对象
     */
    private String extractJson(String text) {
        // 尝试匹配 { ... }
        Pattern p = Pattern.compile("\\{[^{}]*\"name\"[^{}]*\\}", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group();
        // 退一步：尝试匹配任何花括号内容
        Pattern p2 = Pattern.compile("\\{[^{}]*\\}");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return m2.group();
        return null;
    }

    /**
     * 简易 JSON 解析（避免引入额外依赖，字段简单直接手工解析）
     */
    private SiteMetadata parseMetadata(String json, String fallbackUrl) {
        try {
            String name = extractJsonString(json, "name");
            String iconUrl = extractJsonString(json, "iconUrl");
            String description = extractJsonString(json, "description");

            if (name == null || name.isBlank()) {
                name = defaultName(fallbackUrl);
            }
            if (iconUrl == null) iconUrl = "";

            return new SiteMetadata(name, iconUrl,
                    description == null ? "" : description);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 JSON 字符串中提取指定 key 的字符串值
     */
    private String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * AI 检索结果内部类
     */
    private record SiteMetadata(String name, String iconUrl, String description) {}
}