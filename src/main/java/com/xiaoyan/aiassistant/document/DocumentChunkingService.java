package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 文档分块服务，统一封装策略路由和实际切分。
@Service
@RequiredArgsConstructor
public class DocumentChunkingService {
    private final ChunkingStrategyRouter router;
    private final StructureAwareChunker structureAwareChunker;
    private final SemanticChunker semanticChunker;
    private final HybridChunker hybridChunker;

    // 先路由策略，再交给对应分块器执行。
    public List<DocumentChunk> split(String text) {
        ChunkingStrategyRouter.ChunkingDecision decision = router.route(text);
        return switch (decision.strategy()) {
            case STRUCTURE -> structureAwareChunker.split(text);
            case SEMANTIC -> semanticChunker.split(text);
            case HYBRID -> hybridChunker.split(text);
        };
    }
}

