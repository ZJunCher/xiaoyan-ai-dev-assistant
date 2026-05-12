package com.xiaoyan.aiassistant.document;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class TextCleaner {

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

