package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowRerankerService implements RerankerService {
    private final AppProperties properties;

    // 调用 SiliconFlow Reranker API，对混合召回结果做二阶段精排。
    @Override
    public List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates, int topK) {
        if (!shouldCallRemote(query, candidates)) {
            return limit(candidates, topK);
        }
        try {
            RerankResponse response = client().post()
                    .uri(properties.getRag().getReranker().getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getRag().getReranker().getApiKey())
                    .body(request(query, candidates, topK))
                    .retrieve()
                    .body(RerankResponse.class);
            return applyScores(candidates, response, topK);
        } catch (Exception ex) {
            log.warn("Reranker 调用失败，将保留混合检索粗排结果", ex);
            return limit(candidates, topK);
        }
    }

    // 未开启或缺少 key 时自动降级，方便开源项目本地运行。
    private boolean shouldCallRemote(String query, List<RetrievalCandidate> candidates) {
        return properties.getRag().getReranker().isEnabled()
                && StringUtils.hasText(properties.getRag().getReranker().getApiKey())
                && StringUtils.hasText(query)
                && candidates != null
                && !candidates.isEmpty();
    }

    // 使用配置中的超时时间构建请求客户端。
    private RestClient client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getRag().getReranker().getTimeoutSeconds());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory).build();
    }

    // Reranker 只需要 query 和候选文本，不暴露内部 chunkId。
    private RerankRequest request(String query, List<RetrievalCandidate> candidates, int topK) {
        int topN = Math.min(Math.min(topK, properties.getRag().getReranker().getTopN()), candidates.size());
        List<String> documents = candidates.stream()
                .map(this::documentText)
                .toList();
        return new RerankRequest(properties.getRag().getReranker().getModel(), query, documents, topN, false);
    }

    // 标题可为精排模型补充章节语境。
    private String documentText(RetrievalCandidate candidate) {
        String title = candidate.getTitle() == null ? "" : candidate.getTitle();
        String content = candidate.getContent() == null ? "" : candidate.getContent();
        return (title + "\n" + content).trim();
    }

    // 根据返回的 index 写回 rerankScore，再按分数排序。
    private List<RetrievalCandidate> applyScores(List<RetrievalCandidate> candidates, RerankResponse response, int topK) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return limit(candidates, topK);
        }
        List<RetrievalCandidate> ranked = new ArrayList<>();
        for (RerankResult result : response.results()) {
            if (result.index() == null || result.index() < 0 || result.index() >= candidates.size()) {
                continue;
            }
            RetrievalCandidate candidate = candidates.get(result.index());
            candidate.setRerankScore(result.relevance_score() == null ? 0 : result.relevance_score());
            ranked.add(candidate);
        }
        return ranked.stream()
                .sorted(Comparator.comparingDouble(RetrievalCandidate::getRerankScore).reversed())
                .limit(topK)
                .toList();
    }

    // 降级路径只截取 Top-K，不改变粗排顺序。
    private List<RetrievalCandidate> limit(List<RetrievalCandidate> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream().limit(Math.max(topK, 0)).toList();
    }

    private record RerankRequest(String model, String query, List<String> documents, Integer top_n,
                                 Boolean return_documents) {
    }

    private record RerankResponse(List<RerankResult> results) {
    }

    private record RerankResult(Integer index, Double relevance_score) {
    }
}
