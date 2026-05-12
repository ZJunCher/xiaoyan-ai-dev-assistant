package com.xiaoyan.aiassistant.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// QueryIntentAnalyzer 的规则筛选测试。
class QueryIntentAnalyzerTest {

    private final QueryIntentAnalyzer analyzer = new QueryIntentAnalyzer();

    // 短问题应直接判定为单意图。
    @Test
    void shortQuestionIsSingleIntent() {
        QueryIntentAnalyzer.QueryIntentAnalysis analysis = analyzer.analyze("Redis 怎么配置");

        assertThat(analysis.multiIntentCandidate()).isFalse();
    }

    // 复杂并列问题应进入 LLM 多意图判别阶段。
    @Test
    void complexQuestionCanEnterLlmJudgement() {
        QueryIntentAnalyzer.QueryIntentAnalysis analysis = analyzer.analyze("studio-service 怎么启动，另外 Redis 怎么配置，启动失败怎么排查？");

        assertThat(analysis.multiIntentCandidate()).isTrue();
    }
}
