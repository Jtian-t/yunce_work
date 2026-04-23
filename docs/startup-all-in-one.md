# 项目启动总文档

本文档用于启动当前仓库的完整联调链路，包括：

- 基础依赖服务：MySQL、MinIO
- Java Spring 后端
- React 前端
- `parse_pdf` Python 简历分析 Agent

适用目录：

- 根目录：`D:/claude_program/Codex/云测作业`
- 后端：`D:/claude_program/Codex/云测作业/backend`
- 前端：`D:/claude_program/Codex/云测作业/招聘流程协同平台后台`
- Agent：`D:/claude_program/Codex/云测作业/parse_pdf`

## 1. 环境要求

启动整套链路前，请先确认本机具备：

- JDK 21 或更高
- Maven 3.9 或更高
- Node.js 18 或更高
- npm 9 或更高
- Python 3.10 或更高
- Docker Desktop

建议版本：

- Java 21
- Node 20
- Python 3.11+

## 2. 启动顺序

推荐顺序：

1. 启动 Docker 依赖服务
2. 启动 Python `parse_pdf` sidecar
3. 启动 Java Spring 后端
4. 启动 React 前端
5. 访问前端并验证端到端链路

## 3. 启动基础依赖

在项目根目录执行：

```powershell
cd D:\claude_program\Codex\云测作业
docker compose up -d
```

会启动：

- MySQL 8.4
- MinIO
- MinIO bucket 初始化任务

默认端口：

- MySQL：`localhost:3307`
- MinIO API：`http://localhost:9000`
- MinIO Console：`http://localhost:9001`

默认账号：

- MySQL
  - 用户名：`root`
  - 密码：`root123456`
- MinIO
  - Access Key：`minioadmin`
  - Secret Key：`minioadmin`

## 4. 根目录环境变量

根目录已有 `.env.example`，建议先复制一份：

```powershell
Copy-Item .env.example .env
```

默认关键配置如下：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=recruit_platform
DB_USERNAME=root
DB_PASSWORD=root123456

MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=resumes

STORAGE_MODE=minio
SERVER_PORT=8080
```

注意：

- `docker-compose.yml` 映射到宿主机的 MySQL 端口是 `3307`
- 后端 `application.yml` 默认也是 `3307`
- 如果你用根目录 `.env` 覆盖了 `DB_PORT`，建议设成 `3307`

推荐改成：

```env
DB_PORT=3307
```

## 5. 启动 Python 简历分析 Agent

进入 `parse_pdf` 目录：

```powershell
cd D:\claude_program\Codex\云测作业\parse_pdf
```

### 5.1 创建并激活虚拟环境

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
```

如果你的机器没有 `py`，也可以改用自己安装的 Python 可执行文件。

### 5.2 安装依赖

```powershell
pip install -r requirements.txt
```

### 5.3 配置 Agent 环境变量

复制配置：

```powershell
Copy-Item .env.example .env
```

至少需要设置：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`

示例：

```env
LLM_API_KEY=your_api_key_here
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4-turbo-preview
```

### 5.4 启动 Agent

```powershell
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

启动后验证：

- 健康检查：`http://localhost:8000/health`
- 文档：`http://localhost:8000/docs`

## 6. 启动 Java Spring 后端

进入后端目录：

```powershell
cd D:\claude_program\Codex\云测作业\backend
```

### 6.1 后端默认关键配置

当前 `application.yml` 关键默认值：

- 服务端口：`8080`
- 数据库：`jdbc:mysql://localhost:3307/recruit_platform`
- MinIO：`http://localhost:9000`
- Agent sidecar：`http://localhost:8000`

也就是：

- 后端默认会把附件存到 MinIO
- 后端默认会把简历解析和岗位分析请求发给 Python sidecar

### 6.2 启动后端

```powershell
mvn spring-boot:run
```

如果你习惯先编译：

```powershell
mvn -DskipTests compile
mvn spring-boot:run
```

启动后验证：

- 接口根地址：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- Swagger：`http://localhost:8080/swagger-ui.html`

## 7. 启动前端

进入前端目录：

```powershell
cd D:\claude_program\Codex\云测作业\招聘流程协同平台后台
```

### 7.1 安装依赖

```powershell
npm install
```

### 7.2 前端环境变量

如需显式指定后端地址，可创建 `.env.local`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 7.3 启动前端

```powershell
npm run dev
```

默认访问地址通常是：

- `http://localhost:5173`

如果 5173 被占用，Vite 会自动切换端口，请以终端输出为准。

## 8. 启动完成后的访问关系

全部启动后，整体链路如下：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Python Agent：`http://localhost:8000`
- MinIO Console：`http://localhost:9001`
- MySQL：`localhost:3307`

调用关系：

1. 前端请求 Spring 后端
2. Spring 后端读取简历附件
3. Spring 后端调用 `parse_pdf`
4. `parse_pdf` 返回结构化简历与岗位适配分析
5. Spring 后端写回 `AgentJob / AgentResult`
6. 前端候选人详情页展示解析与决策结果

## 9. 联调验证步骤

建议按下面顺序验证：

1. 打开前端候选人列表
2. 进入某个候选人详情页
3. 上传或确认已有简历
4. 点击“重新解析”
5. 确认 `简历解析洞察` 中出现真实结构化结果
6. 在“编辑基础信息”里填写 `岗位要求摘要`
7. 点击“辅助决策”
8. 确认结果综合了：
   - 简历解析
   - 岗位要求摘要
   - 面试官评价

## 10. 关键配置说明

### 10.1 后端 Agent 配置

位于：

- `backend/src/main/resources/application.yml`

关键项：

```yaml
app:
  agent:
    enabled: true
    fallback-to-mock: false
    service-base-url: http://localhost:8000
    parse-path: /api/resume/parse
    analyze-path: /api/resume/analyze
```

说明：

- `enabled=true`：启用 Python sidecar
- `fallback-to-mock=false`：sidecar 不可用时直接失败，不走旧 mock

### 10.2 候选人岗位要求摘要

当前岗位匹配分析优先使用：

1. 候选人 `jdSummary`
2. 请求里的临时 `focusHint`
3. 候选人 `position`

所以如果希望“岗位合适度分析”更准确，建议在候选人详情页先补充 `岗位要求摘要`。

## 11. 常见问题

### 11.1 后端启动时报数据库连接错误

重点检查：

- Docker 的 MySQL 是否已启动
- 端口是否是 `3307`
- `.env` 是否把 `DB_PORT` 错设成了 `3306`

推荐最终统一成：

```env
DB_PORT=3307
```

### 11.2 `mvn test` 报 `Public Key Retrieval is not allowed`

这通常是本机 MySQL 连接参数问题，不是 agent 代码本身问题。

如果只是验证当前代码是否能编译，可先执行：

```powershell
mvn -DskipTests compile
```

### 11.3 前端启动时报 `esbuild spawn EPERM`

这是当前桌面沙箱环境里常见的问题，优先在你本机终端直接运行：

```powershell
npm run dev
```

### 11.4 Python Agent 启动时报找不到 `python`

请先确认本机安装了 Python，并使用：

```powershell
py -3 --version
```

如果有版本输出，再使用：

```powershell
py -3 -m venv .venv
```

### 11.5 解析或分析接口失败

优先检查：

- `parse_pdf` 是否启动在 `8000`
- `.env` 中的 `LLM_API_KEY / LLM_BASE_URL / LLM_MODEL` 是否正确
- 后端 `app.agent.service-base-url` 是否仍是 `http://localhost:8000`

## 12. 建议的日常启动方式

每次本地联调，推荐开 4 个终端：

### 终端 1：Docker 依赖

```powershell
cd D:\claude_program\Codex\云测作业
docker compose up -d
```

### 终端 2：Python Agent

```powershell
cd D:\claude_program\Codex\云测作业\parse_pdf
.\.venv\Scripts\Activate.ps1
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

### 终端 3：Spring 后端

```powershell
cd D:\claude_program\Codex\云测作业\backend
mvn spring-boot:run
```

### 终端 4：React 前端

```powershell
cd D:\claude_program\Codex\云测作业\招聘流程协同平台后台
npm run dev
```
