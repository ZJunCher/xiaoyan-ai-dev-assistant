package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.config.AppProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticDeduplicationService {
    private final EmbeddingModel embeddingModel;
    private final AppProperties properties;

    // 对融合后的候选片段做语义去重，减少重复内容占用 Prompt。
    public List<RetrievalCandidate> deduplicate(List<RetrievalCandidate> candidates, double threshold, int limit) {
        if (!properties.getRag().isSemanticDedupEnabled() || candidates == null || candidates.size() <= 1) {
            return safeLimit(candidates, limit);
        }

        List<RetrievalCandidate> accepted = new ArrayList<>();
        List<Embedding> acceptedEmbeddings = new ArrayList<>();
        int maxCandidates = Math.min(limit, properties.getRag().getSemanticDedupMaxCandidates());

        for (RetrievalCandidate candidate : candidates) {
            if (accepted.size() >= maxCandidates) {
                break;
            }
            Embedding embedding = embed(candidate);
            if (embedding == null || !hasSimilarAccepted(embedding, acceptedEmbeddings, threshold)) {
                accepted.add(candidate);
                if (embedding != null) {
                    acceptedEmbeddings.add(embedding);
                }
            }
        }
        return accepted;
    }

    // embedding 失败时降级不过滤该片段，保证问答主流程不中断。
    private Embedding embed(RetrievalCandidate candidate) {
        try {
            String text = searchableText(candidate);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return embeddingModel.embed(text).content();
        } catch (Exception ex) {
            log.warn("语义去重生成 embedding 失败，将跳过该片段的相似度过滤", ex);
            return null;
        }
    }

    // 判断当前片段是否和已保留片段高度相似。
    private boolean hasSimilarAccepted(Embedding current, List<Embedding> acceptedEmbeddings, double threshold) {
        for (Embedding accepted : acceptedEmbeddings) {
            if (cosine(current, accepted) >= threshold) {
                return true;
            }
        }
        return false;
    }

    // 标题和正文一起比较，可以识别同一章节的重复片段。
    private String searchableText(RetrievalCandidate candidate) {
        if (candidate == null) {
            return "";
        }
        String title = candidate.getTitle() == null ? "" : candidate.getTitle();
        String content = candidate.getContent() == null ? "" : candidate.getContent();
        return (title + "\n" + content).trim();
    }

    // 计算两个向量的余弦相似度。
    private double cosine(Embedding left, Embedding right) {
        float[] a = left.vector();
        float[] b = right.vector();
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 降级路径只截断数量，不改变原始粗排顺序。
    private List<RetrievalCandidate> safeLimit(List<RetrievalCandidate> candidates, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream().limit(Math.max(limit, 0)).toList();
    }
}
