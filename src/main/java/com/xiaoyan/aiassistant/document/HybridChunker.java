package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

// 混合分块器，结合结构分块和语义分块。
@Component
@RequiredArgsConstructor
public class HybridChunker {
    private final DocumentStructureAnalyzer analyzer;
    private final StructureAwareChunker structureAwareChunker;
    private final SemanticChunker semanticChunker;

    // 混合分块会把标题结构和长文本语义分块结合起来。
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

    // 将结构化缓冲区交给结构感知分块器处理。
    private int flushStructure(List<DocumentChunk> chunks, int index, StringBuilder buffer) {
        if (!StringUtils.hasText(buffer)) {
            return index;
        }
        List<DocumentChunk> parts = structureAwareChunker.split(buffer.toString(), index);
        chunks.addAll(parts);
        buffer.setLength(0);
        return index + parts.size();
    }

    // 将弱结构长文本交给语义分块器处理。
    private int flushSemantic(List<DocumentChunk> chunks, int index, String title, StringBuilder buffer) {
        if (!StringUtils.hasText(buffer)) {
            return index;
        }
        List<DocumentChunk> parts = semanticChunker.split(buffer.toString(), index, title);
        chunks.addAll(parts);
        buffer.setLength(0);
        return index + parts.size();
    }

    // 追加段落时保留空行边界。
    private void appendBlock(StringBuilder buffer, String block) {
        if (buffer.length() > 0) {
            buffer.append("\n\n");
        }
        buffer.append(block);
    }

    // 空文本统一转为空字符串。
    private String safeText(String text) {
        return text == null ? "" : text;
    }
}

