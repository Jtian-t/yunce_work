pip install -r requirements.txt
# 简历分析 Agent

基于 FastAPI 的简历分析系统，提供简历解析和分析建议两个核心接口。

## 功能特性

- **简历解析** - 将简历文本转换为结构化JSON数据
- **分析建议** - 根据候选人信息、岗位要求和多轮面试反馈生成决策建议
- **强制JSON输出** - 保证大模型输出标准格式
- **多态反馈支持** - 支持动态传入多轮面试官评价

## 快速开始

### 1. 安装依赖

```bash
使用conda环境
conda activate Agentic_AI
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入你的 LLM API Key
```

### 3. 启动服务

```bash
uvicorn src.main:app --reload
```

### 4. 访问文档

打开浏览器访问: http://localhost:8000/docs

## API 接口

详见 [API.md](./API.md)

### 接口1: 简历解析

```
POST /api/resume/parse
Body: { "resume_text": "..." }
Response: CandidateInfo
```

### 接口2: 分析建议

```
POST /api/resume/analyze
Body: {
  "candidate_info": CandidateInfo,
  "job_requirements": "...",
  "interview_feedbacks": [...]
}
Response: AnalysisResult
```

## 项目结构

```
parse_pdf/
├── src/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置管理
│   ├── schemas.py           # Pydantic 数据模型
│   ├── llm_client.py        # LLM 客户端
│   ├── api/
│   │   └── resume.py        # API 路由
│   ├── agents/
│   │   ├── resume_parser.py   # 简历解析 Agent
│   │   └── recommendation.py  # 分析建议 Agent
│   └── services/
│       └── resume_service.py  # 业务服务
├── tests/
│   ├── sample_resume.txt    # 示例简历
│   └── test_api.py          # 测试脚本
├── PLAN.md                  # 执行计划
├── API.md                   # API 文档
└── requirements.txt         # 依赖
```

## 测试

```bash
# 先启动服务
uvicorn src.main:app --reload

# 使用 curl 或访问 /docs 进行测试
```

