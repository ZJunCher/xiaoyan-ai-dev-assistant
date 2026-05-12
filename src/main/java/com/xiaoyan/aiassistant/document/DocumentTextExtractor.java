package com.xiaoyan.aiassistant.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentTextExtractor {
    private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

    public String extract(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Document document = parser.parse(inputStream);
            return document.text();
        }
    }
}

