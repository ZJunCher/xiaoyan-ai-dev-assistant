package com.xiaoyan.aiassistant.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 数据库中的知识库 chunk 实体。
public class KbChunk {
    private Long id;
    private Long documentId;
    private String vectorId;
    private Integer chunkIndex;
    private String title;
    private String sectionPath;
    private String content;
    private Integer tokenEstimate;
    private LocalDateTime createdAt;
}

