package com.xiaoyan.aiassistant.retrieval;

import com.xiaoyan.aiassistant.document.DocumentMapper;
import com.xiaoyan.aiassistant.document.KbChunk;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class Bm25Service {
    private static final double K1 = 1.5;
    private static final double B = 0.75;
    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHan}]|[A-Za-z0-9_\\-\\.]+");

    private final DocumentMapper documentMapper;
    private final List<IndexedChunk> chunks = new ArrayList<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private double averageLength = 1.0;

    // 应用启动时尝试初始化内存 BM25 索引。
    @PostConstruct
    public void rebuild() {
        try {
            rebuild(documentMapper.findAllChunks());
        } catch (RuntimeException ex) {
            log.warn("BM25 index was not initialized from database: {}. It will be rebuilt after document upload.", ex.getMessage());
            rebuild(List.of());
        }
    }

    // 根据数据库 chunk 重建内存索引。
    public synchronized void rebuild(List<KbChunk> sourceChunks) {
        chunks.clear();
        documentFrequency.clear();
        int totalLength = 0;
        for (KbChunk chunk : sourceChunks) {
            List<String> tokens = tokenize(chunk.getContent());
            Map<String, Integer> frequencies = new HashMap<>();
            for (String token : tokens) {
                frequencies.merge(token, 1, Integer::sum);
            }
            chunks.add(new IndexedChunk(chunk, frequencies, tokens.size()));
            totalLength += tokens.size();
            Set<String> unique = new HashSet<>(tokens);
            unique.forEach(token -> documentFrequency.merge(token, 1, Integer::sum));
        }
        averageLength = chunks.isEmpty() ? 1.0 : Math.max(1.0, totalLength * 1.0 / chunks.size());
    }

    // 使用 BM25 做关键词检索。
    public synchronized List<RetrievalCandidate> search(String query, int topK) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty() || chunks.isEmpty()) {
            return List.of();
        }
        List<RetrievalCandidate> results = new ArrayList<>();
        for (IndexedChunk indexed : chunks) {
            double score = score(queryTokens, indexed);
            if (score > 0) {
                KbChunk chunk = indexed.chunk();
                RetrievalCandidate candidate = new RetrievalCandidate();
                candidate.setId(chunk.getVectorId());
                candidate.setDocumentId(chunk.getDocumentId());
                candidate.setChunkId(chunk.getId());
                candidate.setTitle(chunk.getTitle());
                candidate.setSource("knowledge");
                candidate.setContent(chunk.getContent());
                candidate.setKeywordScore(score);
                candidate.setFusedScore(score);
                results.add(candidate);
            }
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(RetrievalCandidate::getKeywordScore).reversed())
                .limit(topK)
                .toList();
    }

    // 中文按单字切，英文、数字和技术词整体保留。
    public List<String> tokenize(String text) {
        if (text == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    // 计算 query 与单个 chunk 的 BM25 分数。
    private double score(List<String> queryTokens, IndexedChunk chunk) {
        double score = 0;
        int totalChunks = chunks.size();
        for (String token : queryTokens) {
            int tf = chunk.frequencies().getOrDefault(token, 0);
            if (tf == 0) {
                continue;
            }
            int df = documentFrequency.getOrDefault(token, 0);
            double idf = Math.log(1 + (totalChunks - df + 0.5) / (df + 0.5));
            double denominator = tf + K1 * (1 - B + B * chunk.length() / averageLength);
            score += idf * (tf * (K1 + 1)) / denominator;
        }
        return score;
    }

    private record IndexedChunk(KbChunk chunk, Map<String, Integer> frequencies, int length) {
    }
}
