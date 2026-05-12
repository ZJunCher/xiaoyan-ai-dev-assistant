package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 混合检索服务，融合向量召回、BM25、去重和精排。
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {
    private static final int RRF_K = 60;

    private final VectorStoreService vectorStoreService;
    private final Bm25Service bm25Service;
    private final AppProperties properties;
    private final SemanticDeduplicationService semanticDeduplicationService;
    private final RerankerService rerankerService;

    // 兼容单 query 检索入口。
    public List<RetrievalCandidate> retrieveKnowledge(String query) {
        return retrieveKnowledge(List.of(query), query);
    }

    // 同时执行多路向量检索和关键词检索。
    public List<RetrievalCandidate> retrieveKnowledge(List<String> semanticQueries, String keywordQuery) {
        List<List<RetrievalCandidate>> vectorGroups = new ArrayList<>();

        for (String query : normalizeQueries(semanticQueries)) {
            vectorGroups.add(vectorStoreService.search(
                    VectorNamespace.KNOWLEDGE,
                    query,
                    properties.getRag().getVectorTopK(),
                    properties.getRag().getMinVectorScore()));
        }

        List<List<RetrievalCandidate>> keywordGroups = new ArrayList<>();

        if (StringUtils.hasText(keywordQuery)) {
            keywordGroups.add(bm25Service.search(keywordQuery, properties.getRag().getBm25TopK()));
        }

        List<RetrievalCandidate> fused = fuseMultiple(vectorGroups, keywordGroups, properties.getRag().getCandidateTopK());
        List<RetrievalCandidate> deduplicated = semanticDeduplicationService.deduplicate(
                fused,
                properties.getRag().getSemanticDedupThreshold(),
                properties.getRag().getCandidateTopK());
        return rerankerService.rerank(rerankQuery(semanticQueries, keywordQuery), deduplicated, properties.getRag().getFinalTopK());
    }

    // 兼容单 query 长期记忆检索入口。
    public List<RetrievalCandidate> retrieveLongMemory(String userId, String query) {
        return retrieveLongMemory(userId, List.of(query));
    }

    // 从 memory namespace 按用户过滤召回长期记忆。
    public List<RetrievalCandidate> retrieveLongMemory(String userId, List<String> semanticQueries) {
        List<List<RetrievalCandidate>> vectorGroups = new ArrayList<>();

        for (String query : normalizeQueries(semanticQueries)) {
            vectorGroups.add(vectorStoreService.searchMemory(userId, query, 4, properties.getRag().getMinVectorScore()));
        }
        return fuseMultiple(vectorGroups, List.of(), 4);
    }

    // 融合单路向量结果和单路关键词结果。
    public List<RetrievalCandidate> fuse(List<RetrievalCandidate> vectorResults, List<RetrievalCandidate> keywordResults, int limit) {
        return fuseMultiple(List.of(vectorResults), List.of(keywordResults), limit);
    }

    // 使用 RRF 融合多 query 和多通道检索结果。
    public List<RetrievalCandidate> fuseMultiple(List<List<RetrievalCandidate>> vectorGroups,
                                                 List<List<RetrievalCandidate>> keywordGroups,
                                                 int limit) {
        Map<String, RetrievalCandidate> merged = new LinkedHashMap<>();

        for (List<RetrievalCandidate> group : vectorGroups) {
            addRanked(merged, group, true);
        }

        for (List<RetrievalCandidate> group : keywordGroups) {
            addRanked(merged, group, false);
        }
        return new ArrayList<>(merged.values()).stream()
                .sorted(Comparator
                        .comparingDouble(RetrievalCandidate::getFusedScore).reversed()
                        .thenComparing(candidate -> StringUtils.hasText(candidate.getContent()) ? 0 : 1))
                .limit(limit)
                .toList();
    }

    // 按排名累加 RRF 分数，并按 chunkId 去重。
    private void addRanked(Map<String, RetrievalCandidate> merged, List<RetrievalCandidate> candidates, boolean vector) {
        if (candidates == null) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            RetrievalCandidate candidate = candidates.get(i);

            String key = candidate.getChunkId() == null ? candidate.getId() : String.valueOf(candidate.getChunkId());
            RetrievalCandidate target = merged.computeIfAbsent(key, ignored -> candidate);
            if (vector) {
                target.setVectorScore(Math.max(target.getVectorScore(), candidate.getVectorScore()));
            } else {
                target.setKeywordScore(Math.max(target.getKeywordScore(), candidate.getKeywordScore()));
            }

            double rrf = 1.0 / (RRF_K + i + 1);
            target.setFusedScore(target.getFusedScore() + rrf + completenessBoost(target));
        }
    }

    // 清理空 query 并去重。
    private List<String> normalizeQueries(List<String> queries) {
        if (queries == null) {
            return List.of();
        }
        return queries.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    // 精排优先使用关键词增强后的 query。
    private String rerankQuery(List<String> semanticQueries, String keywordQuery) {
        if (StringUtils.hasText(keywordQuery)) {
            return keywordQuery;
        }
        return String.join("\n", normalizeQueries(semanticQueries));
    }

    // 内容更完整且带标题的片段给少量加权。
    private double completenessBoost(RetrievalCandidate candidate) {
        int length = candidate.getContent() == null ? 0 : candidate.getContent().length();

        if (length > 300 && StringUtils.hasText(candidate.getTitle())) {
            return 0.02;
        }
        return 0;
    }
}

