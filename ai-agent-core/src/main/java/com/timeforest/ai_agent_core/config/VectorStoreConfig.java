package com.timeforest.ai_agent_core.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * 覆盖 Spring AI 自动配置的 VectorStore，添加 role_id 元数据字段索引。
 * 这样每个角色的知识库可以在同一个 Redis 索引内通过 role_id 隔离。
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("knowledge_index")
                .prefix("doc:")
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("role_id")
                )
                .initializeSchema(true)
                .build();
    }
}
