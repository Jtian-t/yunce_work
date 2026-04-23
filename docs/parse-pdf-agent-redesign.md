# parse_pdf Agent 化重构设计

## 背景

当前 `parse_pdf` 只能接收 `resume_text`，再通过固定 prompt 调用大模型输出简历解析结果和岗位建议。它具备调用大模型的能力，但并不是真正意义上的 agent，主要问题有：

- 不能直接解析真实 PDF
- 没有 OCR 兜底
- 没有工具注册与执行层
- 没有中间状态、证据链和运行轨迹
- 多轮面试反馈只是普通输入拼接，缺少独立汇总与决策步骤

本次重构的目标，是把它升级为一个可落地的 sidecar agent：

- 输入真实 PDF、纯文本、文件路径或文件 URL
- 自动判断是否需要 OCR
- 通过工具型单代理逐步完成解析与决策
- 输出结构化 `ParseReport` 与 `DecisionReport`
- 保留旧接口兼容现有系统

## 技术选型

### PDF 解析

- 主方案：`PyMuPDF`
- 原因：
  - 文本层提取稳定
  - 能获取页级 block 和坐标
  - 能渲染页面，便于 OCR fallback

### OCR

- 方案：`rapidocr_onnxruntime`
- 原因：
  - Windows 友好
  - 不依赖 Tesseract 或 Poppler
  - 支持中文文档识别

### Agent 编排

- 方案：工具型单代理
- 原因：
  - 比固定 prompt 更灵活
  - 比多代理更轻量，适合当前项目
  - 不依赖豆包原生 function calling

## 运行架构

### Agent Runtime

新增 `src/agent_runtime/`：

- `actions.py`：结构化动作定义
- `context.py`：解析上下文与决策上下文
- `tool_registry.py`：工具注册表
- `runner.py`：单代理行动循环

### 工具列表

新增 `src/tools/`：

- `load_resume_source`
- `extract_pdf_blocks`
- `detect_ocr_need`
- `ocr_pdf_pages`
- `normalize_resume_sections`
- `extract_candidate_profile`
- `aggregate_interview_feedback`
- `score_job_fit`
- `generate_optimization_suggestions`

### 执行策略

运行时优先尝试让 LLM 选择下一步动作；如果输出非法、模型失败，或当前状态只有一个合理动作，则回退到确定性执行顺序。

解析链路默认顺序：

1. 加载输入源
2. 提取 PDF 文本块
3. 判断是否需要 OCR
4. OCR 兜底
5. 归一化简历分段
6. 生成结构化候选人画像

决策链路默认顺序：

1. 汇总多轮面试反馈
2. 评估岗位匹配度
3. 生成优化建议与最终决策报告

## 接口设计

### 兼容旧接口

- `POST /api/resume/parse`
- `POST /api/resume/analyze`

### 新增接口

- `POST /api/resume/parse-report`
- `POST /api/resume/parse-report/upload`
- `POST /api/resume/decision-report`

## 数据模型

### ParseReport

核心字段：

- `summary`
- `highlights`
- `extractedSkills`
- `projectExperiences`
- `skills`
- `projects`
- `experiences`
- `educations`
- `rawBlocks`
- `fields`
- `issues`
- `extractionMode`
- `ocrRequired`

### DecisionReport

核心字段：

- `conclusion`
- `recommendationScore`
- `recommendationLevel`
- `recommendedAction`
- `strengths`
- `risks`
- `missingInformation`
- `supportingEvidence`
- `reasoningSummary`
- `optimizationSuggestions`
- `interviewRoundSummaries`

### 兼容映射

- `ParseReport -> CandidateInfo`
- `DecisionReport -> AnalysisResult`

## PDF 解析策略

1. 如果输入是纯文本，直接进入文本解析模式。
2. 如果输入是 PDF，优先使用 `PyMuPDF` 获取页文本和 block。
3. 若文本过短、可读性差、页面近似空白，则标记 `ocrRequired=true`。
4. 若可用，调用 `rapidocr_onnxruntime` 对页面图像做 OCR。
5. 解析结果保留证据：
   - `rawBlocks`
   - `fields`
   - `issues`
   - `extractionMode`
   - `ocrRequired`

## 已实现的规则增强

为了让真实 PDF 简历解析更稳定，当前版本增加了规则兜底：

- 优先识别 `姓名`、`邮箱`、`手机号码` 等标签字段
- 用正则补充手机号、邮箱
- 用启发式提取教育经历
- 对技能和项目做轻量规则补全
- 在模型输出缺失时，用规则结果兜底

## backend 接入计划

Java backend 的目标接入方式如下：

- parse 阶段优先调用 `/api/resume/parse-report`
- decision 阶段调用 `/api/resume/decision-report`
- 请求中优先传 `resume_file_url`，保留 `resume_text` 作为 fallback
- 结果持久化时保存 `parseReportJson` 与 `decisionReportJson`

说明：

当前工作区的 backend 存在额外的历史脏改动和编码损坏文件，已经影响 Maven 编译。Python sidecar 已经完成并通过验证，但 backend 侧需要先清理已有损坏后再统一收口。

## 验证结果

本轮已验证：

- 纯文本简历解析可生成 `ParseReport`
- 决策链路可生成 `DecisionReport`
- 真实 PDF `金天祥java后端开发简历.pdf` 可直接解析
- FastAPI 新接口可通过 `TestClient` 返回 200

已知限制：

- 某些复杂 PDF 的项目和技能抽取仍有提升空间
- 豆包结构化输出对超时较敏感，因此默认超时已提高
- backend 当前工作区存在独立编译问题，未在本轮彻底清理

## 下一步

建议继续推进：

1. 优化项目经历与技能块的规则提取
2. 为扫描件 PDF 补充 OCR 回归样例
3. 清理 backend 现有编码损坏文件后，再完成 Java 侧联调与提交
