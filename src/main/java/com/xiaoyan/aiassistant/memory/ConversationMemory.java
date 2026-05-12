package com.xiaoyan.aiassistant.memory;

import java.util.ArrayList;
import java.util.List;

public record ConversationMemory(String summary, List<ChatTurn> recentTurns) {
    public ConversationMemory {
        summary = summary == null ? "" : summary;
        recentTurns = recentTurns == null ? new ArrayList<>() : recentTurns;
    }
}

