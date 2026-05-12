package com.xiaoyan.aiassistant.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.aiassistant.config.AppProperties;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortTermMemoryService {
    private static final String KEY_PREFIX = "xiaoyan:chat:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final AppProperties properties;
    private final MemoryTokenEstimator tokenEstimator;

    // 从 Redis 读取指定会话的短期记忆。
    public ConversationMemory get(String sessionId) {
        String json = redisTemplate.opsForValue().get(key(sessionId));
        if (json == null) {
            return new ConversationMemory("", new ArrayList<>());
        }
        try {
            return objectMapper.readValue(json, ConversationMemory.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse conversation memory for session {}", sessionId, ex);
            return new ConversationMemory("", new ArrayList<>());
        }
    }

    // 追加一轮用户问题和模型回答。
    public void append(String sessionId, String userMessage, String assistantMessage) {
        ConversationMemory current = get(sessionId);
        List<ChatTurn> turns = new ArrayList<>(current.recentTurns());
        turns.add(new ChatTurn("user", userMessage));
        turns.add(new ChatTurn("assistant", assistantMessage));

        ConversationMemory updated = compressIfNeeded(current.summary(), turns);
        save(sessionId, updated);
    }

    // 只有超过 token 阈值时才触发摘要，减少不必要的大模型调用。
    ConversationMemory compressIfNeeded(String currentSummary, List<ChatTurn> turns) {
        int totalTokens = tokenEstimator.estimateMemoryTokens(currentSummary, turns);
        if (totalTokens <= properties.getMemory().getMaxTokenBudget()) {
            return new ConversationMemory(currentSummary, turns);
        }

        List<ChatTurn> recentTurns = selectRecentTurns(turns);
        int recentStart = turns.size() - recentTurns.size();
        List<ChatTurn> evictedTurns = new ArrayList<>(turns.subList(0, Math.max(0, recentStart)));
        List<ChatTurn> overlapTurns = selectOverlapTurns(recentTurns);

        List<ChatTurn> turnsToSummarize = new ArrayList<>(evictedTurns);
        turnsToSummarize.addAll(overlapTurns);

        String newSummary = summarize(currentSummary, turnsToSummarize);
        return new ConversationMemory(newSummary, recentTurns);
    }

    // 从后往前保留最新原文窗口，确保刚发生的追问不被摘要掉。
    private List<ChatTurn> selectRecentTurns(List<ChatTurn> turns) {
        int budget = properties.getMemory().getRecentTokenBudget();
        List<ChatTurn> selected = new ArrayList<>();
        int tokens = 0;
        for (int i = turns.size() - 1; i >= 0; i--) {
            ChatTurn turn = turns.get(i);
            int turnTokens = tokenEstimator.estimateTurn(turn);
            if (!selected.isEmpty() && tokens + turnTokens > budget) {
                break;
            }
            selected.add(turn);
            tokens += turnTokens;
        }
        Collections.reverse(selected);

        int minTurns = Math.min(2, turns.size());
        if (selected.size() < minTurns) {
            return new ArrayList<>(turns.subList(turns.size() - minTurns, turns.size()));
        }
        return selected;
    }

    // overlap 只进入摘要输入，不重复保存到摘要之外。
    private List<ChatTurn> selectOverlapTurns(List<ChatTurn> recentTurns) {
        if (recentTurns.isEmpty()) {
            return List.of();
        }
        int overlapCount = (int) Math.floor(recentTurns.size() * properties.getMemory().getSummarizeOverlapRatio());
        if (overlapCount <= 0) {
            return List.of();
        }
        return new ArrayList<>(recentTurns.subList(0, overlapCount));
    }

    // 调用大模型生成新的会话摘要，失败时保留旧摘要。
    private String summarize(String previousSummary, List<ChatTurn> turnsToSummarize) {
        if (turnsToSummarize.isEmpty()) {
            return previousSummary;
        }
        String prompt = buildSummaryPrompt(previousSummary, turnsToSummarize);
        try {
            return chatModel.chat(prompt);
        } catch (RuntimeException ex) {
            log.warn("Failed to summarize memory; falling back to previous summary: {}", ex.getMessage());
            return previousSummary;
        }
    }

    // 摘要 Prompt 控制长度，只保留用户需求、操作和关键结果。
    String buildSummaryPrompt(String previousSummary, List<ChatTurn> turnsToSummarize) {
        AppProperties.Memory memory = properties.getMemory();
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于下面的对话生成短期记忆摘要。\n");
        prompt.append("要求：\n");
        prompt.append("1. 摘要控制在")
                .append(memory.getSummaryTargetMinChars())
                .append("-")
                .append(memory.getSummaryTargetMaxChars())
                .append("字，简洁明了。\n");
        prompt.append("2. 只保留核心信息：用户的需求、已执行的操作、关键结果。\n");
        prompt.append("3. 不要遗漏重要参数，例如项目名、接口名、数据库表名、配置项、错误信息等。\n");
        prompt.append("4. 语言口语化，符合对话上下文逻辑。\n");
        prompt.append("5. 不要添加额外内容，只基于提供的对话生成摘要。\n");
        if (previousSummary != null && !previousSummary.isBlank()) {
            prompt.append("\n已有摘要：\n").append(previousSummary).append("\n");
        }
        prompt.append("\n需要压缩的对话：\n");
        for (ChatTurn turn : turnsToSummarize) {
            prompt.append(roleName(turn.role())).append("：").append(turn.content()).append("\n");
        }
        return prompt.toString();
    }

    // 将内部角色名转换成中文显示名。
    private String roleName(String role) {
        if ("assistant".equals(role)) {
            return "助手";
        }
        if ("user".equals(role)) {
            return "用户";
        }
        return role;
    }

    // 将短期记忆写回 Redis，并设置 TTL 避免长期堆积。
    private void save(String sessionId, ConversationMemory memory) {
        try {
            redisTemplate.opsForValue().set(
                    key(sessionId),
                    objectMapper.writeValueAsString(memory),
                    Duration.ofDays(properties.getMemory().getTtlDays()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize conversation memory", ex);
        }
    }

    // 生成 Redis 短期记忆 key。
    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
