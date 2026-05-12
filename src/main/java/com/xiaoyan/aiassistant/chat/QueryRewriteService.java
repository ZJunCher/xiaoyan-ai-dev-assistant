package com.xiaoyan.aiassistant.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.aiassistant.config.AppProperties;
import com.xiaoyan.aiassistant.memory.ChatTurn;
import com.xiaoyan.aiassistant.memory.ConversationMemory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// 负责调用大模型完成 Query 重写。
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final QueryIntentAnalyzer intentAnalyzer;
    private final AppProperties properties;

    // 对当前问题做上下文补全、多意图拆分和关键词扩展。
    public QueryRewriteResult rewrite(String message, ConversationMemory memory) {
        QueryIntentAnalyzer.QueryIntentAnalysis analysis = intentAnalyzer.analyze(message);
        String prompt = buildPrompt(message, memory, analysis);
        try {
            String response = chatModel.chat(prompt);
            String json = extractJson(response);
            QueryRewriteResult result = objectMapper.readValue(json, QueryRewriteResult.class);
            if (result.rewrittenQuery().isBlank()) {
                return fallback(message);
            }
            return normalizeResult(message, result, analysis);
        } catch (Exception ex) {
            log.warn("Query rewrite failed. Falling back to original query: {}", ex.getMessage());
            return fallback(message);
        }
    }

    // Prompt 明确要求输出 JSON，便于后端稳定解析。
    private String buildPrompt(String message,
                               ConversationMemory memory,
                               QueryIntentAnalyzer.QueryIntentAnalysis analysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                你是智能效研AI研发助手的 RAG 检索 Query 重写器。
                请结合会话摘要和最近对话，把当前问题改写为语义完整、独立可理解、适合检索的问题。
                重点补全指代词和省略对象，例如“这个项目”“它”“上面的问题”必须结合上下文还原为明确对象。
                同时进行关键词扩展，补充同义词、技术简称、相关配置名或错误表达，主要用于增强 BM25 关键词检索。
                只输出 JSON，不要输出 Markdown，格式如下：
                {"rewrittenQuery":"...","subQueries":["..."],"expandedKeywords":["..."]}
                """);

        builder.append("\n多意图规则预判：")
                .append(analysis.multiIntentCandidate() ? "可能多意图" : "更像单意图")
                .append("，分数=").append(analysis.score())
                .append("，分词数=").append(analysis.termCount())
                .append("，动词数=").append(analysis.verbCount())
                .append("，原因=").append(analysis.reason())
                .append("\n");

        builder.append("""
                多意图拆分要求：
                1. 如果规则预判为单意图，通常不要拆分，subQueries 只保留 rewrittenQuery。
                2. 如果规则预判为可能多意图，请判断是否包含多个可能分布在不同文档片段中的独立问题。
                3. 只有“需要分别检索不同资料才能回答”的意图才拆分；“A 和 B 的区别”“价格和颜色”这类同一主题问题不要硬拆。
                4. 子 query 最多 4 个，必须语义完整、互不重复，并且每个都能独立用于检索。
                5. expandedKeywords 最多 8 个，只放真正能提升关键词命中的词。
                """);

        builder.append("会话摘要：").append(memory.summary()).append("\n最近对话：\n");
        for (ChatTurn turn : memory.recentTurns()) {
            builder.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }
        builder.append("当前问题：").append(message);
        return builder.toString();
    }

    // 限制 LLM 输出规模，避免过度拆分拖慢检索。
    private QueryRewriteResult normalizeResult(String message,
                                               QueryRewriteResult result,
                                               QueryIntentAnalyzer.QueryIntentAnalysis analysis) {
        List<String> subQueries = limit(result.subQueries(), properties.getRag().getMaxRewriteQueries());
        if (!analysis.multiIntentCandidate() && subQueries.size() > 1) {
            subQueries = List.of(result.rewrittenQuery());
        }
        List<String> keywords = limit(result.expandedKeywords(), properties.getRag().getMaxExpandedKeywords());
        QueryRewriteResult normalized = new QueryRewriteResult(result.rewrittenQuery(), subQueries, keywords);
        if (normalized.semanticQueries(message).isEmpty()) {
            return fallback(message);
        }
        return normalized;
    }

    // 重写失败时回退原问题，保证主链路不中断。
    private QueryRewriteResult fallback(String message) {
        return new QueryRewriteResult(message, List.of(message), List.of());
    }

    // 截断列表长度，控制多 query 检索成本。
    private List<String> limit(List<String> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        return values.subList(0, limit);
    }

    // 从模型响应中提取 JSON 对象。
    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
