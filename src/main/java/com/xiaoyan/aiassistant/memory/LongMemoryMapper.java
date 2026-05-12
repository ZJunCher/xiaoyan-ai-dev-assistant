package com.xiaoyan.aiassistant.memory;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@SuppressWarnings("SqlResolve")
public interface LongMemoryMapper {

    // 插入用户主动录入的长期记忆，user_id 用于做到用户级隔离。
    @Insert("""
            insert into long_memory(vector_id, user_id, title, content, tags, created_at, updated_at)
            values(#{vectorId}, #{userId}, #{title}, #{content}, #{tags}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LongMemory memory);

    // 查询指定用户的长期记忆，避免不同用户之间互相召回。
    @Select("""
            select id, vector_id, user_id, title, content, tags, created_at, updated_at
            from long_memory
            where user_id = #{userId}
            order by created_at desc
            """)
    List<LongMemory> findByUserId(@Param("userId") String userId);
}
