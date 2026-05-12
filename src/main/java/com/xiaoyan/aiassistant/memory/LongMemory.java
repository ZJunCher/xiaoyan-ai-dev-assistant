package com.xiaoyan.aiassistant.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 数据库中的长期记忆实体。
public class LongMemory {
    private Long id;
    private String vectorId;
    private String userId;
    private String title;
    private String content;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

