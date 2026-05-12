package com.xiaoyan.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
// 绑定 app.* 业务配置，集中管理 RAG、记忆和 Pinecone 参数。
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Pinecone pinecone = new Pinecone();
    private Rag rag = new Rag();
    private Memory memory = new Memory();

    @Data
    // Pinecone 向量库配置。
    public static class Pinecone {
        private boolean enabled;
        private String apiKey;
        private String index;
        private String environment;
        private String projectId;
        private String knowledgeNamespace = "knowledge";
        private String memoryNamespace = "memory";
        private String metadataTextKey = "text";
    }

    @Data
    // RAG 检索和重排序配置。
    public static class Rag {
        private int vectorTopK = 8;
        private int bm25TopK = 8;
        private int finalTopK = 6;
        private int candidateTopK = 30;
        private double minVectorScore = 0.5;
        private int maxRewriteQueries = 4;
        private int maxExpandedKeywords = 8;
        private boolean semanticDedupEnabled = true;
        private double semanticDedupThreshold = 0.9;
        private int semanticDedupMaxCandidates = 30;
        private Reranker reranker = new Reranker();
    }

        @Data
        // 外部 Reranker 服务配置。
        public static class Reranker {
        private boolean enabled = false;
        private String baseUrl = "https://api.siliconflow.cn/v1/rerank";
        private String apiKey;
        private String model = "BAAI/bge-reranker-v2-m3";
        private int topN = 6;
        private int timeoutSeconds = 20;
    }

    @Data
    // 短期记忆压缩配置。
    public static class Memory {
        private int maxTokenBudget = 32000;
        private int recentTokenBudget = 24000;
        private double summarizeOverlapRatio = 0.5;
        private int summaryTargetMinChars = 100;
        private int summaryTargetMaxChars = 200;
        private int ttlDays = 7;
    }
}

