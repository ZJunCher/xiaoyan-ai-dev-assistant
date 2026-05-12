# 智能效研 AI 研发助手

智能效研 AI 研发助手（`xiaoyan-ai-dev-assistant`）是一个面向团队研发提效场景的 RAG 智能问答知识库系统。项目用于集中管理团队项目资料、研发文档、开发规范、新人培训材料和常见问题，帮助成员快速检索知识、理解项目背景、减少重复沟通。

系统基于 Spring Boot 和 LangChain4j 构建，支持多格式文档解析、结构感知分块、语义分块、向量检索、BM25 关键词检索、Query 重写、多轮会话记忆和长期记忆主动录入。

## 功能特性

- 文档知识入库：支持 PDF、Word、Markdown、TXT 等文档上传，基于 Apache Tika 提取文本。
- 智能分块策略：结合结构特征识别、策略路由、结构感知分块、语义分块和混合分段处理。
- RAG 混合检索：结合 Pinecone 向量检索与内存 BM25 关键词检索，并支持 RRF 融合、去重和重排序。
- 查询重写策略：结合短期记忆对用户问题进行上下文补全，支持多意图拆分和关键词扩展。
- 短期记忆：基于 Redis 保存会话上下文，采用 32K Token 阈值触发和 overlap 摘要压缩机制。
- 长期记忆：支持用户主动录入长期记忆，并按 userId 隔离召回。
- 流式问答：提供 SSE 风格的流式聊天接口，前端可实时展示生成结果。
- 轻量前端：内置聊天、文档上传、文档列表、删除文档和长期记忆录入页面。

## 技术栈

- 后端框架：Spring Boot 3.5
- 大模型开发框架：LangChain4j
- 大模型：Qwen-Plus
- 向量模型：text-embedding-v3
- 向量数据库：Pinecone
- 文档解析：Apache Tika
- 关键词检索：内存 BM25
- 中文分词：HanLP
- 缓存与短期记忆：Redis
- 数据存储：MySQL
- ORM：MyBatis
- 前端：Vue 3 CDN + 原生 HTML/CSS

## 系统流程

### 文档入库流程

1. 用户上传文档。
2. 后端使用 Apache Tika 提取正文文本。
3. 对文本进行清洗，去除重复空白和无效字符。
4. 分析标题密度、段落长度、格式标记密度等结构特征。
5. 自动选择结构分块、语义分块或混合分块策略。
6. 将文档和 chunk 元数据写入 MySQL。
7. 将 chunk 文本向量化后写入 Pinecone 的 `knowledge` namespace。
8. 重建内存 BM25 索引。

### 在线问答流程

1. 用户发送问题。
2. 根据 `userId + sessionId` 读取 Redis 中的短期记忆。
3. 使用 LLM 对问题进行 Query 重写、多意图拆分和关键词扩展。
4. 从 Pinecone 的 `memory` namespace 召回长期记忆。
5. 使用向量检索和 BM25 检索召回知识库片段。
6. 对候选结果进行融合、去重和重排序。
7. 构建 Prompt，融合原问题、重写结果、短期记忆、长期记忆和 RAG 片段。
8. 调用 Qwen-Plus 生成流式回答。
9. 将本轮问答写入 Redis，并在超过阈值时触发摘要压缩。

## 项目结构

```text
src/main/java/com/xiaoyan/aiassistant
├── chat          # 聊天、Prompt 构建、Query 重写与多意图分析
├── config        # 应用配置、向量库配置
├── controller    # REST API
├── document      # 文档解析、清洗、分块、入库
├── memory        # 短期记忆、长期记忆
└── retrieval     # BM25、向量检索、混合检索、去重、重排序
```

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis 6+
- Pinecone 账号和 index（可选，本地默认使用内存向量库）
- 阿里百炼 DashScope API Key

## 快速开始

### 1. 创建数据库

默认数据库名为 `team_knowledge`，可按需修改：

```sql
CREATE DATABASE team_knowledge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行初始化脚本：

```text
src/main/resources/schema.sql
```

如果是从旧版本升级长期记忆 userId 隔离能力，再执行：

```text
src/main/resources/schema-user-memory-migration.sql
```

### 2. 配置环境变量

可以参考 `.env.example`：

```bash
API_KEY=你的DashScopeApiKey

MYSQL_URL=jdbc:mysql://localhost:3307/team_knowledge?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的数据库密码

REDIS_HOST=localhost
REDIS_PORT=6379

PINECONE_ENABLED=false
PINECONE_API_KEY=
PINECONE_INDEX=team-knowledge
```

本地开发时，`PINECONE_ENABLED=false` 会使用内存向量库，便于启动和测试。生产或演示环境需要真实向量检索时，再开启 Pinecone。

### 3. 启动项目

```bash
mvn spring-boot:run
```

启动后访问：

```text
http://localhost:8080
```

### 4. 运行测试

```bash
mvn test
```

## 接口说明

### 文档接口

上传文档：

```http
POST /api/documents
Content-Type: multipart/form-data
```

参数：

```text
file: 文档文件
```

查询文档列表：

```http
GET /api/documents
```

删除文档：

```http
DELETE /api/documents/{id}
```

### 聊天接口

流式问答：

```http
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

请求示例：

```json
{
  "userId": "default",
  "sessionId": "session-001",
  "message": "团队 Java 开发规范里对异常处理有什么要求？"
}
```

### 长期记忆接口

新增长期记忆：

```http
POST /api/memories
Content-Type: application/json
```

请求示例：

```json
{
  "userId": "default",
  "title": "代码生成偏好",
  "content": "我主要使用 Java 开发，生成代码时优先使用 Java。",
  "tags": "preference,java"
}
```

查询长期记忆：

```http
GET /api/memories?userId=default
```

## 关键配置

主要配置位于 `src/main/resources/application.yml`。

```yaml
app:
  pinecone:
    enabled: false
    knowledge-namespace: knowledge
    memory-namespace: memory
  rag:
    vector-top-k: 8
    bm25-top-k: 8
    final-top-k: 5
    candidate-top-k: 30
    semantic-dedup-threshold: 0.9
  memory:
    max-token-budget: 32000
    recent-token-budget: 24000
    summarize-overlap-ratio: 0.5
    ttl-days: 7
```

说明：

- `max-token-budget`：短期记忆总预算，超过后触发摘要压缩。
- `recent-token-budget`：压缩后保留的最新原文对话预算。
- `summarize-overlap-ratio`：摘要时带入窗口内较早一部分内容，增强上下文衔接。
- `semantic-dedup-threshold`：语义去重阈值，用于过滤高度相似 chunk。
- `reranker.enabled`：是否启用外部 Reranker 服务，本地默认关闭。
