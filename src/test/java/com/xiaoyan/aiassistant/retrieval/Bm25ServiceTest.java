package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.document.DocumentMapper;
import com.xiaoyan.aiassistant.document.KbChunk;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Bm25Service 的关键词排序测试。
class Bm25ServiceTest {

    // 验证关键词命中的 chunk 排在前面。
    @Test
    void ranksKeywordMatchedChunkFirst() {
        Bm25Service service = new Bm25Service(mock(DocumentMapper.class));
        service.rebuild(List.of(
                chunk(1L, "Java 代码提交前必须运行单元测试"),
                chunk(2L, "Redis key 必须包含业务前缀")
        ));

        List<RetrievalCandidate> results = service.search("Redis key 命名", 2);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getContent()).contains("Redis key");
    }

    // 构造测试用 chunk。
    private KbChunk chunk(Long id, String content) {
        return new KbChunk(id, 1L, "v-" + id, id.intValue(), "规范", "规范", content, 10, LocalDateTime.now());
    }
}
