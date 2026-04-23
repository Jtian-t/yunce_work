# LangChain 简历解析与辅助决策实施方案

## 1. 改造目标

本次改造分两步执行：

1. 先将当前仓库已有改动整体提交并推送到 GitHub，形成清晰基线。
2. 再在 `parse_pdf_langchain` 下实现新的 LangChain 服务，用于：
   - 输出精简、稳定、适合 Java 后端和前端联调的简历解析 JSON
   - 基于简历、岗位要求和多轮面试反馈生成辅助决策 JSON

本次重点不是生成冗长解析文本，而是生成招聘流程真正消费的结构化字段。

## 2. 三端字段映射

### 2.1 简历解析主字段

用于 Python 输出、Java 联调和前端表单回填的精简字段如下：

- `name`
- `targetPosition`
- `phone`
- `email`
- `education`
- `experienceYears`
- `location`
- `source`
- `skillsSummary`
- `projectSummary`

约束：

- 全部为字符串
- 缺失返回空字符串
- `experienceYears` 无法判断时返回 `"0年"`
- `skillsSummary` 为短摘要，不输出长列表堆砌
- `projectSummary` 为短摘要，不输出大段原文

### 2.2 Java 当前兼容字段

为兼容 Java 当前 `parseReport.fields` 消费逻辑，解析结果中继续保留：

- `name`
- `phone`
- `email`
- `location`
- `education`
- `experience`
- `skillsSummary`
- `projectSummary`

说明：

- `experience` 保持旧兼容字段，用于当前 Java/前端直接读取
- `experienceYears` 作为新扩展字段保留在精简 profile 中

### 2.3 辅助决策字段

用于前端辅助决策卡片的核心字段如下：

- `conclusion`
- `recommendationScore`
- `recommendationLevel`
- `recommendedAction`
- `strengths`
- `risks`
- `missingInformation`
- `supportingEvidence`
- `optimizationSuggestions`
- `reasoningSummary`
- `interviewRoundSummaries`

前端语义映射：

- 最新结论 -> `conclusion`
- 推荐动作 -> `recommendedAction`
- 关键优势 -> `strengths`
- 关键风险 -> `risks`
- 缺失信息 / 待补问 -> `missingInformation + optimizationSuggestions`
- 分析依据 -> `supportingEvidence`
- 推理摘要 -> `reasoningSummary`

## 3. 接口路径

LangChain 服务继续兼容当前 HTTP 接口路径：

- `POST /api/resume/parse-report`
- `POST /api/resume/parse-report/upload`
- `POST /api/resume/decision-report`

可选兼容包装接口：

- `POST /api/resume/parse`
- `POST /api/resume/analyze`

## 4. 解析 JSON 示例

### 4.1 精简 profile 示例

```json
{
  "name": "张三",
  "targetPosition": "Java 后端工程师",
  "phone": "13800138000",
  "email": "candidate@example.com",
  "education": "本科，计算机科学与技术",
  "experienceYears": "3年",
  "location": "上海",
  "source": "简历上传",
  "skillsSummary": "熟悉 Java、Spring Boot、MySQL、Redis、RabbitMQ，具备常见后端系统开发经验。",
  "projectSummary": "主导或参与过电商后台和消息调度类项目，覆盖接口开发、缓存优化和消息链路处理。"
}
```

### 4.2 兼容 parse_report 示例

```json
{
  "summary": "已完成候选人关键信息提取，可用于表单预填和后续评估。",
  "highlights": [
    "识别到完整联系方式",
    "识别到核心技术栈",
    "识别到代表性项目经历"
  ],
  "extractedSkills": ["Java", "Spring Boot", "MySQL", "Redis", "RabbitMQ"],
  "projectExperiences": [
    {
      "title": "电商后台项目",
      "summary": "参与订单和库存相关后端开发，负责接口实现与缓存优化。"
    }
  ],
  "fields": {
    "name": { "value": "张三", "confidence": 0.98, "source": "llm" },
    "phone": { "value": "13800138000", "confidence": 0.96, "source": "llm" },
    "email": { "value": "candidate@example.com", "confidence": 0.96, "source": "llm" },
    "location": { "value": "上海", "confidence": 0.80, "source": "llm" },
    "education": { "value": "本科，计算机科学与技术", "confidence": 0.85, "source": "llm" },
    "experience": { "value": "3年后端开发经验", "confidence": 0.82, "source": "llm" },
    "skillsSummary": { "value": "熟悉 Java、Spring Boot、MySQL、Redis、RabbitMQ。", "confidence": 0.88, "source": "llm" },
    "projectSummary": { "value": "参与电商后台与消息调度项目开发。", "confidence": 0.84, "source": "llm" }
  },
  "issues": [],
  "extractionMode": "LANGCHAIN_STRUCTURED",
  "ocrRequired": false
}
```

## 5. 决策 JSON 示例

```json
{
  "conclusion": "候选人当前适合保守推进，建议结合后续技术细节补问后决定是否进入下一轮。",
  "recommendationScore": 72,
  "recommendationLevel": "保守推进",
  "recommendedAction": "建议安排一轮补充技术面，重点确认项目主责范围、系统设计深度和复杂问题处理经验。",
  "strengths": [
    "技术栈与 Java 后端岗位匹配度较高",
    "已有多个后端项目经历，基础开发经验完整",
    "面试反馈显示沟通表达和常规问题回答较稳定"
  ],
  "risks": [
    "复杂项目细节和系统设计深度证据仍不足",
    "部分经历更多体现参与，主导性信息不够明确"
  ],
  "missingInformation": [
    "缺少对高并发场景处理细节的明确信号",
    "缺少对线上问题排查与稳定性治理的直接证据"
  ],
  "supportingEvidence": [
    "简历技能标签：Java、Spring Boot、MySQL、Redis、RabbitMQ",
    "项目摘要显示具备常规后端业务开发经验",
    "多轮面试反馈整体偏正向，但深度问题证据有限"
  ],
  "optimizationSuggestions": [
    "补问候选人在核心项目中的实际负责边界",
    "补问缓存一致性、消息幂等和线上故障排查案例"
  ],
  "reasoningSummary": "本次判断综合了简历基础信息、技术标签、项目摘要和面试反馈。候选人与岗位基础能力匹配，但在高复杂度场景上的直接证据不足，因此建议保守推进而不是直接进入 Offer 阶段。",
  "interviewRoundSummaries": [
    {
      "round": 1,
      "interviewer": "李面试官",
      "score": 75,
      "verdict": "基础能力通过",
      "positives": ["基础知识较扎实", "表达清晰"],
      "negatives": ["项目细节展开不足"]
    }
  ]
}
```

## 6. 联调说明

### 6.1 Python

- `parse_pdf_langchain` 下实现独立 LangChain 服务
- 通过结构化输出直接约束 JSON
- 保持对 Java 旧接口路径兼容

### 6.2 Java

当前 Java 侧已有兼容对象：

- `ParseReportResponse`
- `DecisionReportResponse`
- `ParsedCandidateDraftResponse`

新服务输出保持这些结构可直接反序列化，避免 Java 调度链路大改。

### 6.3 前端

当前前端详情页和辅助决策卡片主要消费：

- `parseReport.fields`
- `parseReport.extractedSkills`
- `parseReport.projectExperiences`
- `decisionReport.conclusion`
- `decisionReport.recommendedAction`
- `decisionReport.strengths`
- `decisionReport.risks`
- `decisionReport.missingInformation`
- `decisionReport.supportingEvidence`
- `decisionReport.reasoningSummary`

本次将前端类型层补充：

- `optimizationSuggestions`
- `interviewRoundSummaries`

并将“缺失信息 / 待补问”展示调整为：

- `missingInformation`
- `optimizationSuggestions`

## 7. 实施范围

主要实现目录：

- `parse_pdf_langchain/src/config.py`
- `parse_pdf_langchain/src/schemas.py`
- `parse_pdf_langchain/src/prompts/`
- `parse_pdf_langchain/src/chains/`
- `parse_pdf_langchain/src/services/`
- `parse_pdf_langchain/src/api/`
- `parse_pdf_langchain/src/utils/`
- `parse_pdf_langchain/src/main.py`
- `parse_pdf_langchain/tests/`

必要时补充前端类型和展示兼容，但不重写当前页面交互逻辑。
