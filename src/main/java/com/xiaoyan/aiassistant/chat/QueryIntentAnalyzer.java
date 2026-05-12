package com.xiaoyan.aiassistant.chat;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class QueryIntentAnalyzer {
    private static final int MIN_COMPLEX_QUERY_LENGTH = 16;
    private static final int MIN_COMPLEX_TERM_COUNT = 6;
    private static final int LONG_QUERY_TERM_COUNT = 12;
    private static final int MULTI_INTENT_SCORE_THRESHOLD = 3;

    private static final List<String> COORDINATE_WORDS = List.of("和", "以及", "并且", "同时", "另外", "还有", "顺便", "再帮我");
    private static final List<String> QUESTION_WORDS = List.of("什么", "怎么", "如何", "为什么", "是否", "能否", "怎么办", "多少", "哪些");
    private static final List<String> ACTION_WORDS = List.of("启动", "部署", "配置", "安装", "查询", "删除", "新增", "修改", "排查", "解决", "生成", "总结", "对比");
    private static final List<String> SINGLE_INTENT_PATTERNS = List.of("区别", "差异", "对比", "关系", "优缺点");

    // 规则层只做低成本筛选，最终是否拆分仍交给 LLM 精判。
    public QueryIntentAnalysis analyze(String query) {
        String normalized = normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return new QueryIntentAnalysis(false, 0, 0, 0, "空问题不进入多意图判别");
        }

        List<Term> terms = HanLP.segment(normalized);
        int termCount = terms.size();
        int verbCount = countVerbTerms(terms);

        // 短问题通常是单意图，直接跳过 LLM 判别以节省成本。
        if (normalized.length() < MIN_COMPLEX_QUERY_LENGTH && termCount < MIN_COMPLEX_TERM_COUNT) {
            return new QueryIntentAnalysis(false, 0, termCount, verbCount, "短问题通常是单意图，直接跳过 LLM 多意图判别");
        }

        int score = 0;
        score += countHits(normalized, COORDINATE_WORDS);
        score += countQuestionMarks(normalized);
        score += Math.max(0, countHits(normalized, QUESTION_WORDS) - 1);
        score += Math.max(0, verbCount - 1);
        score += Math.max(0, countHits(normalized, ACTION_WORDS) - 1);
        if (containsSentenceSeparator(normalized)) {
            score += 1;
        }
        if (termCount >= LONG_QUERY_TERM_COUNT) {
            score += 1;
        }
        if (looksLikeSingleComparison(normalized)) {
            score -= 4;
        }

        boolean candidate = score >= MULTI_INTENT_SCORE_THRESHOLD;
        String reason = candidate ? "规则判断可能包含多个独立意图，需要交给 LLM 精判" : "规则判断更像单意图问题";
        return new QueryIntentAnalysis(candidate, score, termCount, verbCount, reason);
    }

    // 统一清理 query 的空白字符。
    private String normalize(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ");
    }

    // 统计规则词命中次数，用于快速估计复杂度。
    private int countHits(String query, List<String> words) {
        int count = 0;
        for (String word : words) {
            if (query.contains(word)) {
                count++;
            }
        }
        return count;
    }

    // HanLP 词性以 v 开头时按动词统计。
    private int countVerbTerms(List<Term> terms) {
        int count = 0;
        for (Term term : terms) {
            if (term.nature != null && term.nature.toString().startsWith("v")) {
                count++;
            }
        }
        return count;
    }

    // 多个问号通常表示连续提问。
    private int countQuestionMarks(String query) {
        int count = 0;
        for (char ch : query.toCharArray()) {
            if (ch == '?' || ch == '？') {
                count++;
            }
        }
        return count;
    }

    // 分句符号是弱信号，只给少量加分。
    private boolean containsSentenceSeparator(String query) {
        return query.contains("，") || query.contains(",") || query.contains("；") || query.contains(";") || query.contains("。");
    }

    // “A 和 B 的区别”这类对比问题通常不需要拆成两个 query。
    private boolean looksLikeSingleComparison(String query) {
        boolean hasCoordinate = query.contains("和") || query.contains("与") || query.contains("以及");
        boolean hasComparisonPattern = SINGLE_INTENT_PATTERNS.stream().anyMatch(query::contains);
        return hasCoordinate && hasComparisonPattern;
    }

    // 保存规则分析结果，供 Query 重写 Prompt 和测试断言使用。
    public record QueryIntentAnalysis(boolean multiIntentCandidate, int score, int termCount, int verbCount, String reason) {
    }
}
