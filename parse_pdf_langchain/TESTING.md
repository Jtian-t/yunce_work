# parse_pdf_langchain 测试文档

本文档说明如何验证 `parse_pdf_langchain` 的简历解析与辅助决策能力，包括：

- 本地自动化测试
- 服务启动验证
- 接口手动测试
- Java / 前端联调验证

## 1. 测试目标

本轮测试重点确认以下内容：

1. `parse_pdf_langchain` 服务可以正常启动
2. 简历解析接口返回精简且稳定的 JSON
3. 返回的 `parse_report` 兼容 Java 当前消费字段
4. 决策接口可以结合岗位要求和面试反馈返回辅助决策 JSON
5. 前端辅助决策卡片可以展示：
   - 最新结论
   - 推荐动作
   - 关键优势
   - 关键风险
   - 缺失信息 / 待补问
   - 分析依据
   - 推理摘要
   - 面试轮次摘要

## 2. 测试前准备

### 2.1 Python 环境

当前建议使用 Conda 环境：

- `parse_pdf_clean`

先激活环境：

```powershell
conda activate parse_pdf_clean
```

如需重新安装依赖，在仓库根目录执行：

```powershell
python -m pip install -r D:\claude_program\Codex\云测作业\parse_pdf_langchain\requirements.txt
```

### 2.2 LLM 配置

`parse_pdf_langchain` 会优先读取：

1. [parse_pdf_langchain/.env](D:/claude_program/Codex/云测作业/parse_pdf_langchain/.env)
2. 回退读取 [parse_pdf/.env](D:/claude_program/Codex/云测作业/parse_pdf/.env)

如果你想为新服务单独配置，可以复制：

- [parse_pdf_langchain/.env.example](D:/claude_program/Codex/云测作业/parse_pdf_langchain/.env.example)

示例：

```powershell
Copy-Item D:\claude_program\Codex\云测作业\parse_pdf_langchain\.env.example D:\claude_program\Codex\云测作业\parse_pdf_langchain\.env
```

然后填写：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL` 或 `LLM_ENDPOINT_ID`

## 3. 自动化测试

### 3.1 运行单元测试

在仓库根目录执行：

```powershell
$env:PYTHONPATH='D:\claude_program\Codex\云测作业\parse_pdf_langchain'
python -m pytest D:\claude_program\Codex\云测作业\parse_pdf_langchain\tests -q
```

预期结果：

- 输出 `4 passed`
- 无失败用例

当前测试覆盖：

- `parse_report` 是否保留 Java 兼容字段
- `ParseReport -> ResumeProfile` 映射是否正确
- `/api/resume/parse-report` 路由是否返回正确结构
- `/api/resume/decision-report` 路由是否返回正确结构

测试文件：

- [test_schema_mapping.py](D:/claude_program/Codex/云测作业/parse_pdf_langchain/tests/test_schema_mapping.py)
- [test_api.py](D:/claude_program/Codex/云测作业/parse_pdf_langchain/tests/test_api.py)

## 4. 服务启动测试

### 4.1 启动服务

在仓库根目录执行：

```powershell
$env:PYTHONPATH='D:\claude_program\Codex\云测作业\parse_pdf_langchain'
python -m uvicorn src.main:app --host 0.0.0.0 --port 8002 --reload
```

启动成功后访问：

- [http://localhost:8002/health](http://localhost:8002/health)
- [http://localhost:8002/docs](http://localhost:8002/docs)

预期：

- `/health` 返回 `{"status":"ok"}`
- `/docs` 可以打开 Swagger 页面

## 5. 简历解析接口手测

### 5.1 测试 `/api/resume/parse-report`

可以直接在 Swagger 里调，也可以用 PowerShell：

```powershell
$body = @{
  resume_text = @"
张三
应聘岗位：Java后端工程师
电话：13800138000
邮箱：candidate@example.com
本科，计算机科学与技术
3年后端开发经验
现居上海
技能：Java、Spring Boot、MySQL、Redis、RabbitMQ
项目：参与电商后台开发，负责订单接口与缓存优化。
"@
  hint = "简历上传"
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri "http://localhost:8002/api/resume/parse-report" -Method Post -ContentType "application/json" -Body $body
```

重点检查：

- `summary` 存在且为简短总结
- `fields.name / phone / email / location / education / experience / skillsSummary / projectSummary` 存在
- `fields.experienceYears`、`fields.targetPosition`、`fields.source` 如有值也应正常返回
- `extractedSkills` 为精简关键词数组
- `projectExperiences` 为精简项目摘要
- 没有大段原文直接复制进 `summary`、`skillsSummary`、`projectSummary`

### 5.2 测试 `/api/resume/parse-report/upload`

使用仓库内样例 PDF：

- [金天祥java后端开发简历.pdf](D:/claude_program/Codex/云测作业/parse_pdf_langchain/金天祥java后端开发简历.pdf)

PowerShell 示例：

```powershell
$filePath = "D:\claude_program\Codex\云测作业\parse_pdf_langchain\金天祥java后端开发简历.pdf"
Invoke-RestMethod -Uri "http://localhost:8002/api/resume/parse-report/upload" -Method Post -Form @{ file = Get-Item $filePath }
```

重点检查：

- PDF 能正常被抽取文本
- `ocrRequired` 合理返回 `false` 或 `true`
- 输出仍然保持精简 JSON，不臃肿

## 6. 辅助决策接口手测

### 6.1 测试 `/api/resume/decision-report`

示例请求：

```powershell
$body = @{
  job_requirements = "招聘 Java 后端工程师，要求熟悉 Spring Boot、MySQL、Redis、消息队列，有中后台业务开发经验。"
  resume_profile = @{
    name = "张三"
    targetPosition = "Java后端工程师"
    phone = "13800138000"
    email = "candidate@example.com"
    education = "本科，计算机科学与技术"
    experienceYears = "3年"
    location = "上海"
    source = "简历上传"
    skillsSummary = "熟悉 Java、Spring Boot、MySQL、Redis、RabbitMQ，具备常见后端开发经验。"
    projectSummary = "参与电商后台开发，负责订单接口与缓存优化。"
    skillKeywords = @("Java", "Spring Boot", "MySQL", "Redis", "RabbitMQ")
    projectHighlights = @("参与电商后台开发，负责订单接口与缓存优化。")
  }
  interview_feedbacks = @(
    @{
      round = 1
      interviewer = "李面试官"
      feedback = "基础不错，但项目深度展开不够。"
      score = 75
      pros = @("基础扎实", "沟通清晰")
      cons = @("项目深度不足")
    },
    @{
      round = 2
      interviewer = "王面试官"
      feedback = "系统设计一般，建议补问高并发场景。"
      score = 68
      pros = @("常规开发经验完整")
      cons = @("高并发场景经验不足")
    }
  )
} | ConvertTo-Json -Depth 8

Invoke-RestMethod -Uri "http://localhost:8002/api/resume/decision-report" -Method Post -ContentType "application/json" -Body $body
```

重点检查：

- `conclusion` 存在，且是业务结论
- `recommendedAction` 是明确动作建议
- `strengths`、`risks`、`missingInformation`、`supportingEvidence` 为字符串数组
- `optimizationSuggestions` 返回待补问或后续建议
- `reasoningSummary` 为简洁摘要
- `interviewRoundSummaries` 与输入的面试反馈轮次一致

## 7. Java 联调验证

Java 当前重点消费以下字段：

- `parseReport.fields.name`
- `parseReport.fields.phone`
- `parseReport.fields.email`
- `parseReport.fields.location`
- `parseReport.fields.education`
- `parseReport.fields.experience`
- `parseReport.fields.skillsSummary`
- `parseReport.fields.projectSummary`
- `decisionReport.conclusion`
- `decisionReport.recommendedAction`
- `decisionReport.strengths`
- `decisionReport.risks`
- `decisionReport.missingInformation`
- `decisionReport.supportingEvidence`
- `decisionReport.reasoningSummary`

联调时重点确认：

1. Java `RemoteAgentDispatcher` 调新服务路径后可以正常反序列化
2. Java 侧候选人草稿字段可以自动回填
3. 决策结果可以保存并返回给前端

建议联调顺序：

1. 先独立启动 `parse_pdf_langchain`
2. 再启动 Java 后端
3. 通过候选人详情页触发“简历解析”和“辅助决策”
4. 检查 Java 返回的 `parseReport` / `decisionReport` JSON

## 8. 前端验证

当前前端已补充以下兼容：

- `optimizationSuggestions`
- `interviewRoundSummaries`

需要在候选人详情页确认：

1. “关键优势”展示 `strengths`
2. “关键风险”展示 `risks`
3. “缺失信息 / 待补问”同时展示：
   - `missingInformation`
   - `optimizationSuggestions`
4. “分析依据”展示 `supportingEvidence`
5. “推理摘要”展示 `reasoningSummary`
6. “面试轮次摘要”展示 `interviewRoundSummaries`

## 9. 推荐执行顺序

建议按下面顺序测试：

1. 跑自动化测试
2. 启动 `parse_pdf_langchain`
3. 手测 `/health` 和 `/docs`
4. 手测 `/api/resume/parse-report`
5. 手测 `/api/resume/parse-report/upload`
6. 手测 `/api/resume/decision-report`
7. 启动 Java 后端做联调
8. 打开前端页面验证展示

## 10. 常见问题

### 10.1 `No module named ...`

说明依赖未安装完整，重新执行：

```powershell
conda activate parse_pdf_clean
python -m pip install -r D:\claude_program\Codex\云测作业\parse_pdf_langchain\requirements.txt
```

### 10.2 LLM 配置报错

检查：

- `parse_pdf_langchain/.env`
- [parse_pdf/.env](D:/claude_program/Codex/云测作业/parse_pdf/.env:1)

是否至少提供了：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL` 或 `LLM_ENDPOINT_ID`

### 10.3 返回内容过长

这类问题优先检查：

- 提示词是否被改动
- `skillsSummary` / `projectSummary` 是否被下游又拼接了原文
- 输入简历文本是否包含大量重复 OCR 内容
