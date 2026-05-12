package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChunkingStrategyRouter {
    private final DocumentStructureAnalyzer analyzer;

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

    public record ChunkingDecision(ChunkingStrategy strategy, DocumentStructureFeatures features) {
    }
}

