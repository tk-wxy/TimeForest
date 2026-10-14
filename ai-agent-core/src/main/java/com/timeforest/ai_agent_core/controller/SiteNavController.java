package com.timeforest.ai_agent_core.controller;

import com.timeforest.ai_agent_core.dto.AddSiteRequest;
import com.timeforest.ai_agent_core.dto.SiteCardResponse;
import com.timeforest.ai_agent_core.entity.User;
import com.timeforest.ai_agent_core.service.SiteNavService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SiteNavController {

    private final SiteNavService siteNavService;

    public SiteNavController(SiteNavService siteNavService) {
        this.siteNavService = siteNavService;
    }

    /* 获取所有网站导航 */
    @GetMapping("/sites")
    public List<SiteCardResponse> listAll(@AuthenticationPrincipal User user) {
        return siteNavService.listAll(user != null ? user.getId() : null);
    }

    /* 添加网站 */
    @PostMapping("/sites")
    public SiteCardResponse addSite(
            @AuthenticationPrincipal User user,
            @RequestBody AddSiteRequest request) {
        return siteNavService.addSite(user.getId(), request);
    }

    /* 删除网站 */
    @DeleteMapping("/sites/{id}")
    public ResponseEntity<Map<String, String>> deleteSite(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        siteNavService.deleteSite(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}