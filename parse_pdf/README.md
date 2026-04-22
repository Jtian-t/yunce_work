# 简历分析 Agent

基于 FastAPI 的简历分析子服务，提供两类能力：

- 简历结构化解析：把简历文本转换为候选人结构化信息
- 候选人岗位适配分析：结合岗位要求和面试反馈，给出匹配度建议

## 启动方式

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

需要至少配置：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`

### 3. 启动服务

```bash
uvicorn src.main:app --reload
```

默认地址：

- `http://localhost:8000`
- Swagger 文档：`http://localhost:8000/docs`

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

本目录作为 Python sidecar 服务使用：

- Java Spring 主项目负责附件读取、业务编排、结果落库
- 本服务负责调用 LLM 完成解析与适配度分析

Spring 侧默认通过 HTTP 调用：

- `/api/resume/parse`
- `/api/resume/analyze`
