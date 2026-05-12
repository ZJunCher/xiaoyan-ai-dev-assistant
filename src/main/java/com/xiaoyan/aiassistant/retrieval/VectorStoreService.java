package com.xiaoyan.aiassistant.retrieval;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// 向量存储服务，封装 embedding、写入、检索和删除。
@Service
public class VectorStoreService {
    private static final String DEFAULT_USER_ID = "default";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> knowledgeStore;
    private final EmbeddingStore<TextSegment> memoryStore;

    public VectorStoreService(
            EmbeddingModel embeddingModel,
            @Qualifier("knowledgeEmbeddingStore") EmbeddingStore<TextSegment> knowledgeStore,
            @Qualifier("memoryEmbeddingStore") EmbeddingStore<TextSegment> memoryStore) {
        this.embeddingModel = embeddingModel;
        this.knowledgeStore = knowledgeStore;
        this.memoryStore = memoryStore;
    }

    // 向指定 namespace 写入一个文本向量。
    public void add(VectorNamespace namespace, String vectorId, String content, Map<String, String> metadata) {
        Embedding embedding = embeddingModel.embed(content).content();
        Metadata segmentMetadata = new Metadata();
        metadata.forEach(segmentMetadata::put);
        store(namespace).addAll(List.of(vectorId), List.of(embedding), List.of(TextSegment.from(content, segmentMetadata)));
    }

    // 执行普通向量检索。
    public List<RetrievalCandidate> search(VectorNamespace namespace, String query, int topK, double minScore) {
        return search(namespace, query, topK, minScore, null);
    }

    // 按 userId 检索长期记忆向量。
    public List<RetrievalCandidate> searchMemory(String userId, String query, int topK, double minScore) {
        int expandedTopK = Math.max(topK * 5, topK);
        return search(VectorNamespace.MEMORY, query, expandedTopK, minScore, normalizeUserId(userId))
                .stream()
                .limit(topK)
                .toList();
    }

    // 带可选 userId 过滤的向量检索。
    private List<RetrievalCandidate> search(VectorNamespace namespace, String query, int topK, double minScore, String userIdFilter) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        Filter filter = userIdFilter == null || DEFAULT_USER_ID.equals(userIdFilter) ? null : new IsEqualTo("userId", userIdFilter);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(minScore)
                .filter(filter)
                .build();
        List<RetrievalCandidate> candidates = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : store(namespace).search(request).matches()) {
            TextSegment segment = match.embedded();
            Metadata metadata = segment == null ? new Metadata() : segment.metadata();
            if (userIdFilter != null && !userIdFilter.equals(normalizeUserId(metadata.getString("userId")))) {
                continue;
            }
            candidates.add(new RetrievalCandidate(
                    match.embeddingId(),
                    parseLong(metadata.getString("documentId")),
                    parseLong(metadata.getString("chunkId")),
                    metadata.getString("title"),
                    metadata.getString("source"),
                    segment == null ? "" : segment.text(),
                    match.score(),
                    0,
                    match.score()
            ));
        }
        return candidates;
    }

    // 根据向量 ID 批量删除向量。
    public void remove(VectorNamespace namespace, Collection<String> vectorIds) {
        if (vectorIds != null && !vectorIds.isEmpty()) {
            store(namespace).removeAll(vectorIds);
        }
    }

    // 根据 namespace 选择向量存储。
    private EmbeddingStore<TextSegment> store(VectorNamespace namespace) {
        return namespace == VectorNamespace.MEMORY ? memoryStore : knowledgeStore;
    }

    // 空用户统一归入 default。
    private String normalizeUserId(String userId) {
        return StringUtils.hasText(userId) ? userId.trim() : DEFAULT_USER_ID;
    }

    // 字符串元数据转换为 Long。
    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
