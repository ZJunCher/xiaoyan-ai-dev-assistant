package com.xiaoyan.aiassistant.memory;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoryTokenEstimator {

    public int estimateMemoryTokens(String summary, List<ChatTurn> turns) {
        int total = estimateText(summary);
        for (ChatTurn turn : turns) {
            total += estimateTurn(turn);
        }
        return total;
    }

    public int estimateTurn(ChatTurn turn) {
        if (turn == null) {
            return 0;
        }
        return 6 + estimateText(turn.role()) + estimateText(turn.content());
    }

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

