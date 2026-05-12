package com.xiaoyan.aiassistant.retrieval;

import java.util.List;

// 候选片段重排序接口。
public interface RerankerService {

    List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates, int topK);
}

