package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 根据文档结构特征路由分块策略。
@Component
@RequiredArgsConstructor
public class ChunkingStrategyRouter {
    private final DocumentStructureAnalyzer analyzer;

    // 根据结构特征分数选择最合适的分块策略。
    public ChunkingDecision route(String text) {
        DocumentStructureFeatures features = analyzer.analyze(text);
        if (features.structureScore() >= 4) {
            return new ChunkingDecision(ChunkingStrategy.STRUCTURE, features);
        }
        if (features.structureScore() <= 2) {
            return new ChunkingDecision(ChunkingStrategy.SEMANTIC, features);
        }
        return new ChunkingDecision(ChunkingStrategy.HYBRID, features);
    }

    // 保存分块策略和结构分析结果，便于调试路由原因。
    public record ChunkingDecision(ChunkingStrategy strategy, DocumentStructureFeatures features) {
    }
}

