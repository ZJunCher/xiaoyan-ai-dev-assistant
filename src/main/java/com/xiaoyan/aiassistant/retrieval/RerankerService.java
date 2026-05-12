package com.xiaoyan.aiassistant.retrieval;

import java.util.List;

public interface RerankerService {

    List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates, int topK);
}

