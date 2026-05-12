package com.xiaoyan.aiassistant.document;

// 分块阶段的临时对象，保留标题路径和 token 估算。
public record DocumentChunk(
        int index,
        String title,
        String sectionPath,
        String content,
        int tokenEstimate
) {
}

