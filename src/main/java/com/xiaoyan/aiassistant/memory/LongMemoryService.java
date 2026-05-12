package com.xiaoyan.aiassistant.memory;

import com.xiaoyan.aiassistant.retrieval.VectorNamespace;
import com.xiaoyan.aiassistant.retrieval.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LongMemoryService {
    private static final String DEFAULT_USER_ID = "default";

    private final LongMemoryMapper mapper;
    private final VectorStoreService vectorStoreService;

    // 新增一条用户主动录入的长期记忆。
    @Transactional
    public LongMemory add(LongMemoryRequest request) {
        if (!StringUtils.hasText(request.content())) {
            throw new IllegalArgumentException("长期记忆内容不能为空");
        }

        String userId = normalizeUserId(request.userId());
        LocalDateTime now = LocalDateTime.now();
        LongMemory memory = new LongMemory(
                null,
                "memory-" + UUID.randomUUID(),
                userId,
                request.title(),
                request.content(),
                request.tags(),
                now,
                now
        );
        mapper.insert(memory);

        vectorStoreService.add(VectorNamespace.MEMORY, memory.getVectorId(), memory.getContent(), Map.of(
                "memoryId", String.valueOf(memory.getId()),
                "userId", memory.getUserId(),
                "title", nullToEmpty(memory.getTitle()),
                "source", "long_memory",
                "tags", nullToEmpty(memory.getTags())
        ));
        return memory;
    }

    // 查询指定用户的长期记忆列表。
    public List<LongMemory> list(String userId) {
        return mapper.findByUserId(normalizeUserId(userId));
    }

    // 空 userId 统一归入 default。
    private String normalizeUserId(String userId) {
        return StringUtils.hasText(userId) ? userId.trim() : DEFAULT_USER_ID;
    }

    // 向量元数据不接受 null，统一转为空字符串。
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
