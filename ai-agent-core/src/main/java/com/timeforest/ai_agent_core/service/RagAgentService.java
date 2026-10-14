package com.timeforest.ai_agent_core.service;

import com.timeforest.ai_agent_core.entity.AiRole;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagAgentService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagAgentService(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.vectorStore = vectorStore;
    }

    /**
     * RAG + 角色人格 + 多轮记忆 + 流式输出。
     *
     * @param question       用户问题
     * @param conversationId 会话 UUID（对应 MySQL chat_sessions.id）
     * @param role           当前角色（含 systemPrompt 和 id 用于知识库过滤）
     */
    public Flux<String> askStream(String question, String conversationId, AiRole role) {
        // 按角色 ID 过滤向量库，确保每个角色只访问自己的知识库
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .filterExpression(filter.eq("role_id", String.valueOf(role.getId())).build())
                        .build()
        );

        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 角色专属系统提示 + 动态注入检索内容
        String systemPrompt = role.getSystemPrompt() + "\n\n" +
                (context.isBlank()
                        ? "【本次查询未检索到相关知识库内容，请直接使用通用知识作答】"
                        : "【参考资料】（来自本角色知识库，请优先参考）:\n" + context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content();
    }
}
