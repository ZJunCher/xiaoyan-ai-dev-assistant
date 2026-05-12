package com.xiaoyan.aiassistant.chat;

// 聊天请求体，包含用户、会话和本轮问题。
public record ChatRequest(String userId, String sessionId, String message) {
}

