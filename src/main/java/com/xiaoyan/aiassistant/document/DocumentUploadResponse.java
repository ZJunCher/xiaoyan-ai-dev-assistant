package com.xiaoyan.aiassistant.document;

public record DocumentUploadResponse(
        Long documentId,
        String fileName,
        String status,
        int chunkCount
) {
}

