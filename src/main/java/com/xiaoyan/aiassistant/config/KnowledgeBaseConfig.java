package com.xiaoyan.aiassistant.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

// 配置知识库和长期记忆的向量存储 Bean。
@Slf4j
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class KnowledgeBaseConfig {

    // 知识库文档使用独立命名空间，避免和长期记忆向量混在一起。
    @Bean
    @Qualifier("knowledgeEmbeddingStore")
    public EmbeddingStore<TextSegment> knowledgeEmbeddingStore(AppProperties properties) {
        return embeddingStore(properties, properties.getPinecone().getKnowledgeNamespace());
    }

    // 长期记忆使用单独命名空间，后续可以按用户或团队维度扩展隔离策略。
    @Bean
    @Qualifier("memoryEmbeddingStore")
    public EmbeddingStore<TextSegment> memoryEmbeddingStore(AppProperties properties) {
        return embeddingStore(properties, properties.getPinecone().getMemoryNamespace());
    }

    // 测试和本地演示默认使用内存库，避免强依赖外部 Pinecone 服务。
    private EmbeddingStore<TextSegment> embeddingStore(AppProperties properties, String namespace) {
        AppProperties.Pinecone pinecone = properties.getPinecone();
        if (!pinecone.isEnabled() || !StringUtils.hasText(pinecone.getApiKey())) {
            log.warn("Pinecone 未启用或缺少 API Key，namespace={} 将使用内存向量库。", namespace);
            return new InMemoryEmbeddingStore<>();
        }

        // 生产环境打开开关后，两个命名空间会写入同一个 Pinecone index。
        return PineconeEmbeddingStore.builder()
                .apiKey(pinecone.getApiKey())
                .index(pinecone.getIndex())
                .nameSpace(namespace)
                .metadataTextKey(pinecone.getMetadataTextKey())
                .environment(pinecone.getEnvironment())
                .projectId(pinecone.getProjectId())
                .build();
    }
}

