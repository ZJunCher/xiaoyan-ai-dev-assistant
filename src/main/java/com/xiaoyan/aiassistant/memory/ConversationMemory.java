package com.xiaoyan.aiassistant.memory;

import java.util.ArrayList;
import java.util.List;

// Redis 中保存的短期记忆结构。
public record ConversationMemory(String summary, List<ChatTurn> recentTurns) {
    // 构造时兜底空摘要和空对话列表。
    public ConversationMemory {
        summary = summary == null ? "" : summary;
        recentTurns = recentTurns == null ? new ArrayList<>() : recentTurns;
    }
}

