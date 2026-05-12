package com.xiaoyan.aiassistant.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryTokenEstimatorTest {

    private final MemoryTokenEstimator estimator = new MemoryTokenEstimator();

    @Test
    void estimatesChineseAndTechnicalText() {
        assertThat(estimator.estimateText("Redis key 命名")).isEqualTo(4);
        assertThat(estimator.estimateText("Java MySQL Redis 1234")).isEqualTo(5);
    }

    @Test
    void turnEstimateIncludesRoleOverhead() {
        int textTokens = estimator.estimateText("Redis key 命名");
        int turnTokens = estimator.estimateTurn(new ChatTurn("user", "Redis key 命名"));

        assertThat(turnTokens).isGreaterThan(textTokens);
    }
}
