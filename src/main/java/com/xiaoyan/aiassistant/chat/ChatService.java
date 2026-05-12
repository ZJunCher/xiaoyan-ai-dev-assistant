package com.xiaoyan.aiassistant.chat;

import com.xiaoyan.aiassistant.memory.ConversationMemory;
import com.xiaoyan.aiassistant.memory.ShortTermMemoryService;
import com.xiaoyan.aiassistant.retrieval.HybridRetrievalService;
import com.xiaoyan.aiassistant.retrieval.RetrievalCandidate;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;

// 负责在线问答主链路编排。
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String DEFAULT_USER_ID = "default";

    private final ShortTermMemoryService shortTermMemoryService;
    private final QueryRewriteService queryRewriteService;
    private final HybridRetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final StreamingChatModel streamingChatModel;

    // 在线问答主流程：记忆读取、Query 重写、RAG 检索、流式生成、记忆更新。
    public Flux<String> stream(ChatRequest request) {
        String userId = normalizeUserId(request.userId());
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : "default";
        String memorySessionId = userId + ":" + sessionId;
        String message = request.message();
        return Flux.create(sink -> {
            ConversationMemory memory = shortTermMemoryService.get(memorySessionId);
            QueryRewriteResult rewrite = queryRewriteService.rewrite(message, memory);
            List<String> semanticQueries = rewrite.semanticQueries(message);
            List<RetrievalCandidate> longMemories = retrievalService.retrieveLongMemory(userId, semanticQueries);
            List<RetrievalCandidate> knowledge = retrievalService.retrieveKnowledge(semanticQueries, rewrite.keywordText(message));
            String prompt = promptBuilder.build(message, rewrite, memory, longMemories, knowledge);
            StringBuilder answer = new StringBuilder();
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                // 收到流式片段后立即推给前端。
                @Override
                public void onPartialResponse(String partialResponse) {
                    answer.append(partialResponse);
                    sink.next(partialResponse);
                }

                // 完整回答结束后写入短期记忆。
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    shortTermMemoryService.append(memorySessionId, message, answer.toString());
                    sink.complete();
                }

                // 模型调用异常时交给响应流向前端暴露。
                @Override
                public void onError(Throwable error) {
                    sink.error(error);
                }
            });
        });
    }

    // 空用户统一归入 default，避免 Redis key 和长期记忆过滤出现空值。
    private String normalizeUserId(String userId) {
        return StringUtils.hasText(userId) ? userId.trim() : DEFAULT_USER_ID;
    }
}
