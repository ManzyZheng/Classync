# Classync - 课堂互动系统

一个完整的前后端分离课堂互动系统，用于课堂实时 PPT 展示、互动问答与讨论。

## 技术栈

### 前端
- Vue 3
- Vite
- Vue Router
- Pinia
- PDF.js

### 后端
- Spring Boot
- Spring Data JPA
- Spring WebSocket
- H2 数据库（内嵌）

## 项目结构

```
Classync/
├── backend/          # Spring Boot 后端
│   ├── src/
│   └── pom.xml
├── frontend/         # Vue 3 前端
│   ├── src/
│   └── package.json
└── README.md
```

## 快速启动

### 后端启动

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将运行在 http://localhost:8080

- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:classync`
  - Username: `sa`
  - Password: (空)

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端将运行在 http://localhost:5173

## 功能模块

### 1. 登录模块
- 模拟身份认证
- 支持账号和姓名输入

### 2. 主界面
- 加入课堂（输入课堂码）
- 创建课堂（上传 PDF、设置时间）
- 查看我创建的课堂列表

### 3. 课堂页面
- 三栏布局：缩略图 + PDF 展示 + 互动区
- 主持人视角：可翻页、管理互动
- 观众视角：跟随主持人翻页、参与互动

### 4. PPT 实时同步
- 使用 PDF.js 渲染 PDF
- WebSocket 实时同步页码
- 缩略图导航

### 5. 互动模块
- 选择题（单选/多选、实时统计）
- 问答题（自由作答、词云展示）
- 主持人可开放/关闭/删除/编辑问题

### 6. 讨论区
- 发布评论
- 回复评论
- 匿名选项

## API 接口

### 用户相关
- `POST /api/auth/login` - 模拟登录
- `GET /api/users/{id}` - 获取用户信息

### 课堂相关
- `POST /api/classrooms` - 创建课堂
- `GET /api/classrooms/{id}` - 获取课堂详情
- `GET /api/classrooms/code/{classCode}` - 通过课堂码获取课堂
- `GET /api/classrooms/host/{userId}` - 获取用户创建的课堂列表
- `POST /api/classrooms/{id}/upload` - 上传 PDF

### 问题相关
- `GET /api/questions/classroom/{classroomId}` - 获取课堂问题列表
- `POST /api/questions` - 创建问题
- `PUT /api/questions/{id}` - 更新问题
- `DELETE /api/questions/{id}` - 删除问题
- `POST /api/questions/{id}/toggle` - 开放/关闭问题
- `POST /api/questions/{id}/finish` - 结束问题统计

### 答案相关
- `POST /api/answers` - 提交答案
- `GET /api/answers/question/{questionId}` - 获取问题的所有答案
- `GET /api/answers/statistics/{questionId}` - 获取答案统计

### 讨论相关
- `GET /api/discussions/classroom/{classroomId}` - 获取讨论列表
- `POST /api/discussions` - 发布讨论

## WebSocket 事件

连接地址：`ws://localhost:8080/ws`

### 客户端发送事件
- `join_classroom` - 加入课堂
- `page_change` - 翻页（仅主持人）
- `question_open` - 开放问题
- `question_close` - 关闭问题
- `answer_submit` - 提交答案
- `discussion_new` - 新讨论

### 服务端推送事件
- `page_update` - 页码更新
- `question_update` - 问题状态更新
- `answer_update` - 答案更新
- `discussion_update` - 讨论更新

## 开发说明

### 数据库
使用 H2 内存数据库，应用重启后数据会重置。如需持久化，可修改 `application.properties` 配置。

### 文件上传
PDF 文件上传后存储在 `backend/uploads/` 目录下。

### WebSocket
后端使用 Spring WebSocket，前端使用原生 WebSocket API。

## 注意事项

1. 确保 8080 端口未被占用（后端）
2. 确保 5173 端口未被占用（前端）
3. 首次运行需要安装依赖
4. 跨域已在后端配置，无需额外处理

## 许可证

MIT License

