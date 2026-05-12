package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SiliconFlowRerankerServiceTest {

    @Test
    void returnsOriginalTopKWhenRerankerDisabled() {
        AppProperties properties = new AppProperties();
        properties.getRag().getReranker().setEnabled(false);
        SiliconFlowRerankerService service = new SiliconFlowRerankerService(properties);

        List<RetrievalCandidate> results = service.rerank("Redis 缓存穿透", List.of(
                candidate(1L),
                candidate(2L),
                candidate(3L)
        ), 2);

        assertThat(results).extracting(RetrievalCandidate::getChunkId).containsExactly(1L, 2L);
    }

    private RetrievalCandidate candidate(Long chunkId) {
        return new RetrievalCandidate("v-" + chunkId, 1L, chunkId, "测试文档", "doc", "Redis 缓存穿透说明", 0, 0, 0);
    }
}
