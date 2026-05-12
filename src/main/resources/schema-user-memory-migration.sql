-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlResolveForFile

-- 旧数据库升级到 userId 隔离版时执行。
-- 新库直接使用 schema.sql 建表；旧库需要补充 user_id，历史长期记忆统一归到 default 用户。
ALTER TABLE long_memory ADD COLUMN user_id VARCHAR(120) NOT NULL DEFAULT 'default' AFTER vector_id;
CREATE INDEX idx_long_memory_user_id ON long_memory(user_id);
