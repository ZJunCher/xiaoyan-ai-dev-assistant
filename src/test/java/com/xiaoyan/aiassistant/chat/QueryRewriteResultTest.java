package com.xiaoyan.aiassistant.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// QueryRewriteResult 的结果归一化测试。
class QueryRewriteResultTest {

    // 验证语义 query 去重和关键词拼接逻辑。
    @Test
    void buildsSemanticQueriesAndKeywordText() {
        QueryRewriteResult result = new QueryRewriteResult(
                "studio-service 项目如何启动",
                List.of("studio-service 项目启动步骤", "studio-service 项目启动步骤"),
                List.of("启动失败", "Redis 配置"));

        assertThat(result.semanticQueries("这个项目怎么启动"))
                .containsExactly("studio-service 项目如何启动", "studio-service 项目启动步骤");
        assertThat(result.keywordText("这个项目怎么启动"))
                .contains("studio-service 项目如何启动", "启动失败", "Redis 配置");
    }
}
