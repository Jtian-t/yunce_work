
# API 接口文档

## 接口1: 简历解析

**POST** `/api/resume/parse`

### 请求参数

```json
{
  "resume_text": "张三\n电话：13800138000\n邮箱：zhangsan@example.com\n..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| resume_text | string | 是 | 简历文本内容 |

### 响应结果 (CandidateInfo)

```json
{
  "name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "education": [
    {
      "school": "清华大学",
      "major": "计算机科学与技术",
      "degree": "本科",
      "duration": "2016-2020"
    }
  ],
  "work_experience": [
    {
      "company": "阿里巴巴",
      "position": "高级工程师",
      "duration": "2020-至今",
      "description": "负责电商平台后端开发..."
    }
  ],
  "projects": [
    {
      "name": "双11大促系统",
      "role": "核心开发者",
      "description": "支撑百万级并发的订单系统",
      "tech_stack": ["Java", "Spring Boot", "Redis"]
    }
  ],
  "skills": ["Java", "Python", "Redis", "MySQL"],
  "summary": "5年后端开发经验，擅长高并发系统设计..."
}
```

---

## 接口2: 分析建议

**POST** `/api/resume/analyze`

### 请求参数

```json
{
  "candidate_info": {
    "name": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "education": [...],
    "work_experience": [...],
    "projects": [...],
    "skills": [...],
    "summary": "..."
  },
  "job_requirements": "岗位职责：\n1. 5年以上Java开发经验\n2. 熟悉Spring Boot、Redis\n3. 有高并发系统经验优先",
  "interview_feedbacks": [
    {
      "round": 1,
      "interviewer": "李面试官",
      "feedback": "候选人技术基础扎实，对Redis理解深入，但项目经验细节描述不够清晰",
      "score": 80,
      "pros": ["技术基础好", "沟通顺畅"],
      "cons": ["项目细节不够"]
    },
    {
      "round": 2,
      "interviewer": "王面试官",
      "feedback": "系统设计能力不错，对高并发有自己的见解",
      "score": 85,
      "pros": ["设计能力强"],
      "cons": []
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| candidate_info | CandidateInfo | 是 | 解析后的候选人信息 |
| job_requirements | string | 是 | 岗位要求描述 |
| interview_feedbacks | InterviewFeedback[] | 否 | 已完成的面试反馈列表 |

### InterviewFeedback 结构

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| round | int | 是 | 面试轮次 |
| interviewer | string | 是 | 面试官姓名 |
| feedback | string | 是 | 反馈内容 |
| score | int | 否 | 评分 (0-100) |
| pros | string[] | 否 | 优点列表 |
| cons | string[] | 否 | 缺点列表 |

### 响应结果 (AnalysisResult)

```json
{
  "experience_score": 85,
  "skill_match_score": 90,
  "overall_score": 87,
  "recommendation_reason": "候选人技术背景匹配度高，两轮面试反馈都不错，建议进入下一轮",
  "risk_points": [
    "项目经验细节需要进一步确认",
    "期望薪资可能较高"
  ],
  "interview_questions": [
    "请详细描述双11系统中你负责的模块具体实现",
    "如果让你 redesign 这个系统，你会做哪些改进？"
  ],
  "feedback_summary": "第一轮：技术基础扎实，Redis理解深入；第二轮：系统设计能力优秀"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| experience_score | int | 经验评分 (0-100) |
| skill_match_score | int | 技能匹配度 (0-100) |
| overall_score | int | 综合评分 (0-100) |
| recommendation_reason | string | 推荐理由 |
| risk_points | string[] | 风险点列表 |
| interview_questions | string[] | 建议面试问题 |
| feedback_summary | string | 历史反馈总结 |

