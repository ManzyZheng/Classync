# 项目结构总览

## 完整目录结构

```
Classync/
├── README.md                          # 项目总体说明文档
├── QUICK_START.md                     # 快速启动指南
├── PROJECT_STRUCTURE.md               # 项目结构说明（本文件）
├── QUICK_REFERENCE.md                 # 快速参考卡片 (NEW)
├── COPY_CLASSROOM_GUIDE.md            # 复制课堂使用指南 (NEW)
├── COPY_CLASSROOM_TESTING.md          # 复制课堂测试指南 (NEW)
├── IMPLEMENTATION_SUMMARY.md          # 复制课堂实现总结 (NEW)
├── CHANGES.md                         # 变更清单 (NEW)
├── COMPLETION_REPORT.md               # 完成报告 (NEW)
├── .gitignore                         # Git 忽略配置
│
├── backend/                           # Spring Boot 后端
│   ├── pom.xml                       # Maven 配置文件
│   ├── README.md                     # 后端说明文档
│   └── src/
│       └── main/
│           ├── java/com/classync/
│           │   ├── ClassyncApplication.java           # 启动类
│           │   │
│           │   ├── config/                           # 配置类
│           │   │   ├── WebConfig.java               # Web 配置(CORS)
│           │   │   └── WebSocketConfig.java         # WebSocket 配置
│           │   │
│           │   ├── entity/                           # 实体类
│           │   │   ├── User.java                    # 用户实体
│           │   │   ├── Classroom.java               # 课堂实体
│           │   │   ├── Question.java                # 问题实体
│           │   │   ├── QuestionOption.java          # 问题选项实体
│           │   │   ├── Answer.java                  # 答案实体
│           │   │   └── Discussion.java              # 讨论实体
│           │   │
│           │   ├── repository/                       # 数据访问层
│           │   │   ├── UserRepository.java
│           │   │   ├── ClassroomRepository.java
│           │   │   ├── QuestionRepository.java
│           │   │   ├── QuestionOptionRepository.java
│           │   │   ├── AnswerRepository.java
│           │   │   └── DiscussionRepository.java
│           │   │
│           │   ├── service/                          # 业务逻辑层
│           │   │   ├── UserService.java
│           │   │   ├── ClassroomService.java
│           │   │   ├── QuestionService.java
│           │   │   ├── AnswerService.java
│           │   │   ├── DiscussionService.java
│           │   │   └── FileService.java
│           │   │
│           │   ├── controller/                       # 控制器层
│           │   │   ├── AuthController.java          # 认证控制器
│           │   │   ├── UserController.java
│           │   │   ├── ClassroomController.java
│           │   │   ├── QuestionController.java
│           │   │   ├── AnswerController.java
│           │   │   └── DiscussionController.java
│           │   │
│           │   ├── dto/                              # 数据传输对象
│           │   │   ├── LoginRequest.java
│           │   │   ├── ClassroomRequest.java
│           │   │   ├── QuestionRequest.java
│           │   │   ├── OptionRequest.java
│           │   │   ├── AnswerRequest.java
│           │   │   ├── DiscussionRequest.java
│           │   │   ├── QuestionWithOptionsDTO.java
│           │   │   ├── AnswerStatisticsDTO.java
│           │   │   └── DiscussionWithUserDTO.java
│           │   │
│           │   └── websocket/                        # WebSocket
│           │       ├── WebSocketMessage.java
│           │       └── WebSocketController.java
│           │
│           └── resources/
│               └── application.properties            # 应用配置
│
└── frontend/                          # Vue 3 前端
    ├── package.json                  # NPM 配置文件
    ├── vite.config.js                # Vite 配置
    ├── index.html                    # HTML 模板
    ├── README.md                     # 前端说明文档
    └── src/
        ├── main.js                   # 入口文件
        ├── App.vue                   # 根组件
        ├── style.css                 # 全局样式
        │
        ├── api/                      # API 接口
        │   └── index.js             # API 统一管理
        │
        ├── stores/                   # Pinia 状态管理
        │   ├── user.js              # 用户状态
        │   └── classroom.js         # 课堂状态
        │
        ├── router/                   # Vue Router
        │   └── index.js             # 路由配置
        │
        ├── utils/                    # 工具类
        │   └── websocket.js         # WebSocket 服务
        │
        ├── views/                    # 页面组件
        │   ├── Login.vue            # 登录页
        │   ├── Home.vue             # 主页(创建/加入课堂)
        │   ├── ClassroomHost.vue    # 课堂主持人视角
        │   └── ClassroomViewer.vue  # 课堂观众视角
        │
        └── components/               # 可复用组件
            ├── PdfViewer.vue        # PDF 主视图
            ├── PdfThumbnails.vue    # PDF 缩略图
            ├── InteractionPanel.vue # 互动面板容器
            ├── InteractionTab.vue   # 互动标签页
            ├── QuestionEditor.vue   # 问题编辑器
            └── DiscussionTab.vue    # 讨论区标签页
```

## 核心功能模块说明

### 后端模块

#### 1. 实体层 (Entity)
- **User**: 用户基本信息
- **Classroom**: 课堂信息(包含课堂码、PDF路径、当前页码)
- **Question**: 问题信息(类型、内容、开放/结束状态)
- **QuestionOption**: 选择题选项(内容、是否正确)
- **Answer**: 学生答案
- **Discussion**: 讨论/评论(支持回复和匿名)

#### 2. 服务层 (Service)
- **UserService**: 模拟登录逻辑
- **ClassroomService**: 课堂管理、课堂码生成、页码更新
- **QuestionService**: 问题 CRUD、开放/关闭、结束统计
- **AnswerService**: 答案提交、统计计算
- **DiscussionService**: 讨论发布、查询(含用户信息)
- **FileService**: PDF 文件上传

#### 3. WebSocket
- **连接管理**: 课堂订阅、断线重连
- **事件处理**:
  - `join_classroom`: 加入课堂
  - `page_change`: 翻页同步
  - `question_toggle`: 问题开关
  - `question_finish`: 结束统计
  - `answer_submit`: 答案提交
  - `discussion_new`: 新讨论

### 前端模块

#### 1. 页面 (Views)
- **Login.vue**: 模拟认证登录
- **Home.vue**: 
  - 加入课堂(输入课堂码)
  - 创建课堂(上传PDF、设置时间)
  - 我的课堂列表
- **ClassroomHost.vue**: 主持人视角
  - 三栏布局
  - 控制翻页
  - 管理互动问题
- **ClassroomViewer.vue**: 观众视角
  - 跟随主持人翻页
  - 参与互动答题
  - 发布讨论

#### 2. 组件 (Components)
- **PdfViewer.vue**: PDF.js 渲染主视图
- **PdfThumbnails.vue**: 缩略图导航(标记主持人当前页)
- **InteractionPanel.vue**: 互动/讨论区切换容器
- **InteractionTab.vue**: 
  - 主持人: 问题列表、创建/编辑/删除、实时统计
  - 观众: 答题界面、结果展示、词云(问答题)
- **QuestionEditor.vue**: 问题创建/编辑弹窗
- **DiscussionTab.vue**: 讨论区(评论/回复/匿名)

#### 3. 状态管理 (Pinia)
- **user**: 当前用户、身份(主持人/观众)
- **classroom**: 当前课堂、当前页码

#### 4. 工具 (Utils)
- **websocket.js**: STOMP WebSocket 封装

## 数据流设计

### 1. 登录流程
```
用户输入 → 模拟认证弹窗 → POST /api/auth/login → 保存用户信息 → 跳转主页
```

### 2. 创建课堂流程
```
填写信息 → POST /api/classrooms → 上传PDF → 生成课堂码 → 显示在列表
```

### 3. 加入课堂流程
```
输入课堂码 → GET /api/classrooms/code/{code} → 连接WebSocket → 进入观众视角
```

### 4. 翻页同步流程
```
主持人翻页 → WebSocket.send(page_change) → 服务器广播 → 观众端自动翻页
```

### 5. 互动问答流程
```
主持人开放问题 → WebSocket通知 → 观众看到问题 → 提交答案 → 实时统计 → 主持人结束 → 展示结果
```

### 6. 讨论流程
```
发布评论/回复 → POST /api/discussions → WebSocket广播 → 所有人实时看到
```

## 关键技术点

### 后端
1. **H2 内存数据库**: 快速启动、自动建表
2. **JPA 关联查询**: Repository 层灵活查询
3. **STOMP WebSocket**: 基于主题的消息广播
4. **文件上传**: MultipartFile 处理 PDF
5. **CORS 配置**: 跨域支持前后端分离

### 前端
1. **PDF.js**: Canvas 渲染 PDF 页面
2. **Pinia**: 轻量级状态管理
3. **STOMP.js**: WebSocket 客户端
4. **动态路由**: 主持人/观众不同视角
5. **组件化设计**: 可复用、职责清晰

## 扩展建议

1. **真实认证**: 接入 JWT/OAuth2
2. **数据持久化**: H2 → MySQL/PostgreSQL
3. **词云功能**: 集成 wordcloud 库完善问答题可视化
4. **文件管理**: 支持 PDF 预览、删除、替换
5. **权限控制**: 细粒度权限(如禁言、踢出)
6. **历史记录**: 答题记录、讨论历史查询
7. **导出功能**: 答题统计导出 Excel
8. **多媒体**: 支持图片、视频

