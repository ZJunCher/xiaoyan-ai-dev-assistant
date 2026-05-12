package com.xiaoyan.aiassistant.memory;

import org.springframework.stereotype.Component;

import java.util.List;

// 短期记忆的轻量 token 估算器。
@Component
public class MemoryTokenEstimator {

    // 估算摘要和最近对话的总 token。
    public int estimateMemoryTokens(String summary, List<ChatTurn> turns) {
        int total = estimateText(summary);
        for (ChatTurn turn : turns) {
            total += estimateTurn(turn);
        }
        return total;
    }

    // 单轮消息额外加入角色名和格式开销。
    public int estimateTurn(ChatTurn turn) {
        if (turn == null) {
            return 0;
        }
        return 6 + estimateText(turn.role()) + estimateText(turn.content());
    }

    // 中文按 1 token 估算，英文数字按约 4 字符 1 token。
    public int estimateText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int chineseChars = 0;
        int otherChars = 0;
        for (char ch : text.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (!Character.isWhitespace(ch)) {
                otherChars++;
            }
        }
        return chineseChars + (int) Math.ceil(otherChars / 4.0);
    }
}

