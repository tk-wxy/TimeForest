package com.timeforest.ai_agent_core.config;

import com.timeforest.ai_agent_core.entity.AiRole;
import com.timeforest.ai_agent_core.repository.AiRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时，若 ai_roles 表为空，写入一个系统默认角色（userId=null，所有用户可见）。
 * 用户可在此基础上自行创建更多自定义角色。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final AiRoleRepository roleRepository;

    public DataInitializer(AiRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.count() > 0) return;

        AiRole general = new AiRole(
                "通用助手",
                "全能型 AI，可回答各类问题",
                "你是 TimeForest 平台的通用智能助手「林语」，知识渊博，思维敏捷。\n" +
                "请尽量用 Markdown 格式回答，条理清晰、简洁专业。",
                "🌿"
        );
        // userId = null 表示系统角色
        roleRepository.save(general);
        System.out.println("====== 系统默认角色初始化完成 ======");
    }
}
