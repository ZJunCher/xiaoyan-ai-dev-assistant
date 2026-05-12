package com.xiaoyan.aiassistant.document;

// 文档上传后的响应结果。
public record DocumentUploadResponse(
        Long documentId,
        String fileName,
        String status,
        int chunkCount
) {
}

