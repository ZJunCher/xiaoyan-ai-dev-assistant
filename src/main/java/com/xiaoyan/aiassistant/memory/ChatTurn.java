package com.xiaoyan.aiassistant.memory;

import java.time.LocalDateTime;

public record ChatTurn(String role, String content, LocalDateTime timestamp) {

    public ChatTurn {
    }

    public ChatTurn(String role, String content) {
        this(role, content, LocalDateTime.now());
    }
}

