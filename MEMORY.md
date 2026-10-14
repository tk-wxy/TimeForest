# TimeForest — 项目记忆索引

> 最后更新：2026-06-04（部署适配）

> 
> **核心准则**：最小侵入式修改，不推倒重构，不私自引入新依赖。

---

## 一、项目概览

| 模块 | 技术栈 | 端口 | 说明 |
|------|--------|------|------|
| **Gris** | Vue 3 + Vite + Pinia + Vue Router | 80 | 主前端入口，AI 对话界面 |
| **ai-agent-core** | Spring Boot 3.5.13 + Spring AI 1.0.1 + JPA + MySQL | 8080 | 后端核心服务 |
| **ai-agent-frontend** | (待探索) | — | 另一个前端模块 |

### 基础设施（docker-compose）

- **MySQL 8.0** — 业务数据（用户、角色、会话、消息）
- **Redis 7** — RAG 向量存储 + 缓存

---

## 二、Gris 前端（Vue 3）

### 核心文件结构

```
Gris/src/
├── App.vue                          # 根组件
├── main.js                          # 入口：Pinia + Router + 全局样式
├── style.css                        # 全局主题变量（--tf-*）
├── router/index.js                  # 路由：/ /login /register /chat /profile /blogs /blog/:id /blog/editor/:id
├── stores/
│   ├── auth.js                      # 认证状态（token, username, login/register/logout）
│   └── roles.js                     # 角色管理
├── utils/
│   ├── api.js                       # Axios 实例 + 拦截器（JWT 注入、401 拦截）
│   └── theme.js                     # 主题切换（system/dark/light）
├── components/
│   ├── ChatMessageList.vue          # ★ 消息列表子组件（气泡、loading、流式、返回底部按钮）
│   ├── ChatComposer.vue             # ★ 输入区子组件（textarea 自适应、v-model）
│   ├── FrostCard.vue                # 毛玻璃卡片
│   ├── RoleCreateModal.vue          # 角色创建弹窗
│   └── SessionSidebar.vue           # 会话侧边栏（列表、新建、删除）
└── views/
    ├── HomeView.vue                 # 首页/登录后入口
    ├── LoginView.vue & RegisterView.vue
    ├── ProfileView.vue              # 个人主页
    ├── ChatView.vue                 # ★ AI 对话主容器（~390行，含智能滚动+Toast错误提示）
    ├── BlogListView.vue             # ★ 博客列表页（搜索/分页/卡片网格）
    ├── BlogDetailView.vue           # ★ 文章详情页（Markdown/点赞/收藏/评论）
    └── BlogEditorView.vue           # ★ 文章编辑器（分栏/预览/自动保存/发布弹窗）
```

### ChatView 架构（2026-06-01 重构）

- **父组件 ChatView**：管理状态（messages、inputText、sending、流式数据）、会话逻辑、SSE 通信、智能滚动控制
- **子组件 ChatMessageList**：仅负责渲染消息列表（空状态/气泡/loading/流式/返回底部按钮），纯展示组件
- **子组件 ChatComposer**：封装 textarea + 发送按钮，通过 `v-model` 双向绑定，`defineExpose` 暴露 `focus()`
- **Toast 错误提示**：`showToast()` 在屏幕底部居中弹出，4.5 秒自动消失

### 智能滚动机制

- `isAutoScrolling` 布尔状态控制自动跟随
- `@scroll.passive="onChatScroll"` 监听手动上滑 → 打断自动跟随
- 打断后右下角显示 `scroll-bottom-btn` 悬浮按钮（由 ChatMessageList 渲染），点击恢复跟随
- 发送新消息时强制重置为跟随状态
- `scrollToBottom(force=true)` 使用 `instant` 行为避免历史加载时的视觉抖动

### 依赖

| 依赖 | 用途 |
|------|------|
| vue 3.5 | 框架 |
| pinia 3 | 状态管理 |
| vue-router 5 | 路由 |
| axios 1.16 | HTTP 请求 |
| vite 8 | 构建工具 |
| marked (CDN) | Markdown 渲染 |
| highlight.js (CDN) | 代码高亮 |
| Font Awesome 6 (CDN) | 图标 |

---

## 三、ai-agent-core 后端（Java / Spring Boot）

### 核心目录

```
com.timeforest.ai_agent_core/
├── AiAgentCoreApplication.java      # 启动入口
├── AiController.java                # AI 对话 SSE 接口(/ask)
├── config/                          # Spring AI/Redis/安全等配置
├── controller/
│   ├── AuthController.java          # 登录/注册
│   ├── BlogController.java          # ★ Blog REST API（12 个端点）
│   ├── RoleController.java          # AI 角色管理
│   ├── SessionController.java       # 会话管理
│   └── UserController.java          # 用户信息
├── dto/                             # 数据传输对象（含 Blog 系列 DTO）
├── entity/                          # JPA 实体（User, Role, BlogPost, BlogComment, etc.）
├── repository/                      # Spring Data JPA 仓库
├── security/                        # JWT 生成/验证、Security 过滤器
└── service/
    ├── AuthService.java
    ├── BlogService.java             # ★ Blog 核心业务逻辑
    ├── RagAgentService.java
    ├── RoleService.java
    └── SessionService.java
```

### 关键依赖

| 依赖 | 用途 |
|------|------|
| spring-ai-starter-model-openai | DeepSeek/OpenAI 聊天模型 |
| spring-ai-starter-model-transformers | 本地 ONNX Embedding |
| spring-ai-starter-vector-store-redis | Redis 向量库（RAG） |
| spring-ai-autoconfigure-model-chat-memory-repository-jdbc | JDBC 持久化聊天记忆 |
| spring-boot-starter-data-jpa + mysql-connector-j | JPA + MySQL |
| spring-boot-starter-security + jjwt 0.12 | 认证 + JWT |

---

## 四、环境变量

| 变量 | 用途 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `DB_PASSWORD` | MySQL root 密码（默认 timeforest123） |
| `JWT_SECRET` | JWT 签名密钥（需>=32字符） |
| `VITE_API_BASE` | 前端 API 基础路径（默认 /api） |

---

## 五、Blog 功能架构（2026-06-02 新增）

### 后端 API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/blogs` | 发布列表（分页 + 全文搜索） |
| GET | `/api/blogs/mine` | 我的草稿/已发布 |
| POST | `/api/blogs` | 创建空白草稿 |
| GET | `/api/blogs/{id}` | 阅读详情（自动浏览+1） |
| PUT | `/api/blogs/{id}` | 更新草稿（标题+正文） |
| PUT | `/api/blogs/{id}/publish` | 发布（副标题+封面/海报） |
| DELETE | `/api/blogs/{id}` | 删除文章 |
| POST | `/api/blogs/{id}/like` | 点赞/取消点赞 |
| POST | `/api/blogs/{id}/favorite` | 收藏/取消收藏 |
| GET | `/api/blogs/{id}/comments` | 评论列表 |
| POST | `/api/blogs/{id}/comments` | 发表评论 |
| DELETE | `/api/blogs/{id}/comments/{cid}` | 删除评论 |

### 前端页面

| 路由 | 组件 | 功能 |
|------|------|------|
| `/blogs` | BlogListView | 博客列表（卡片网格、搜索、分页加载更多、创建草稿入口） |
| `/blog/:id` | BlogDetailView | 文章详情（封面图/海报、Markdown 渲染、点赞/收藏、评论列表+发表） |
| `/blog/editor/:id` | BlogEditorView | 编辑器（左右分栏编辑/预览、3 秒自动保存、Ctrl+S 快捷键、发布弹窗设置副标题+封面/海报文字） |

### 发布流程

1. 点击"写博客" -> `POST /api/blogs` 创建草稿 -> 跳转编辑器
2. 编辑器：Markdown 原文 + 实时预览，自动保存
3. 点击"发布" -> 弹出 Modal 设置副标题（必填）、封面图片链接（可选，图床 URL）或海报文字（可选，文字渲染封面）
4. 确认发布：先 `PUT /api/blogs/{id}` 保存最新内容，再 `PUT /api/blogs/{id}/publish` 提交发布参数

---

## 六、网站导航功能架构（2026-06-03 新增）

### 后端 API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/sites` | 获取所有网站导航卡片 |
| POST | `/api/sites` | 添加网站（仅网址必填，其余字段 AI 检索自动填充） |
| DELETE | `/api/sites/{id}` | 删除网站（所有用户共用，任何人均可删除） |

### AI 自动检索流程

1. **本地默认**：从 URL 提取域名作为网站名，图标使用默认 globe 图标，简介为空
2. **AI 检索**：后端调用 ChatClient，让 AI 根据网址检索该网站的官方名称、favicon 图标 URL、一句话简介
3. **优先级**：AI 检索结果优先于本地默认；用户手动填写的字段优先于 AI 检索
4. **容错**：AI 检索失败时静默回退到本地默认填充

### 前端页面

| 路由 | 组件 | 功能 |
|------|------|------|
| `/sites` | SiteNavView | 网站导航墙（卡片网格、点击加号添加、删除二次确认） |

### 首页入口

- HomeView 中"林间笔记"按钮 → 跳转 `/sites`

## 八、已知问题 / 待优化

1. **Markdown / highlight.js 通过 CDN 引入**（`marked`/`hljs` 挂载在 `globalThis`），后续可改为 npm 包
2. **SessionSidebar** 内部逻辑较复杂（含角色检索、创建、分页），可进一步拆分

---

## 九、变更记录

| 日期 | 文件 | 变更摘要 |
|------|------|----------|
| 2026-06-01 | `Gris/src/views/ChatView.vue` | 新增智能流式滚动 |
| 2026-06-01 | `Gris/src/components/ChatMessageList.vue` | 新建：消息列表子组件 |
| 2026-06-01 | `Gris/src/components/ChatComposer.vue` | 新建：输入区子组件 |
| 2026-06-01 | `Gris/src/views/ChatView.vue` | 重构：子组件拆分 + Toast 错误提示 |
| 2026-06-02 | `ai-agent-core/.../BlogController.java` | 新建：Blog REST API（12 个端点） |
| 2026-06-02 | `Gris/src/views/BlogListView.vue` | 新建：博客列表页 |
| 2026-06-02 | `Gris/src/views/BlogDetailView.vue` | 新建：文章详情页 |
| 2026-06-02 | `Gris/src/views/BlogEditorView.vue` | 新建：文章编辑器 |
| 2026-06-02 | `Gris/src/router/index.js` | 新增 3 条 Blog 路由 |
| 2026-06-02 | `Gris/src/views/BlogListView.vue` | 顶栏新增"我的"按钮，跳转 `/profile` |
| 2026-06-02 | `Gris/src/views/BlogDetailView.vue` | 导航栏新增"我的"按钮，跳转 `/profile` |
| 2026-06-03 | `ai-agent-core/.../entity/SiteNav.java` | 新建：网站导航 JPA 实体 |
| 2026-06-03 | `ai-agent-core/.../repository/SiteNavRepository.java` | 新建：SiteNav 数据仓库 |
| 2026-06-03 | `ai-agent-core/.../dto/AddSiteRequest.java` | 新建：添加网站请求 DTO |
| 2026-06-03 | `ai-agent-core/.../dto/SiteCardResponse.java` | 新建：网站卡片响应 DTO |
| 2026-06-03 | `ai-agent-core/.../service/SiteNavService.java` | 新建：网站导航核心服务（含 AI 检索自动填充） |
| 2026-06-03 | `ai-agent-core/.../controller/SiteNavController.java` | 新建：Site Nav REST API（3 个端点） |
| 2026-06-03 | `Gris/src/views/SiteNavView.vue` | 新建：网站导航墙页面 |
| 2026-06-03 | `Gris/src/router/index.js` | 新增 `/sites` 路由 |
| 2026-06-03 | `Gris/src/views/HomeView.vue` | "林间笔记"按钮跳转至 `/sites` |
| 2026-06-04 | `Gris/src/views/ProfileView.vue` | 左上返回按钮改为 logo 叶子图标，点击跳转首页 |
| 2026-06-04 | `docker-compose.yml` | 适配已有 MySQL/Redis，仅保留 backend + frontend 两个服务 |
| 2026-06-04 | `ai-agent-core/src/main/resources/application-prod.properties` | 新建：生产环境配置（通过环境变量连接宿主机数据库） |
| 2026-06-04 | `.env.example` | 新建：环境变量模板文件 |
| 2026-06-04 | `Gris/src/views/SiteNavView.vue` | Bug 修复：弹窗 overlay `@click.self` → `@mousedown.self`，防止选中文本拖出窗口误关闭 |
| 2026-06-04 | `Gris/src/views/BlogEditorView.vue` | Bug 修复：发布弹窗封面图添加 `referrerpolicy="no-referrer"` + `@error` 降级 |
| 2026-06-04 | `Gris/src/views/BlogDetailView.vue` | Bug 修复：文章封面图添加 `referrerpolicy="no-referrer"` + `@error` 降级 |
| 2026-06-04 | `Gris/src/views/ProfileView.vue` | 林间记录标签页集成删除按钮（草稿/已发布/收藏均支持），调用 `DELETE /api/blogs/{id}` |
