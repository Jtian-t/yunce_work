# parse_pdf Agent 设计说明

## 1. 设计目标

`parse_pdf` 的目标不是单纯“调一次大模型”，而是把简历解析和岗位决策拆成一组可执行工具，由一个轻量级 agent 按步骤推进。

这个 agent 需要解决四类问题：

1. 输入源不固定
   - 可能是纯文本
   - 可能是本地 PDF
   - 可能是远程文件 URL

2. PDF 质量不固定
   - 可能是文本型 PDF
   - 可能是需要 OCR 的扫描件

3. 输出不只是一段话
   - 要得到结构化 `ParseReport`
   - 要得到结构化 `DecisionReport`

4. 大模型并不总是稳定
   - 可能超时
   - 可能输出非 JSON
   - 可能选错下一步工具

所以这里采用的是“工具型单代理”方案，而不是固定 prompt 链。

## 2. 整体架构

代码主要分成四层：

1. API 层
   - 文件位置：`parse_pdf/src/api/resume.py`
   - 对外提供 `/api/resume/parse`、`/api/resume/analyze`、`/api/resume/parse-report`、`/api/resume/decision-report`

2. Service 层
   - 文件位置：`parse_pdf/src/services/resume_service.py`
   - 负责拼装上下文，调用 agent runner，返回结果

3. Agent Runtime 层
   - 文件位置：`parse_pdf/src/agent_runtime/`
   - 负责工具注册、动作选择、执行循环、状态维护

4. Tool 层
   - 文件位置：`parse_pdf/src/tools/`
   - 每个工具只做一件事，agent 通过它们逐步完成任务

## 3. Agent Runtime 设计

### 3.1 核心对象

#### `ToolRegistry`

文件：

- `parse_pdf/src/agent_runtime/tool_registry.py`

作用：

- 注册工具名、工具描述、工具处理函数
- 按名称执行工具
- 提供工具描述给 LLM 选择下一步动作

#### `ParseAgentContext`

文件：

- `parse_pdf/src/agent_runtime/context.py`

作用：

- 保存简历解析过程中的中间状态

关键字段：

- `request`：输入请求
- `source`：统一后的简历源
- `blocks`：抽取出的内容块
- `sections`：分段结果
- `fields`：规则识别出的字段证据
- `issues`：解析过程中发现的问题
- `candidate_info`：结构化候选人信息
- `parse_report`：最终解析报告
- `metadata`：执行过程中的附加状态，如是否做过 OCR 判断
- `step_logs`：执行轨迹

#### `DecisionAgentContext`

文件：

- `parse_pdf/src/agent_runtime/context.py`

作用：

- 保存岗位决策过程中的中间状态

关键字段：

- `candidate_info`
- `parse_report`
- `feedbacks`
- `feedback_aggregate`
- `decision_report`
- `metadata`
- `step_logs`

#### `AgentAction`

文件：

- `parse_pdf/src/agent_runtime/actions.py`

作用：

- 表示 agent 每一步选择的动作

关键字段：

- `tool_name`
- `reason`
- `done`

### 3.2 Runner 设计

文件：

- `parse_pdf/src/agent_runtime/runner.py`

当前有两个 runner：

1. `ParseAgentRunner`
2. `DecisionAgentRunner`

它们的工作方式一致：

1. 初始化时注册一组工具
2. 进入循环
3. 选择下一步动作
4. 执行工具
5. 把结果写回 context
6. 如果最终结果已产生，就提前结束

### 3.3 动作选择机制

当前动作选择是“两段式”：

1. 优先尝试让 LLM 选择下一步工具
2. 如果 LLM 失败、输出非法、或者当前只有一个合理工具，则走 fallback

这样设计的原因是：

- 给 agent 保留灵活性
- 同时避免因为模型不稳定让主流程完全失控

## 4. 定义了哪些工具

### 4.1 简历解析工具

#### `load_resume_source`

作用：

- 把 `resume_text`、`resume_file_path`、`resume_file_url` 统一成内部的 `ResumeSource`

输出：

- `context.source`

#### `extract_pdf_blocks`

作用：

- 如果是纯文本，直接生成一个文本块
- 如果是 PDF，用 `PyMuPDF` 提取页面文本和 block

输出：

- `context.blocks`
- `context.extraction_mode`

#### `detect_ocr_need`

作用：

- 根据提取文本质量判断是否需要 OCR

典型判断依据：

- 文本过少
- 页面接近空白
- 可读字符比例低

输出：

- `context.ocr_required`
- `context.metadata["ocr_checked"]`

#### `ocr_pdf_pages`

作用：

- 当文本 PDF 抽取质量较差时，用 `rapidocr_onnxruntime` 做 OCR 兜底

输出：

- 更新 `context.blocks`
- `context.metadata["ocr_completed"]`

#### `normalize_resume_sections`

作用：

- 按“教育背景 / 项目经验 / 专业技能 / 自我评价”等关键词分段
- 用规则先提取手机号、邮箱、姓名等字段

输出：

- `context.sections`
- `context.fields`
- `context.highlights`
- `context.issues`

#### `extract_candidate_profile`

作用：

- 调用大模型生成 `CandidateInfo`
- 再用规则兜底修正关键字段
- 组装最终 `ParseReport`

输出：

- `context.candidate_info`
- `context.parse_report`

### 4.2 决策分析工具

#### `aggregate_interview_feedback`

作用：

- 汇总多轮面试反馈
- 提炼优点、缺点、缺失信息
- 生成每一轮的摘要

输出：

- `context.feedback_aggregate`

#### `score_job_fit`

作用：

- 结合岗位要求、简历和面试反馈，生成岗位匹配评分

输出：

- `context.metadata["fit_score"]`

#### `generate_optimization_suggestions`

作用：

- 在评分基础上产出最终建议
- 生成 `DecisionReport`

输出：

- `context.decision_report`

## 5. 执行流程是什么样的

### 5.1 简历解析流程

标准流程：

1. `load_resume_source`
2. `extract_pdf_blocks`
3. `detect_ocr_need`
4. `ocr_pdf_pages`（如果需要）
5. `normalize_resume_sections`
6. `extract_candidate_profile`

最终得到：

- `ParseReport`

### 5.2 岗位决策流程

标准流程：

1. `aggregate_interview_feedback`
2. `score_job_fit`
3. `generate_optimization_suggestions`

最终得到：

- `DecisionReport`

## 6. 故障处理怎么做

这个 agent 的设计重点之一，就是“即使某一层不稳定，也尽量不要整条链路直接崩掉”。

### 6.1 LLM 选工具失败

场景：

- 模型输出不是合法 JSON
- 模型选了不存在的工具
- 模型选了当前状态下不该执行的工具

处理方式：

- 直接回退到 runner 内置的 fallback sequence

这部分逻辑在：

- `ParseAgentRunner._choose_next_action`
- `DecisionAgentRunner._choose_next_action`

### 6.2 工具前置条件不满足

场景：

- 还没加载源就去抽块
- 还没分段就去抽结构化字段

处理方式：

- 用 `_is_viable(...)` 和 `_fallback_action(...)` 做保护
- agent 不会盲目执行顺序错误的工具

### 6.3 OCR 不可用

场景：

- OCR 依赖没安装
- OCR 运行时报错

处理方式：

- 记录 issue / warning
- 保留已有文本提取结果
- 尽量不让整个流程直接失败

### 6.4 PDF 解析依赖缺失

场景：

- `PyMuPDF` 没装

处理方式：

- 抛出明确错误
- 同时在上下文 issue 中记录原因

### 6.5 大模型超时或上游异常

场景：

- 豆包返回超时
- 网络失败
- endpoint 不存在

处理方式：

- `llm_client.py` 统一捕获
- 抛出带 `base_url` 和 `model` 的明确错误
- 对 JSON 模式不支持的模型自动降级为普通对话输出

### 6.6 最终结果未生成

场景：

- 多步执行后仍没有 `parse_report` 或 `decision_report`

处理方式：

- runner 在退出前会强制执行最后一个核心工具
- 若仍失败，再抛出明确 `RuntimeError`

## 7. 当前设计的优点

1. 相比固定 prompt，更接近真实 agent
2. 每个工具职责单一，方便替换和扩展
3. 有上下文和执行轨迹，便于排错
4. 有 fallback，不完全依赖模型稳定性
5. 同时兼容旧接口和新接口

## 8. 当前已知局限

1. 目前还是单代理，不是多代理协作
2. 项目经历和工作经历的规则提取仍有继续优化空间
3. OCR 分支虽然已接入，但扫描件样本回归还不够充分
4. LLM 规划工具选择的价值目前不算特别高，主流程仍以 fallback sequence 为主

## 9. 后续可优化方向

1. 给 `ParseReport` 增加字段级置信度追踪
2. 增加更多版式样本的规则库
3. 为扫描 PDF 增加专门测试集
4. 把项目经历识别拆成独立工具
5. 给 runner 增加更详细的运行审计日志
