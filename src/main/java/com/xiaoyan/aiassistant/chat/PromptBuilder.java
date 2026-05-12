package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.memory.ChatTurn;
import com.xiaoyan.aiassistant.memory.ConversationMemory;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    // 构建最终回答 Prompt，把记忆、长期偏好和知识库片段放到同一上下文中。
    public String build(String originalMessage,
                        QueryRewriteResult rewriteResult,
                        ConversationMemory memory,
                        List<RetrievalCandidate> longMemories,
                        List<RetrievalCandidate> knowledge) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                你是“智能效研AI研发助手”，服务于团队研发提效场景。
                请严格围绕团队项目资料、研发文档、开发规范、新人培训和常见问题回答。
                优先依据给定知识回答；如果资料不足，请明确说明知识库中没有足够依据，不要编造。
                回答要清晰、可执行、简洁，适合团队同学直接参考。
                """);
        appendMemory(prompt, memory);
        appendCandidates(prompt, "长期记忆", longMemories);
        appendCandidates(prompt, "知识库检索片段", knowledge);
        prompt.append("原始问题：").append(originalMessage).append("\n");
        prompt.append("检索改写：").append(rewriteResult.rewrittenQuery()).append("\n");
        if (!rewriteResult.subQueries().isEmpty()) {
            prompt.append("多意图拆分：").append(String.join("；", rewriteResult.subQueries())).append("\n");
        }
        if (!rewriteResult.expandedKeywords().isEmpty()) {
            prompt.append("关键词扩展：").append(String.join("，", rewriteResult.expandedKeywords())).append("\n");
        }
        prompt.append("请直接给出最终回答。");
        return prompt.toString();
    }

    // 短期记忆用于补全追问中的指代关系。
    private void appendMemory(StringBuilder prompt, ConversationMemory memory) {
        prompt.append("\n会话摘要：").append(memory.summary().isBlank() ? "无" : memory.summary()).append("\n");
        prompt.append("最近对话：\n");
        if (memory.recentTurns().isEmpty()) {
            prompt.append("无\n");
            return;
        }
        for (ChatTurn turn : memory.recentTurns()) {
            prompt.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }
    }

    // 检索片段按排序结果写入，避免模型忽略靠前的高相关内容。
    private void appendCandidates(StringBuilder prompt, String title, List<RetrievalCandidate> candidates) {
        prompt.append("\n").append(title).append("：\n");
        if (candidates == null || candidates.isEmpty()) {
            prompt.append("无\n");
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            RetrievalCandidate candidate = candidates.get(i);
            prompt.append("[").append(i + 1).append("] ");
            if (candidate.getTitle() != null && !candidate.getTitle().isBlank()) {
                prompt.append(candidate.getTitle()).append(" - ");
            }
            if (candidate.getSource() != null && !candidate.getSource().isBlank()) {
                prompt.append(candidate.getSource()).append("\n");
            }
            prompt.append(candidate.getContent()).append("\n");
        }
    }
}
