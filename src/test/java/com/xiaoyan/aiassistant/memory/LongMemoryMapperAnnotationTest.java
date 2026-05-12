package com.xiaoyan.aiassistant.memory;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// LongMemoryMapper 的 SQL 注解保护测试。
class LongMemoryMapperAnnotationTest {

    // 确保长期记忆 Mapper 保留 SQL 注解，保证 userId 隔离查询能被 MyBatis 注册。
    @Test
    void longMemoryMapperMethodsShouldKeepSqlAnnotations() throws NoSuchMethodException {
        assertThat(LongMemoryMapper.class.getMethod("insert", LongMemory.class).isAnnotationPresent(Insert.class)).isTrue();
        assertThat(LongMemoryMapper.class.getMethod("findByUserId", String.class).isAnnotationPresent(Select.class)).isTrue();
    }
}
