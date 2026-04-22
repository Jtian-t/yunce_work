
# 简历分析 Agent 执行计划

## Context

用户需要构建一个基于FastAPI的简历分析系统，包含两个核心接口：
1. **简历解析接口** - HR上传简历第一步调用，提取结构化信息
2. **分析建议接口** - 传入已解析的简历 + 多轮面试官反馈 + 岗位要求，生成决策建议

核心要求：强制大模型输出标准化JSON格式，简历解析要健壮（支持工作/项目经验动态可选）。

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        FastAPI App                           │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐         ┌──────────────┐              │
│  │  /api/resume/    │────────▶│  Service层   │              │
│  │  - parse         │         └──────────────┘              │
│  │  - analyze       │              │                        │
│  └──────────────────┘              ▼                        │
│                            ┌──────────────┐                │
│                            │   Agent层    │                │
│                            │  - Parser    │                │
│                            │  - Analyzer  │                │
│                            └──────────────┘                │
└─────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                           ┌──────────────┐
                           │   LLM API    │
                           └──────────────┘
```

## 实现步骤

### Step 1: 项目初始化
- 创建项目目录结构
- 生成 `requirements.txt`（FastAPI, Uvicorn, python-dotenv, langchain/openai, pydantic）
- 创建 `.env.example` 配置文件

### Step 2: 配置与基础模块
- `config.py` - 配置管理（LLM API密钥等）
- `llm_client.py` - LLM客户端封装（含JSON强制输出逻辑 + 重试机制）

### Step 3: 数据模型设计
- `schemas.py` - Pydantic模型
  - `ResumeParseRequest` - 解析请求（简历文本）
  - `CandidateInfo` - 解析结果（含动态可选字段）
  - `InterviewFeedback` - 单轮面试反馈
  - `ResumeAnalyzeRequest` - 分析请求（CandidateInfo + Feedback列表 + 岗位要求）
  - `AnalysisResult` - 最终分析结果

### Step 4: Agent核心逻辑
- `agents/resume_parser.py` - 简历解析Agent（专门优化）
  - 健壮的提示词（支持无工作经验、只有项目经验等情况）
  - JSON schema强制约束
  - 输出后验证与fallback
- `agents/recommendation.py` - 分析建议Agent
  - 多态反馈拼接逻辑
  - 动态提示词构建

### Step 5: 业务逻辑层
- `services/resume_service.py` - 简历服务
  - `parse_resume()` - 解析逻辑
  - `analyze_candidate()` - 分析逻辑

### Step 6: API层
- `main.py` - FastAPI应用入口
- `api/resume.py` - 两个API路由
  - `POST /api/resume/parse` - 简历解析
  - `POST /api/resume/analyze` - 分析建议

### Step 7: 提示词设计

**简历解析提示词（优化版）**：
```
你是一个专业的简历解析助手。请从以下简历文本中提取候选人信息，严格按照JSON输出。

注意：
- 若没有工作经验，work_experience 返回空数组 []
- 若没有项目经验，projects 返回空数组 []
- 若字段缺失，留空字符串 "" 或 null

{
  "name": "姓名",
  "phone": "电话",
  "email": "邮箱",
  "education": [
    {"school": "学校", "major": "专业", "degree": "学历", "duration": "时间段"}
  ],
  "work_experience": [
    {"company": "公司", "position": "职位", "duration": "时间", "description": "职责描述"}
  ],
  "projects": [
    {"name": "项目名", "role": "角色", "description": "描述", "tech_stack": ["技术栈"]}
  ],
  "skills": ["技能1", "技能2"],
  "summary": "个人总结"
}

简历内容：
{resume_text}
```

**分析建议提示词**：
```
你是一个专业的招聘顾问。请根据以下信息给出分析建议，严格JSON输出：

{
  "experience_score": 85,
  "skill_match_score": 90,
  "overall_score": 87,
  "recommendation_reason": "推荐理由...",
  "risk_points": ["风险点1"],
  "interview_questions": ["问题1"],
  "feedback_summary": "历史反馈总结..."
}

候选人信息：
{candidate_info}

岗位要求：
{job_requirements}

已完成的面试反馈：
{interview_feedbacks}
```

## 核心文件路径

- `src/main.py` - FastAPI入口
- `src/config.py` - 配置
- `src/llm_client.py` - LLM客户端
- `src/schemas.py` - 数据模型
- `src/agents/resume_parser.py` - 解析Agent（重点优化）
- `src/agents/recommendation.py` - 建议Agent
- `src/services/resume_service.py` - 业务服务
- `src/api/resume.py` - API路由（两个接口）
- `requirements.txt` - 依赖

## API 接口设计

```
POST /api/resume/parse
Body: { "resume_text": "..." }
Response: CandidateInfo

POST /api/resume/analyze
Body: {
  "candidate_info": CandidateInfo,
  "job_requirements": "...",
  "interview_feedbacks": [
    {"round": 1, "interviewer": "张三", "feedback": "...", "score": 80}
  ]
}
Response: AnalysisResult
```

## 验证计划

1. 启动服务：`uvicorn src.main:app --reload`
2. 测试解析接口：`POST /api/resume/parse`
3. 测试分析接口：`POST /api/resume/analyze`（无反馈/带1轮/带多轮）
4. 验证边缘情况：无工作经验、只有项目经验的简历
