# TimeForest

TimeForest 是一个集成了大语言模型会话（RAG）、个人博客与网址导航的全栈 Web 应用。后端基于 Spring Boot 与 Spring AI 构建，前端基于 Vue 3 开发。

## 功能特性

- **AI 对话与知识检索 (RAG)**：基于 Spring AI 与 DeepSeek API 实现流式输出；使用本地 ONNX 模型进行文本向量化，结合 Redis 向量库完成知识库检索与多角色上下文管理。
- **博客系统**：支持 Markdown 编辑与实时预览、草稿自动保存；支持文章发布、浏览统计、点赞、收藏与评论。
- **网址导航**：提供网站导航收藏功能，支持输入链接后由模型自动提取并补全网站名称、Favicon 与简介。
- **认证与权限**：采用 Spring Security 与 JWT 实现无状态认证，前端基于 Vue Router 守卫实现访问控制与失效重定向。
- **主题适配**：前端支持暗黑模式、浅色模式与系统自适应切换。

## 技术栈

| 模块 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **后端核心** | Spring Boot 3.5, Java 17 | 业务服务与 RESTful API |
| **模型集成** | Spring AI, DeepSeek API | 对话生成与文本处理 |
| **向量检索** | Redis 7 (RedisSearch), ONNX | 本地嵌入模型与向量存储 |
| **数据持久化** | MySQL 8.0, Spring Data JPA | 关系型业务数据存储 |
| **安全认证** | Spring Security, JJWT | JWT 签发与接口鉴权 |
| **前端架构** | Vue 3, Vite, Pinia, Vue Router | 前端单页应用 |

## 快速开始

### 环境依赖

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7+ (需包含 RedisSearch 模块)
- DeepSeek API Key

### 本地运行

#### 后端 (ai-agent-core)

1. 配置环境变量或在 `ai-agent-core/src/main/resources/application-dev.properties` 中指定密钥：
   ```properties
   DEEPSEEK_API_KEY=your_deepseek_api_key
   ```
2. 启动服务：
   ```bash
   cd ai-agent-core
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   后端默认监听 `8080` 端口。

#### 前端 (Gris)

```bash
cd Gris
npm install
npm run dev
```
前端默认监听 `5173` 端口。

## 生产部署

项目根目录下提供了 `docker-compose.yml`，适用于容器化部署：

1. 复制环境变量模板并填入对应配置：
   ```bash
   cp .env.example .env
   ```
2. 启动容器：
   ```bash
   docker-compose up -d --build
   ```

## 项目结构

```text
├── Gris/                      # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── components/        # 公共组件（输入器、消息列表等）
│   │   ├── views/             # 视图页面（对话、博客、导航、个人中心）
│   │   ├── router/            # 路由与守卫
│   │   └── stores/            # Pinia 状态管理
│   └── vite.config.js
├── ai-agent-core/             # 后端项目 (Spring Boot)
│   ├── src/main/java/         # 控制器、服务、实体与安全过滤
│   └── src/main/resources/    # 配置文件与 ONNX 模型资源
└── docker-compose.yml         # 生产部署配置
```

## 开源协议

本项目基于 MIT License 协议开源。
