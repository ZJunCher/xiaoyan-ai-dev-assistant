package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkingService {
    private final ChunkingStrategyRouter router;
    private final StructureAwareChunker structureAwareChunker;
    private final SemanticChunker semanticChunker;
    private final HybridChunker hybridChunker;

    public List<DocumentChunk> split(String text) {
        ChunkingStrategyRouter.ChunkingDecision decision = router.route(text);
        return switch (decision.strategy()) {
            case STRUCTURE -> structureAwareChunker.split(text);
            case SEMANTIC -> semanticChunker.split(text);
            case HYBRID -> hybridChunker.split(text);
        };
    }
}

