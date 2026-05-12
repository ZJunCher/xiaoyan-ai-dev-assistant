package com.xiaoyan.aiassistant.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryIntentAnalyzerTest {

    private final QueryIntentAnalyzer analyzer = new QueryIntentAnalyzer();

    @Test
    void shortQuestionIsSingleIntent() {
        QueryIntentAnalyzer.QueryIntentAnalysis analysis = analyzer.analyze("Redis 怎么配置");

        assertThat(analysis.multiIntentCandidate()).isFalse();
    }

    @Test
    void complexQuestionCanEnterLlmJudgement() {
        QueryIntentAnalyzer.QueryIntentAnalysis analysis = analyzer.analyze("studio-service 怎么启动，另外 Redis 怎么配置，启动失败怎么排查？");

        assertThat(analysis.multiIntentCandidate()).isTrue();
    }
}
