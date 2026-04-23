# 辅助决策实现说明

本文说明当前“辅助决策”功能在项目中的实际实现方式，包括前端交互、后端任务流、结果持久化、评分逻辑和当前限制。

## 1. 功能目标

辅助决策不是一个单独页面，而是候选人详情页中的一个主操作。

目标是：

- 结合候选人当前流程状态
- 结合最新简历解析结果
- 结合部门反馈
- 结合面试安排与面试评价
- 生成一份可保存、可回看、可重复触发的决策建议

当前实现采用“保留历史 + 默认展示最新”的策略。

## 2. 前端是怎么触发的

入口在候选人详情页：

- 简历区域按钮：`辅助决策`
- 最近一次辅助决策卡片中的“查看完整结果与历史”
- 弹窗底部按钮：`生成新的辅助决策`

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:381`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:499`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:983`

候选人详情页在加载时会同时取：

- 候选人详情
- 时间线
- 部门反馈
- 面试记录
- 最新简历解析结果
- 辅助决策历史

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:107`

## 3. 前端弹窗交互逻辑

弹窗使用现有 `Dialog` 组件实现，主要分成两列：

- 左侧：当前选中决策记录的完整内容
- 右侧：历史记录列表

弹窗内容包括：

- 决策关注点输入框
- 推荐等级与评分
- 最新结论
- 推荐动作
- 关键优势
- 关键风险
- 缺失信息 / 待补问
- 分析依据
- 推理摘要
- 历史记录切换

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:846`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:865`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:950`

点击“生成新的辅助决策”后的前端流程：

1. 打开弹窗
2. 调用创建决策任务接口
3. 轮询最新决策任务
4. 成功后重新拉取历史
5. 默认选中刚生成的最新记录

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:212`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\pages\CandidateDetail.tsx:248`

## 4. 前端调用了哪些接口

前端数据层在 `DataContext` 中封装了辅助决策相关接口：

- 创建决策任务：`POST /api/candidates/{id}/decision-jobs`
- 查询最新结果：`GET /api/candidates/{id}/decision-jobs/latest`
- 查询历史结果：`GET /api/candidates/{id}/decision-jobs`

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:597`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:607`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:612`

前端数据模型里也单独定义了决策结果结构：

- `DecisionReport`
- `AgentJob`
- `AgentJobResult`

相关实现：

- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:199`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:211`
- `D:\claude_program\Codex\云测作业\招聘流程协同平台后台\src\app\context\DataContext.tsx:224`

## 5. 后端接口是怎么设计的

后端在 `AgentController` 中单独定义了决策任务接口，没有复用解析接口：

- 创建任务
- 查询最新
- 查询历史
- 内部回调写入结果

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentController.java:38`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentController.java:43`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentController.java:48`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentController.java:71`

任务类型也单独扩展了 `DECISION`，和 `PARSE` / `ANALYSIS` 分开：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\common\enums\AgentJobType.java:3`

## 6. 创建辅助决策时，后端收集了哪些输入

`AgentService.createDecisionJob(...)` 会收集当前候选人的完整上下文后再创建任务。

输入包括：

- 候选人 ID
- 决策关注点 `focusHint`
- 当前状态 `statusCode` / `statusLabel`
- 岗位信息 `position`
- 下一步动作 `nextAction`
- 所属部门
- 简历访问地址
- 简历对象信息
- 最新简历解析结果
- 全部部门反馈
- 全部面试安排
- 全部面试评价

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:97`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:113`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:125`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:126`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:127`

如果没有最新简历解析结果，后端不会中断，而是退回到候选人已录入资料构造一个兜底版 `parseReport`。

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:286`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:293`

## 7. 当前“辅助决策 Agent”是怎么执行的

当前版本默认不是调用外部大模型，而是走本地可插拔 mock dispatcher：`LoggingAgentDispatcher`。

也就是说：

- 前后端接口已经按 Agent 任务流建好
- 当前本地运行时由 `LoggingAgentDispatcher` 直接自动完成决策结果
- 后续如果接入真实外部 Agent，可以保持前端接口不变，只替换 dispatcher / callback 流程

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:43`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:64`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:107`

## 8. 决策结果是怎么计算出来的

当前 mock 决策逻辑在 `buildDecisionReport(...)` 中完成，主要分四部分：

### 8.1 先聚合证据

它会分别从这些来源提取信息：

- 简历解析出的技能标签
- 简历解析出的项目经历
- 部门反馈条数、通过数、反馈内容、拒绝原因
- 面试评价条数、平均分、亮点、弱项
- 简历解析里的告警问题

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:266`

### 8.2 生成四类内容

最终会生成：

- `strengths`：关键优势
- `risks`：关键风险
- `missingInformation`：缺失信息 / 待补充
- `supportingEvidence`：分析依据

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:273`

### 8.3 计算评分

会生成 4 个维度分数：

- `resumeReadiness`
- `feedbackConfidence`
- `interviewConfidence`
- `overallRecommendation`

总分权重是：

- 简历准备度 35%
- 部门反馈置信度 25%
- 面试置信度 40%

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:335`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:344`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:373`

### 8.4 根据分数映射推荐等级

当前等级规则：

- `>= 85`：强烈推荐
- `>= 70`：建议推进
- `>= 55`：保守推进
- `< 55`：暂缓

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:345`

## 9. 推荐动作是怎么给出的

推荐动作并不是固定一句，而是结合当前候选人流程状态来给。

例如：

- 新建 / 部门处理中：先完成部门筛选
- 待面试：建议安排下一轮面试
- 面试中：建议等待结果或补齐评价
- 面试通过：建议推进 Offer 或安排终面
- Offer 待发：建议准备 Offer 方案
- Offer 已发：建议跟进候选人确认
- 已录用：建议沉淀录用经验
- 已淘汰：建议归档候选人并沉淀原因

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:350`

## 10. 决策结果怎么持久化

每次点击“生成新的辅助决策”，都会新建一条 `AgentJob`。

对应结果会写入 `AgentResult`，其中决策专用内容保存在：

- `decisionReportJson`
- `summary`
- `overallScore`
- `dimensionScoresJson`
- `strengths`
- `risks`
- `recommendedAction`
- `rawReasoningDigest`

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:227`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:170`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentResult.java:18`

历史保留的关键点有两个：

- 仓库查询按 `candidateId + jobType=DECISION + createdAt desc`
- 前端历史列表直接展示所有决策 job

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentJobRepository.java:14`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentJobRepository.java:17`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:148`

## 11. 返回给前端的结构

后端 DTO 中把决策结果单独定义成 `DecisionReportResponse`，字段包括：

- `conclusion`
- `recommendationScore`
- `recommendationLevel`
- `recommendedAction`
- `strengths`
- `risks`
- `missingInformation`
- `supportingEvidence`
- `reasoningSummary`

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentDtos.java:81`

这也是前端弹窗直接渲染的核心数据结构。

## 12. 结果一致性与回退逻辑

当前实现里，为了保证辅助决策在资料不完整时也能工作，做了几层兜底：

- 没有解析结果：用候选人资料拼一个 fallback parse report
- 没有面试评价：允许出结论，但会补“依据不足”
- 没有部门反馈：允许出结论，但会提示先补部门筛选
- 没有简历附件：仍然可生成，但会明确提示依据不足

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\AgentService.java:293`
- `D:\claude_program\Codex\云测作业\backend\src\main\java\com\recruit\platform\agent\LoggingAgentDispatcher.java:393`

## 13. 当前测试覆盖

已经有自动化测试验证：

- 连续两次触发辅助决策会保留两条历史
- `/decision-jobs/latest` 能拿到最新结果
- 结果中包含 `conclusion` 和 `recommendedAction`

相关实现：

- `D:\claude_program\Codex\云测作业\backend\src\test\java\com\recruit\platform\RecruitPlatformApplicationTests.java:326`

## 14. 当前版本的特点与限制

### 当前特点

- 前后端链路完整
- 任务模型独立
- 支持历史保留
- 支持基于最新简历解析结果做决策
- 支持关注点输入
- 支持未来无缝替换为真实外部 Agent

### 当前限制

- 本地默认是规则计算 + mock dispatcher，不是真实大模型推理
- 评分逻辑目前是启发式规则，不是训练模型
- 暂未做多角色差异化展示
- 暂未展示多次决策结果之间的字段级 diff

## 15. 后续如果要继续增强，建议从这几步开始

1. 将 `LoggingAgentDispatcher` 替换成真实 Agent 调度实现
2. 为决策结果增加“引用来源片段”，提升可解释性
3. 增加不同岗位维度权重配置
4. 增加历史决策对比视图
5. 将评分规则抽到独立策略层，便于运营调整
