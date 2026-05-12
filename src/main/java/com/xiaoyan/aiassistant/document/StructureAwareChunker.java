package com.xiaoyan.aiassistant.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StructureAwareChunker {
    static final int MAX_CHARS = 1800;
    static final int MIN_CHARS = 260;

    private final DocumentStructureAnalyzer analyzer;

    // 默认从第 0 个 chunk 开始切分。
    public List<DocumentChunk> split(String text) {
        return split(text, 0);
    }

    // 结构分块优先按标题和段落边界切分，避免破坏章节语义。
    public List<DocumentChunk> split(String text, int startIndex) {
        List<Section> sections = toSections(text);
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String title = "";
        String path = "";
        int index = startIndex;

        for (Section section : sections) {
            if (buffer.length() > 0 && buffer.length() + section.text().length() > MAX_CHARS) {
                index = flushWithSecondarySplit(chunks, index, title, path, buffer.toString());
                buffer.setLength(0);
            }
            if (StringUtils.hasText(section.title())) {
                title = section.title();
                path = section.title();
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(section.text());
        }
        if (StringUtils.hasText(buffer)) {
            flushWithSecondarySplit(chunks, index, title, path, buffer.toString());
        }
        return chunks;
    }

    // 将原文拆成标题和正文 section，便于后续按结构聚合。
    private List<Section> toSections(String text) {
        List<Section> sections = new ArrayList<>();
        String[] blocks = safeText(text).split("\\n\\s*\\n");
        String currentTitle = "";
        for (String block : blocks) {
            String trimmed = block.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String firstLine = trimmed.lines().findFirst().orElse(trimmed).trim();
            if (analyzer.isHeading(firstLine, trimmed.length())) {
                currentTitle = normalizeHeading(firstLine);
                sections.add(new Section(currentTitle, currentTitle));
                String rest = trimmed.substring(firstLine.length()).trim();
                if (StringUtils.hasText(rest)) {
                    sections.add(new Section(currentTitle, rest));
                }
            } else {
                sections.add(new Section(currentTitle, trimmed));
            }
        }
        return sections;
    }

    // 超长 section 再按句子边界二次切分。
    private int flushWithSecondarySplit(List<DocumentChunk> chunks, int index, String title, String path, String content) {
        for (String part : secondarySplit(content)) {
            String trimmed = part.trim();
            if (StringUtils.hasText(trimmed)) {
                chunks.add(new DocumentChunk(index++, title, path, trimmed, estimateTokens(trimmed)));
            }
        }
        return index;
    }

    // 句号、问号、感叹号等边界优先，减少句子被硬切。
    List<String> secondarySplit(String content) {
        if (content.length() <= MAX_CHARS) {
            return List.of(content);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String sentence : content.split("(?<=[。！？?!])")) {
            if (buffer.length() > MIN_CHARS && buffer.length() + sentence.length() > MAX_CHARS) {
                parts.add(buffer.toString());
                buffer.setLength(0);
            }
            buffer.append(sentence);
        }
        if (buffer.length() > 0) {
            parts.add(buffer.toString());
        }
        return parts;
    }

    // 粗略估算 token 数，用于记录 chunk 规模。
    static int estimateTokens(String text) {
        int chineseChars = 0;
        int otherChars = 0;
        for (char ch : safeText(text).toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (!Character.isWhitespace(ch)) {
                otherChars++;
            }
        }
        return chineseChars + Math.max(1, otherChars / 4);
    }

    // 兼容 Markdown 标题，去掉开头的 #。
    private String normalizeHeading(String line) {
        return line.replaceFirst("^#{1,6}\\s+", "").trim();
    }

    // 统一处理空文本。
    private static String safeText(String text) {
        return text == null ? "" : text;
    }

    private record Section(String title, String text) {
    }
}
