package com.xiaoyan.aiassistant.document;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
@SuppressWarnings("SqlResolve")
public interface DocumentMapper {

    // 插入文档元数据，id 由 MySQL 自增生成后回填到实体。
    @Insert("""
            insert into kb_document(file_name, content_type, file_size, status, chunk_count, error_message, created_at, updated_at)
            values(#{fileName}, #{contentType}, #{fileSize}, #{status}, #{chunkCount}, #{errorMessage}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDocument(KbDocument document);

    // 更新文档处理结果，上传成功或失败都会走这里统一落库。
    @Update("""
            update kb_document
            set status = #{status},
                chunk_count = #{chunkCount},
                error_message = #{errorMessage},
                updated_at = #{updatedAt}
            where id = #{id}
            """)
    void updateDocument(KbDocument document);

    // 按创建时间倒序展示文档列表，方便前端优先看到最新上传内容。
    @Select("""
            select id, file_name, content_type, file_size, status, chunk_count, error_message, created_at, updated_at
            from kb_document
            order by created_at desc
            """)
    List<KbDocument> findDocuments();

    // 根据文档 id 查询单个文档，删除或详情场景会用到。
    @Select("""
            select id, file_name, content_type, file_size, status, chunk_count, error_message, created_at, updated_at
            from kb_document
            where id = #{id}
            """)
    KbDocument findDocument(@Param("id") Long id);

    // 删除文档元数据；调用前会先删除 chunk 和向量数据。
    @Delete("""
            delete from kb_document
            where id = #{id}
            """)
    void deleteDocument(@Param("id") Long id);

    // 插入一个文档分块，vector_id 用于关联 Pinecone 中的向量记录。
    @Insert("""
            insert into kb_chunk(document_id, vector_id, chunk_index, title, section_path, content, token_estimate, created_at)
            values(#{documentId}, #{vectorId}, #{chunkIndex}, #{title}, #{sectionPath}, #{content}, #{tokenEstimate}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertChunk(KbChunk chunk);

    // 查询指定文档的全部分块，按 chunk_index 保持原文顺序。
    @Select("""
            select id, document_id, vector_id, chunk_index, title, section_path, content, token_estimate, created_at
            from kb_chunk
            where document_id = #{documentId}
            order by chunk_index
            """)
    List<KbChunk> findChunksByDocumentId(@Param("documentId") Long documentId);

    // 查询全部分块，用于应用启动或文档变更后重建内存 BM25 索引。
    @Select("""
            select id, document_id, vector_id, chunk_index, title, section_path, content, token_estimate, created_at
            from kb_chunk
            order by id
            """)
    List<KbChunk> findAllChunks();

    // 删除指定文档下的全部分块，避免文档删除后留下孤儿数据。
    @Delete("""
            delete from kb_chunk
            where document_id = #{documentId}
            """)
    void deleteChunksByDocumentId(@Param("documentId") Long documentId);
}
