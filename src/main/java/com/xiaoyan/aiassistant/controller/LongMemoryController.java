package com.xiaoyan.aiassistant.controller;

import com.xiaoyan.aiassistant.memory.LongMemory;
import com.xiaoyan.aiassistant.memory.LongMemoryRequest;
import com.xiaoyan.aiassistant.memory.LongMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 长期记忆录入和查询接口。
@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class LongMemoryController {
    private final LongMemoryService longMemoryService;

    // 主动录入一条用户长期记忆。
    @PostMapping
    public LongMemory add(@RequestBody LongMemoryRequest request) {
        return longMemoryService.add(request);
    }

    // 查询指定用户的长期记忆列表。
    @GetMapping
    public List<LongMemory> list(@RequestParam(required = false) String userId) {
        return longMemoryService.list(userId);
    }
}

