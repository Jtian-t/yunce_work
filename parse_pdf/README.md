# 简历分析 Agent

`parse_pdf` 是当前招聘协同平台使用的 Python sidecar 服务，负责两类能力：

- 简历结构化解析
- 结合岗位要求和面试反馈生成候选人岗位适配建议

Java Spring 主服务会通过 HTTP 调用它，不直接把它嵌进 JVM 里运行。

## 启动方式

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

复制示例文件：

```bash
cp .env.example .env
```

至少需要配置：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`

示例：

```env
LLM_API_KEY=your_api_key_here
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4-turbo-preview
```

### 3. 启动服务

```bash
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

默认地址：

- `http://localhost:8000`
- Swagger 文档：`http://localhost:8000/docs`
- 健康检查：`http://localhost:8000/health`

## API

### `POST /api/resume/parse`

输入：

```json
{
  "resume_text": "..."
}
```

输出：结构化 `CandidateInfo`

### `POST /api/resume/analyze`

输入：

```json
{
  "candidate_info": {},
  "job_requirements": "...",
  "interview_feedbacks": []
}
```

输出：`AnalysisResult`

## 与 Java 主项目的关系

当前架构固定为：

- Java Spring 主服务负责附件读取、流程编排、结果落库、前端接口
- `parse_pdf` 负责调用 LLM，输出简历解析和岗位适配分析

Spring 侧默认通过以下接口调用本服务：

- `POST /api/resume/parse`
- `POST /api/resume/analyze`

对应的 Spring 配置位于：

- `backend/src/main/resources/application.yml`

关键配置项：

- `app.agent.service-base-url`
- `app.agent.parse-path`
- `app.agent.analyze-path`
- `app.agent.enabled`
- `app.agent.fallback-to-mock`
