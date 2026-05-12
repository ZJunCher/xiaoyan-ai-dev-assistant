package com.xiaoyan.aiassistant.document;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

// 文档文本清洗器。
@Component
public class TextCleaner {

    // 清洗 Tika 提取结果，减少噪声进入分块和检索。
    public String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return removeRepeatedShortLines(normalized);
    }

    // 去除连续重复短行，主要处理页眉页脚类噪声。
    private String removeRepeatedShortLines(String text) {
        String[] lines = text.split("\\n");
        Set<String> seen = new LinkedHashSet<>();
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() > 80 || seen.add(trimmed)) {
                builder.append(line.stripTrailing()).append('\n');
            }
        }
        return builder.toString().trim();
    }
}

