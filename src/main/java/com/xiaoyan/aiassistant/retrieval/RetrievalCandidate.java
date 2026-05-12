package com.xiaoyan.aiassistant.retrieval;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
// 检索阶段的候选片段对象。
public class RetrievalCandidate {
    private String id;
    private Long documentId;
    private Long chunkId;
    private String title;
    private String source;
    private String content;
    private double vectorScore;
    private double keywordScore;
    private double fusedScore;
    private double rerankScore;

    public RetrievalCandidate(String id, Long documentId, Long chunkId, String title, String source, String content,
                              double vectorScore, double keywordScore, double fusedScore) {
        this(id, documentId, chunkId, title, source, content, vectorScore, keywordScore, fusedScore, 0);
    }

    public RetrievalCandidate(String id, Long documentId, Long chunkId, String title, String source, String content,
                              double vectorScore, double keywordScore, double fusedScore, double rerankScore) {
        this.id = id;
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.title = title;
        this.source = source;
        this.content = content;
        this.vectorScore = vectorScore;
        this.keywordScore = keywordScore;
        this.fusedScore = fusedScore;
        this.rerankScore = rerankScore;
    }
}

