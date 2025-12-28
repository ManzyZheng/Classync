# 快速启动指南

## 前置要求

### 后端
- **JDK 17+** (必须)
- **Maven 3.6+** (必须)

### 前端
- **Node.js 16+** (必须)
- **npm 或 pnpm** (必须)

## 第一步：启动后端

打开终端，进入后端目录：

```bash
cd backend
```

### 首次运行（安装依赖并启动）

```bash
mvn clean install
mvn spring-boot:run
```

### 后续运行

```bash
mvn spring-boot:run
```

**启动成功标志**：
- 控制台显示 "Started ClassyncApplication in X seconds"
- 访问 http://localhost:8080/h2-console 能看到 H2 数据库控制台

**后端服务地址**：
- API: http://localhost:8080/api
- H2 Console: http://localhost:8080/h2-console
- WebSocket: ws://localhost:8080/ws

## 第二步：启动前端

**新开一个终端窗口**，进入前端目录：

```bash
cd frontend
```

### 首次运行（安装依赖并启动）

```bash
npm install
npm run dev
```

或使用 pnpm（更快）：

```bash
pnpm install
pnpm dev
```

### 后续运行

```bash
npm run dev
```

**启动成功标志**：
- 控制台显示 "Local: http://localhost:5173/"
- 浏览器自动打开或手动访问该地址

## 第三步：开始使用

### 1. 登录系统

1. 打开浏览器访问 http://localhost:5173
2. 输入账号和姓名（任意内容，系统会自动创建）
3. 点击"登录"
4. 在弹窗中点击"已完成认证"

### 2. 创建课堂（主持人视角）

1. 登录后进入主页
2. 右侧点击"+ 创建课堂"
3. 填写课堂信息：
   - 课堂名称（如：Java高级编程）
   - 开始时间
   - 结束时间
   - 上传 PDF 文件（可选）
4. 点击"确定"
5. 记下生成的**课堂码**（6位大写字母数字组合）
6. 点击课堂卡片进入主持人视角

### 3. 加入课堂（观众视角）

**方式一：使用不同浏览器/隐身窗口**
1. 打开新的浏览器或隐身窗口
2. 访问 http://localhost:5173
3. 使用不同账号登录
4. 左侧输入课堂码
5. 点击"加入"

**方式二：退出后重新登录**
1. 在当前页面点击"退出"
2. 用不同账号重新登录
3. 输入课堂码加入

### 4. 课堂互动演示

#### 主持人操作：
1. **翻页**：点击中间 PDF 区域的"上一页/下一页"
2. **创建问题**：
   - 右侧"互动"标签
   - 点击"+"号
   - 选择"选择题"或"问答题"
   - 填写内容和选项
   - 点击"确定"
3. **开放问题**：点击问题的"开放"按钮
4. **查看统计**：点击已开放的问题，查看实时答题情况
5. **结束问题**：点击"结束统计并展示结果"

#### 观众操作：
1. **跟随翻页**：主持人翻页后自动同步
2. **自由浏览**：可以自行翻页查看，点击"跳转到当前页"回到主持人页面
3. **回答问题**：
   - 看到开放的问题后选择答案
   - 点击"提交"
   - 等待主持人结束后查看结果
4. **参与讨论**：
   - 切换到"讨论区"标签
   - 输入评论内容
   - 勾选"匿名"可匿名发布
   - 点击"回复"按钮回复他人

## 常见问题

### 1. 后端启动失败

**问题**：端口 8080 被占用
**解决**：修改 `backend/src/main/resources/application.properties`
```properties
server.port=8081
```
前端也需要同步修改 `frontend/vite.config.js` 中的代理地址

### 2. 前端无法连接后端

**问题**：CORS 跨域错误
**解决**：确保后端已启动，检查 `application.properties` 中的 CORS 配置

### 3. PDF 无法显示

**问题**：PDF.js worker 加载失败
**解决**：检查网络连接，确保能访问 cdnjs.cloudflare.com

**临时解决**：下载 worker 文件到本地
```bash
# 在 frontend 目录下
npm install pdfjs-dist
```
修改 `PdfViewer.vue`:
```javascript
import * as pdfjsLib from 'pdfjs-dist'
import pdfjsWorker from 'pdfjs-dist/build/pdf.worker.entry'
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorker
```

### 4. WebSocket 连接失败

**问题**：WebSocket handshake 失败
**解决**：
1. 确认后端已启动
2. 检查浏览器控制台是否有 CORS 错误
3. 尝试刷新页面重新连接

### 5. 上传 PDF 失败

**问题**：文件过大
**解决**：修改 `application.properties`:
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

## 测试场景建议

### 场景一：基础翻页同步
1. 主持人创建课堂并上传 PDF
2. 观众加入课堂
3. 主持人翻页，观察观众端是否同步

### 场景二：选择题互动
1. 主持人创建选择题并设置正确答案
2. 主持人开放问题
3. 观众提交答案
4. 主持人查看实时统计
5. 主持人结束统计
6. 观众查看结果（正确答案标绿）

### 场景三：问答题互动
1. 主持人创建问答题
2. 主持人开放问题
3. 多个观众提交不同答案
4. 主持人查看词云（需要实现）
5. 主持人结束统计

### 场景四：讨论区互动
1. 观众发布评论
2. 其他观众回复评论
3. 使用匿名功能发布
4. 观察实时更新

### 场景五：大屏展示模式
1. 主持人创建课堂并上传 PDF
2. 点击教师端右上角的"放映"按钮
3. 系统自动在新窗口中打开展示页面：`http://localhost:5173/classroom/display/{课堂ID}`
4. 大屏会全屏展示当前 PDF 页面（无工具栏、无互动面板）
5. 主持人在自己的设备上翻页时，大屏会实时同步显示
6. 大屏只接收 WebSocket 消息，不发送任何消息

**使用场景**：
- 教室投影仪展示 PPT
- 会议室大屏同步演示
- 线下课堂互动教学

**使用方法**：
- **方法一（推荐）**：点击教师端右上角的"▶ 放映"按钮，自动打开展示页面
- **方法二**：手动在大屏浏览器中访问：`http://localhost:5173/classroom/display/{课堂ID}`

**特点**：
- ✅ 一键打开展示页面
- ✅ 全屏沉浸式展示
- ✅ 实时同步主持人翻页
- ✅ 只读模式，无交互干扰
- ✅ 自适应屏幕尺寸

## 开发调试

### 后端热更新

使用 Spring Boot DevTools，修改代码后自动重启：
```bash
mvn spring-boot:run
```

### 前端热更新

Vite 自动支持热更新，修改代码后立即生效

### 查看数据库

访问 H2 Console: http://localhost:8080/h2-console

连接信息：
- JDBC URL: `jdbc:h2:mem:classync`
- Username: `sa`
- Password: (留空)

可以直接执行 SQL 查看数据：
```sql
SELECT * FROM USERS;
SELECT * FROM CLASSROOMS;
SELECT * FROM QUESTIONS;
SELECT * FROM ANSWERS;
SELECT * FROM DISCUSSIONS;
```

## 构建生产版本

### 后端打包

```bash
cd backend
mvn clean package
```

生成的 JAR 文件在 `target/` 目录，运行：
```bash
java -jar target/classync-backend-1.0.0.jar
```

### 前端构建

```bash
cd frontend
npm run build
```

生成的静态文件在 `dist/` 目录，可以部署到任何静态服务器

## 下一步

1. 阅读 `PROJECT_STRUCTURE.md` 了解详细架构
2. 查看各模块的 README.md
3. 根据需求扩展功能
4. 集成真实的认证系统
5. 更换为持久化数据库

## 技术支持

- 后端问题：检查 Spring Boot 日志
- 前端问题：检查浏览器控制台
- WebSocket 问题：检查网络标签页的 WS 连接

祝使用愉快！🎉

