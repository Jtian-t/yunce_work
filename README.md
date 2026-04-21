# 招聘流程协同平台

基于 Figma 前端原型实现的一套招聘流程协同平台，包含：

- 前端：React + Vite 的招聘协同后台页面
- 后端：Java 21 + Spring Boot 3 的业务服务
- Agent 扩展：用于用户简历分析与评判的异步任务接口

当前仓库已经可以在本地完成前后端联调，覆盖候选人台账、详情、部门待办、反馈提交、Dashboard、日报和简历分析回调等核心链路。

## 目录结构

```text
.
├─ backend/                 # Java 后端
├─ 招聘流程协同平台后台/      # React 前端
├─ PLAN.md
└─ 招聘提效方案.md
```

## 技术栈

### 前端

- React
- Vite
- TypeScript
- MUI / Radix UI / Recharts

### 后端

- Java 21
- Spring Boot 3.4
- Spring Security + JWT
- Spring Data JPA
- H2（本地默认）
- MySQL / Redis / MinIO（生产配置已预留）
- SpringDoc OpenAPI

## 当前已实现的能力

- 认证与授权：JWT 登录、刷新令牌、RBAC 角色控制
- 候选人管理：候选人列表、详情、更新、时间线
- 简历管理：简历上传、下载、元数据存储
- 流程协同：候选人分发给部门、部门待办、已办、催办
- 反馈与面试：部门反馈、面试安排、面试评价
- 看板与日报：Dashboard 聚合、日报快照查询
- Agent 扩展：创建简历分析任务、查询最新任务、接收异步回调
- 通知与超时：超时扫描、提醒通知数据落库

## 环境要求

- Node.js 18+，建议 Node.js 20+
- npm 9+
- JDK 21+（JDK 23 也可运行）
- Maven 3.9+

## 快速启动

### 1. 启动后端

进入后端目录：

```powershell
cd backend
```

使用本地 profile 启动：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

也可以先打包再运行：

```powershell
mvn -DskipTests package
java -jar target/platform-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

后端默认地址：

- 服务地址：[http://localhost:8080](http://localhost:8080)
- 健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger 文档：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- H2 控制台：[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

### 2. 启动前端

进入前端目录：

```powershell
cd 招聘流程协同平台后台
```

安装依赖：

```powershell
npm install
```

启动开发服务器：

```powershell
npm run dev
```

前端默认会运行在以下地址之一：

- [http://localhost:5173](http://localhost:5173)
- 如果 5173 被占用，Vite 会自动切换到其他端口，请以终端输出为准

## 本地联调说明

### 前端默认联调配置

前端会通过 `VITE_API_BASE_URL` 访问后端，当前代码默认值为：

```text
http://localhost:8080
```

如果你想改成别的后端地址，可以在前端目录新增 `.env.local`：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### 本地 profile 的行为

后端默认使用 `local` profile，特点如下：

- 数据库使用 H2 内存库，不需要额外安装 MySQL
- 简历文件默认写入 `backend/backend-data/resumes`
- Redis 健康检查已关闭，避免本地未安装 Redis 时启动失败
- 启动时会自动注入演示账号、部门和候选人数据

## 默认演示账号

本地启动后，后端会自动写入以下账号，密码统一为：

```text
Password123!
```

账号列表：

- `admin`：平台管理员
- `hr`：HR
- `techlead`：技术部门负责人
- `productlead`：产品部门负责人
- `interviewer`：面试官

说明：

- 当前前端 `DataContext` 为了便于本地联调，会自动使用 `hr / Password123!` 登录
- 如果后续要接正式登录页，可以把这段自动登录逻辑替换掉

## 使用方式

推荐按下面的顺序体验项目：

1. 打开前端首页 Dashboard，查看候选人概览、漏斗、状态分布和超时提醒
2. 进入候选人列表，查看候选人台账
3. 点击候选人详情，查看基本信息、流程时间线、反馈和面试信息
4. 进入部门待办页，查看等待处理的候选人
5. 打开反馈页，提交通过或淘汰反馈
6. 返回 Dashboard 或候选人详情，确认状态流转是否更新
7. 访问日报页，查看当日汇总

当前可访问的主要前端页面：

- `/`：Dashboard
- `/candidates`：候选人列表
- `/candidates/:id`：候选人详情
- `/dept`：部门待办
- `/dept/completed`：部门已办
- `/feedback/:id`：部门反馈页
- `/report`：日报页

## 主要接口

### 认证与账户

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`

### 候选人

- `GET /api/candidates`
- `POST /api/candidates`
- `GET /api/candidates/{id}`
- `PUT /api/candidates/{id}`
- `GET /api/candidates/{id}/timeline`
- `POST /api/candidates/{id}/resume`
- `GET /api/candidates/{id}/resume/download`

### 部门协同

- `POST /api/candidates/{id}/assignments`
- `GET /api/department/tasks`
- `GET /api/department/tasks/completed`
- `POST /api/department/tasks/{assignmentId}/remind`

### 反馈与面试

- `POST /api/feedbacks`
- `GET /api/candidates/{id}/feedbacks`
- `POST /api/interviews`
- `POST /api/interviews/{id}/evaluations`
- `GET /api/candidates/{id}/interviews`

### Dashboard 与日报

- `GET /api/dashboard/overview`
- `GET /api/dashboard/funnel`
- `GET /api/dashboard/status-distribution`
- `GET /api/dashboard/department-efficiency`
- `GET /api/dashboard/alerts`
- `GET /api/reports/daily?date=2026-04-21`

### 简历分析 Agent

- `POST /api/candidates/{id}/analysis-jobs`
- `GET /api/candidates/{id}/analysis-jobs/latest`
- `POST /api/internal/agent/jobs/{jobId}/result`

## Agent 扩展说明

平台已经预留了异步简历分析能力，方式如下：

1. 主后端创建分析任务
2. 外部 Agent 获取任务上下文并执行分析
3. Agent 通过内部回调接口回写结果
4. 前端查询最新分析结果并展示给用户

适合扩展的分析维度：

- 候选人与 JD 的匹配度
- 技术栈匹配度
- 项目经历亮点
- 风险提示
- 推荐动作

## 常用验证命令

### 后端测试

```powershell
cd backend
mvn test
```

### 后端打包

```powershell
cd backend
mvn -DskipTests package
```

### 前端构建

```powershell
cd 招聘流程协同平台后台
npm run build
```

## 常见问题

### 1. 前端提示无法请求后端

请优先确认：

- 后端是否已经启动在 `http://localhost:8080`
- 前端的 `VITE_API_BASE_URL` 是否配置正确
- 浏览器控制台是否存在跨域错误

### 2. 本地没有 MySQL / Redis / MinIO 能不能跑

可以。默认 `local` profile 已经切换到：

- H2 内存数据库
- 本地文件存储
- 关闭 Redis 健康检查

所以本地开发不依赖这三类外部服务。

### 3. 为什么前端一打开就有数据

因为后端启动时会自动注入演示数据，前端也会自动用 `hr` 账号登录，目的是让联调和演示开箱即用。

## 后续建议

- 增加前端登录页，替换当前自动登录逻辑
- 把前端剩余静态页面逐步切换为真实接口
- 接入真实 MySQL / Redis / MinIO 环境
- 为外部简历分析 Agent 增加消息队列或任务调度能力
- 补充更多接口测试和浏览器级 E2E 测试
