package com.xiaoyan.aiassistant.document;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// DocumentMapper 的 SQL 注解保护测试。
class DocumentMapperAnnotationTest {

    // 确保 Mapper 方法都有 SQL 注解，避免运行时才发现 MyBatis statement 不存在。
    @Test
    void documentMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(DocumentMapper.class.getMethod("insertDocument", KbDocument.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("updateDocument", KbDocument.class).isAnnotationPresent(Update.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("findDocuments").isAnnotationPresent(Select.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("findDocument", Long.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("deleteDocument", Long.class).isAnnotationPresent(Delete.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("insertChunk", KbChunk.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("findChunksByDocumentId", Long.class).isAnnotationPresent(Select.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("findAllChunks").isAnnotationPresent(Select.class)).isTrue();
        assertThat(DocumentMapper.class.getMethod("deleteChunksByDocumentId", Long.class).isAnnotationPresent(Delete.class)).isTrue();
    }
}
