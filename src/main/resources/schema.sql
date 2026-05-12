-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlResolveForFile

CREATE TABLE IF NOT EXISTS kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120),
  file_size BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  chunk_count INT NOT NULL DEFAULT 0,
  error_message TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  vector_id VARCHAR(120) NOT NULL,
  chunk_index INT NOT NULL,
  title VARCHAR(500),
  section_path VARCHAR(1000),
  content MEDIUMTEXT NOT NULL,
  token_estimate INT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_kb_chunk_document_id (document_id),
  UNIQUE KEY uk_kb_chunk_vector_id (vector_id)
);

CREATE TABLE IF NOT EXISTS long_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vector_id VARCHAR(120) NOT NULL,
  user_id VARCHAR(120) NOT NULL,
  title VARCHAR(255),
  content TEXT NOT NULL,
  tags VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_long_memory_user_id (user_id),
  UNIQUE KEY uk_long_memory_vector_id (vector_id)
);
