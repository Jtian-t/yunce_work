
# CLAUDE.md - 简历分析 Agent

## 项目概览

基于 FastAPI 的简历分析子服务，作为 Java 主项目的 sidecar 服务，提供两类核心能力：

1. **简历结构化解析** (`POST /api/resume/parse`)：把简历文本转换为 `CandidateInfo`
2. **候选人岗位适配分析** (`POST /api/resume/analyze`)：结合岗位要求和面试反馈，给出匹配度建议

**技术栈**：FastAPI + OpenAI SDK + Pydantic

## 目录结构

```
parse_pdf/
├── src/
│   ├── main.py                 # FastAPI 入口
│   ├── config.py               # 配置管理（Settings）
│   ├── schemas.py            # Pydantic 模型定义
│   ├── llm_client.py          # LLM 客户端（JSON 强制输出 + 重试）
│   ├── api/
│   │   └── resume.py         # API 路由
│   ├── agents/
│   │   ├── resume_parser.py  # 简历解析 Agent
│   │   └── recommendation.py # 分析建议 Agent
│   └── services/
│       └── resume_service.py # 业务服务层
├── tests/
│   ├── sample_resume.txt     # 示例简历
│   └── test_api.py            # 测试脚本
├── .env.example              # 环境变量示例
├── requirements.txt           # Python 依赖
├── PLAN.md                  # 执行计划
├── API.md                   # API 文档
└── README.md                # 项目说明
```

## 启动命令

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入 LLM_API_KEY、LLM_BASE_URL、LLM_MODEL

# 3. 启动服务（开发模式）
uvicorn src.main:app --reload

# 4. 访问文档
# http://localhost:8000/docs
```

## 测试命令

```bash
# 健康检查
curl http://localhost:8000/health

# 使用 Swagger UI 测试
# 打开 http://localhost:8000/docs

# 使用 tests/test_api.py (需自行取消注释后运行)
python tests/test_api.py
```

## 代码规范

### 分层架构

- **API 层** (`src/api/`)：只做参数接收、响应包装、异常处理
- **Service 层** (`src/services/`)：业务编排，不涉及具体 LLM 调用
- **Agent 层** (`src/agents/`)：封装 LLM 调用、提示词管理
- **LLM 客户端** (`src/llm_client.py`)：通用 LLM 通信、JSON 提取与重试

### 提示词约定

- 系统提示词放在各 Agent 文件顶部
- 强制纯 JSON 输出，不要 Markdown 解释
- 使用 `temperature=0.3` 保证输出稳定

### 异常处理

- API 层捕获异常统一转 `HTTPException(status_code=500)`
- LLM 调用失败自动重试最多 3 次

## 关键架构

### 请求流程

```
Java 主项目
    ↓ HTTP
FastAPI 路由 (src/api/resume.py
    ↓
ResumeService
    ↓
ResumeParserAgent / RecommendationAgent
    ↓
LLMClient (JSON 提取 + 重试
    ↓
LLM API
```

### 数据模型

| 模型 | 位置 | 说明 |
|------|------|
| `CandidateInfo` | `src/schemas.py` | 解析后的候选人信息 |
| `InterviewFeedback` | `src/schemas.py` | 单轮面试反馈 |
| `AnalysisResult` | `src/schemas.py` | 分析结果 |

### 多态反馈设计

`InterviewFeedback` 支持可选字段：
- `round`, `interviewer`, `feedback` 必填
- `score`, `pros`, `cons` 可选
- `interview_feedbacks` 可空数组

## 当前进度

✅ 项目初始化与目录结构

✅ 配置管理（Settings + LLMClient）

✅ 数据模型定义（CandidateInfo, InterviewFeedback, AnalysisResult）

✅ 简历解析 Agent（含优化提示词）

✅ 分析建议 Agent（支持多轮反馈）

✅ 两个 API 接口

✅ API 文档与测试示例

## 已知问题

暂无。

## 下次建议切入点

1. **接入真实 PDF 解析**：当前接口目前只接受纯文本，可集成 `PyPDF2` 或 `pdfplumber` 直接从 MinIO/本地读取 PDF
2. **添加请求/响应日志**：便于调试和问题排查
3. **添加单元测试**：针对 Agent 和 Service 层
4. **支持更多 LLM 模型**：扩展 `llm_client.py` 支持多模型切换
5. **缓存机制**：相同简历文本避免重复解析
6. **性能优化**：异步 LLM 调用
7. **监控指标**：添加 Prometheus 指标暴露

