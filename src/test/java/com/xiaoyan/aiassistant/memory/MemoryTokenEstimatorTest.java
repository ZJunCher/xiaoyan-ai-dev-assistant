package com.xiaoyan.aiassistant.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// MemoryTokenEstimator 的 token 估算测试。
class MemoryTokenEstimatorTest {

    private final MemoryTokenEstimator estimator = new MemoryTokenEstimator();

    // 验证中文和技术词的轻量 token 估算。
    @Test
    void estimatesChineseAndTechnicalText() {
        assertThat(estimator.estimateText("Redis key 命名")).isEqualTo(4);
        assertThat(estimator.estimateText("Java MySQL Redis 1234")).isEqualTo(5);
    }

    // 验证单轮消息包含角色和格式开销。
    @Test
    void turnEstimateIncludesRoleOverhead() {
        int textTokens = estimator.estimateText("Redis key 命名");
        int turnTokens = estimator.estimateTurn(new ChatTurn("user", "Redis key 命名"));

        assertThat(turnTokens).isGreaterThan(textTokens);
    }
}
