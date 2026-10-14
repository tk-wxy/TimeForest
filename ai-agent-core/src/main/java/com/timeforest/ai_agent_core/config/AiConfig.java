package com.timeforest.ai_agent_core.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * 使用 JDBC 持久化记忆（由 spring-ai-autoconfigure-model-chat-memory-repository-jdbc 自动配置）。
     * 每个 conversationId 对应一个用户会话，消息存储在 MySQL 的 SPRING_AI_CHAT_MEMORY 表中。
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
