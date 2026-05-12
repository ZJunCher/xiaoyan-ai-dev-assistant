package com.xiaoyan.aiassistant.controller;

import com.xiaoyan.aiassistant.document.DocumentService;
import com.xiaoyan.aiassistant.document.DocumentUploadResponse;
import com.xiaoyan.aiassistant.document.KbDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping
    public DocumentUploadResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        return documentService.upload(file);
    }

    @GetMapping
    public List<KbDocument> list() {
        return documentService.list();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }
}

