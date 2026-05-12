package com.xiaoyan.aiassistant.document;

import com.xiaoyan.aiassistant.retrieval.Bm25Service;
import com.xiaoyan.aiassistant.retrieval.VectorNamespace;
import com.xiaoyan.aiassistant.retrieval.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentMapper documentMapper;
    private final DocumentTextExtractor extractor;
    private final TextCleaner cleaner;
    private final DocumentChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final Bm25Service bm25Service;

    // 解析上传文件并写入知识库索引。
    @Transactional
    public DocumentUploadResponse upload(MultipartFile file) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        KbDocument document = new KbDocument(
                null,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                DocumentStatus.PROCESSING.name(),
                0,
                null,
                now,
                now
        );
        documentMapper.insertDocument(document);
        try {
            String text = cleaner.clean(extractor.extract(file));
            List<DocumentChunk> chunks = chunkingService.split(text);
            for (DocumentChunk chunk : chunks) {
                KbChunk kbChunk = toKbChunk(document, chunk);
                documentMapper.insertChunk(kbChunk);
                vectorStoreService.add(VectorNamespace.KNOWLEDGE, kbChunk.getVectorId(), kbChunk.getContent(), metadata(document, kbChunk));
            }
            document.setStatus(DocumentStatus.READY.name());
            document.setChunkCount(chunks.size());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateDocument(document);
            bm25Service.rebuild(documentMapper.findAllChunks());
            return new DocumentUploadResponse(document.getId(), document.getFileName(), document.getStatus(), chunks.size());
        } catch (RuntimeException | IOException ex) {
            document.setStatus(DocumentStatus.FAILED.name());
            document.setErrorMessage(ex.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateDocument(document);
            throw ex;
        }
    }

    // 查询文档元数据列表。
    public List<KbDocument> list() {
        return documentMapper.findDocuments();
    }

    // 删除文档及其 chunk、向量和 BM25 索引。
    @Transactional
    public void delete(Long documentId) {
        List<KbChunk> chunks = documentMapper.findChunksByDocumentId(documentId);
        vectorStoreService.remove(VectorNamespace.KNOWLEDGE, chunks.stream().map(KbChunk::getVectorId).toList());
        documentMapper.deleteChunksByDocumentId(documentId);
        documentMapper.deleteDocument(documentId);
        bm25Service.rebuild(documentMapper.findAllChunks());
    }

    // 将分块结果转换为数据库 chunk 实体。
    private KbChunk toKbChunk(KbDocument document, DocumentChunk chunk) {
        return new KbChunk(
                null,
                document.getId(),
                "doc-" + document.getId() + "-chunk-" + chunk.index() + "-" + UUID.randomUUID(),
                chunk.index(),
                chunk.title(),
                chunk.sectionPath(),
                chunk.content(),
                chunk.tokenEstimate(),
                LocalDateTime.now()
        );
    }

    // 构造写入向量库的元数据，数字统一转字符串以兼容 Pinecone。
    private Map<String, String> metadata(KbDocument document, KbChunk chunk) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("documentId", String.valueOf(document.getId()));
        metadata.put("chunkId", String.valueOf(chunk.getId()));
        metadata.put("title", nullToEmpty(chunk.getTitle()));
        metadata.put("source", nullToEmpty(document.getFileName()));
        metadata.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));
        return metadata;
    }

    // 将空值转为空字符串。
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
