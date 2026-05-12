package com.xiaoyan.aiassistant.document;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

// 语义分块器，使用 embedding 相似度合并片段。
@Component
@RequiredArgsConstructor
public class SemanticChunker {
    private static final int MAX_CHARS = 1800;
    private static final int MIN_CHARS = 260;
    private static final double SIMILARITY_THRESHOLD = 0.72;

    private final EmbeddingModel embeddingModel;

    // 默认从第 0 个 chunk 开始做语义分块。
    public List<DocumentChunk> split(String text) {
        return split(text, 0, "");
    }

    // 基于相邻片段向量相似度合并语义接近的文本。
    public List<DocumentChunk> split(String text, int startIndex, String title) {
        List<String> units = semanticUnits(text);
        List<DocumentChunk> chunks = new ArrayList<>();
        if (units.isEmpty()) {
            return chunks;
        }

        StringBuilder buffer = new StringBuilder(units.get(0));
        Embedding previousEmbedding = embed(units.get(0));
        int index = startIndex;

        for (int i = 1; i < units.size(); i++) {
            String unit = units.get(i);
            Embedding currentEmbedding = embed(unit);
            double similarity = cosine(previousEmbedding, currentEmbedding);
            boolean tooLong = buffer.length() + unit.length() > MAX_CHARS;
            boolean weakRelation = similarity < SIMILARITY_THRESHOLD;
            if (tooLong || weakRelation) {
                chunks.add(chunk(index++, title, buffer.toString()));
                buffer.setLength(0);
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(unit);
            previousEmbedding = currentEmbedding;
        }
        if (StringUtils.hasText(buffer)) {
            chunks.add(chunk(index, title, buffer.toString()));
        }
        return chunks;
    }

    // 先按空行得到语义单元，过长段落再继续拆句。
    private List<String> semanticUnits(String text) {
        List<String> units = new ArrayList<>();
        for (String block : safeText(text).split("\\n\\s*\\n")) {
            String trimmed = block.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.length() <= MAX_CHARS) {
                units.add(trimmed);
            } else {
                units.addAll(sentenceUnits(trimmed));
            }
        }
        return units;
    }

    // 长段落按中文和英文句末标点继续拆分。
    private List<String> sentenceUnits(String text) {
        List<String> units = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String sentence : text.split("(?<=[。！？?])")) {
            if (buffer.length() > MIN_CHARS && buffer.length() + sentence.length() > MAX_CHARS) {
                units.add(buffer.toString());
                buffer.setLength(0);
            }
            buffer.append(sentence);
        }
        if (buffer.length() > 0) {
            units.add(buffer.toString());
        }
        return units;
    }

    // 生成最终分块对象并估算 token 数。
    private DocumentChunk chunk(int index, String title, String content) {
        String trimmed = content.trim();
        return new DocumentChunk(index, title, title, trimmed, StructureAwareChunker.estimateTokens(trimmed));
    }

    // 调用 embedding 模型获得文本向量。
    private Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    // 计算两个 embedding 的余弦相似度。
    private double cosine(Embedding left, Embedding right) {
        float[] a = left.vector();
        float[] b = right.vector();
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 空文本统一转为空字符串。
    private String safeText(String text) {
        return text == null ? "" : text;
    }
}

