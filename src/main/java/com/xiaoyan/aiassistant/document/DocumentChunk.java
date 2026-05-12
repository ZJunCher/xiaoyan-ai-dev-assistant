package com.xiaoyan.aiassistant.document;

public record DocumentChunk(
        int index,
        String title,
        String sectionPath,
        String content,
        int tokenEstimate
) {
}

