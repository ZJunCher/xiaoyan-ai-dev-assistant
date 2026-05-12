package com.xiaoyan.aiassistant.chat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// Query 重写结果，保存独立问题、子问题和扩展关键词。
public record QueryRewriteResult(
        String rewrittenQuery,
        List<String> subQueries,
        List<String> expandedKeywords
) {
    // 构造时统一清理空值和重复项。
    public QueryRewriteResult {
        rewrittenQuery = rewrittenQuery == null ? "" : rewrittenQuery.trim();
        subQueries = normalizeList(subQueries);
        expandedKeywords = normalizeList(expandedKeywords);
    }

    // 向量检索使用完整语义 query，不混入零散关键词。
    public List<String> semanticQueries(String originalQuery) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        addIfText(queries, rewrittenQuery);
        for (String subQuery : subQueries) {
            addIfText(queries, subQuery);
        }
        if (queries.isEmpty()) {
            addIfText(queries, originalQuery);
        }
        return new ArrayList<>(queries);
    }

    // BM25 使用改写问题作为主体，再追加关键词扩展。
    public String keywordText(String originalQuery) {
        StringBuilder builder = new StringBuilder();
        builder.append(rewrittenQuery.isBlank() ? originalQuery : rewrittenQuery);
        for (String keyword : expandedKeywords) {
            builder.append(' ').append(keyword);
        }
        return builder.toString();
    }

    // 兼容旧调用：把语义 query 和关键词合成一段检索文本。
    public String retrievalText(String originalQuery) {
        StringBuilder builder = new StringBuilder();
        for (String query : semanticQueries(originalQuery)) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(query);
        }
        for (String keyword : expandedKeywords) {
            builder.append(' ').append(keyword);
        }
        return builder.toString();
    }

    // 清理列表中的空值和重复值。
    private static List<String> normalizeList(List<String> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : source) {
            addIfText(values, item);
        }
        return new ArrayList<>(values);
    }

    // 只有真实文本才加入集合。
    private static void addIfText(LinkedHashSet<String> values, String item) {
        if (item != null && !item.isBlank()) {
            values.add(item.trim());
        }
    }
}
