package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HybridChunker {
    private final DocumentStructureAnalyzer analyzer;
    private final StructureAwareChunker structureAwareChunker;
    private final SemanticChunker semanticChunker;

    public List<DocumentChunk> split(String text) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] blocks = safeText(text).split("\\n\\s*\\n");
        StringBuilder semanticBuffer = new StringBuilder();
        StringBuilder structureBuffer = new StringBuilder();
        String currentTitle = "";
        int index = 0;

        for (String block : blocks) {
            String trimmed = block.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String firstLine = trimmed.lines().findFirst().orElse(trimmed).trim();
            boolean heading = analyzer.isHeading(firstLine, trimmed.length());
            if (heading) {
                index = flushSemantic(chunks, index, currentTitle, semanticBuffer);
                index = flushStructure(chunks, index, structureBuffer);
                currentTitle = firstLine.replaceFirst("^#{1,6}\\s+", "").trim();
                structureBuffer.append(trimmed).append("\n\n");
            } else if (trimmed.length() > StructureAwareChunker.MAX_CHARS) {
                index = flushStructure(chunks, index, structureBuffer);
                appendBlock(semanticBuffer, trimmed);
            } else {
                appendBlock(structureBuffer, trimmed);
            }
        }
        index = flushStructure(chunks, index, structureBuffer);
        flushSemantic(chunks, index, currentTitle, semanticBuffer);
        return chunks;
    }

    private int flushStructure(List<DocumentChunk> chunks, int index, StringBuilder buffer) {
        if (!StringUtils.hasText(buffer)) {
            return index;
        }
        List<DocumentChunk> parts = structureAwareChunker.split(buffer.toString(), index);
        chunks.addAll(parts);
        buffer.setLength(0);
        return index + parts.size();
    }

    private int flushSemantic(List<DocumentChunk> chunks, int index, String title, StringBuilder buffer) {
        if (!StringUtils.hasText(buffer)) {
            return index;
        }
        List<DocumentChunk> parts = semanticChunker.split(buffer.toString(), index, title);
        chunks.addAll(parts);
        buffer.setLength(0);
        return index + parts.size();
    }

    private void appendBlock(StringBuilder buffer, String block) {
        if (buffer.length() > 0) {
            buffer.append("\n\n");
        }
        buffer.append(block);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}

