package com.xiaoyan.aiassistant.memory;

// 长期记忆录入请求体。
public record LongMemoryRequest(String userId, String title, String content, String tags) {
}

