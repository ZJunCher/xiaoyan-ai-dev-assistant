package com.xiaoyan.aiassistant.memory;

import java.time.LocalDateTime;

// 单条对话消息，记录角色、内容和时间戳。
public record ChatTurn(String role, String content, LocalDateTime timestamp) {

    // 保留规范构造器，便于后续统一校验字段。
    public ChatTurn {
    }

    // 默认构造会自动补齐当前时间。
    public ChatTurn(String role, String content) {
        this(role, content, LocalDateTime.now());
    }
}

