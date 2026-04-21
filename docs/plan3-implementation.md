# 计划3实施文档与全量实施方案

## Summary

采用“保留 `plan3.md` 原方案不动，新增一份实施文档”的方式推进。新文档用于承接当前代码现状、差距分析、分批任务、接口变更和验收标准，避免 `plan3.md` 同时承担“愿景方案”和“执行清单”两个角色。

实施范围按“全量按计划3”执行，但落地时仍按依赖顺序分阶段推进，避免并行改动过多导致接口和页面来回返工。

## 当前现状摘要

- 已有能力
  - 候选人 CRUD
  - 详情页展示
  - 面试安排/评价
  - 基础通知落库
  - 简历解析任务
  - 辅助决策任务
- 当前缺口
  - 详情不可编辑
  - 解析结果不可回填
  - 面试无会议信息
  - 无面试官工作台
  - 无通知 API/页面
  - 部门页不是工作台
  - 解析仍是 mock 级

## 目标拆解

- P0
  - 候选人详情编辑
  - 解析回填
  - 面试会议字段
  - 结构化通知
- P1
  - 面试官工作台
  - 流程校验
  - 多轮面试进展
  - 部门工作台
- P2
  - 简历解析链路与结果模型升级
  - 人工修正机制
  - 解析服务扩展点

## 接口与数据模型变更

- `InterviewPlan`
  - 增加会议信息
  - 增加轮次编码
  - 增加组织人
  - 增加部门归属
- `Notification`
  - 增加通知类型
  - 增加扩展 JSON
- 解析结果
  - 增加结构化 JSON 字段
- API
  - `GET /api/interviews/mine`
  - 通知查询/已读
  - 解析结果应用/人工修正接口

## 页面改造清单

- `CandidateDetail`
- Department 工作台
- Interviewer “我的面试”
- 通知面板/页面

## 全量实施顺序

### 第一阶段：先补主流程闭环

- 候选人详情页增加“编辑基础信息”入口，复用现有 `PUT /api/candidates/{id}`。
- 在详情页增加解析结果字段回填能力，先实现字段级一键带入编辑表单。
- 扩展面试安排模型、DTO、接口、前端表单，支持会议类型、会议链接、会议号、密码、备注。
- 扩展通知模型与服务，新增 `INTERVIEW_ASSIGNED`，面试创建时写入结构化通知。
- 提供通知查询与已读 API，并把前端假通知角标换成真实数据。

### 第二阶段：补部门与面试官视角

- 新增 `GET /api/interviews/mine`，返回当前面试官的面试任务列表。
- 新增“我的面试”页面与导航入口，支持查看会议链接、候选人详情、评价状态。
- 将部门页从“待处理列表”升级成“部门工作台”，按候选人阶段分组展示。
- 在候选人详情页增加“面试进展”卡片，以轮次显示已安排、已完成、已评价状态。

### 第三阶段：补流程规则

- 在候选人推进逻辑中增加校验：
  - 上一轮未完成不允许推进下一轮
  - 已安排但未提交评价时，推进 Offer 需拦截或强提示
- 明确面试状态与候选人状态的关系，避免“评价未交但候选人状态已前移”。
- 确保部门工作台、我的面试、候选人详情三处看到的是同一套轮次状态。

### 第四阶段：升级简历解析

- 先重构当前解析代码为分阶段 pipeline，而不是直接堆在 `LoggingAgentDispatcher` 内。
- 新增结构化字段模型：
  - 技能标签数组
  - 项目经历数组
  - 工作经历数组
  - 教育经历数组
  - 原始块数组
- 项目经历拆分为 `projectName/period/role/techStack/responsibilities/achievements/summary`。
- 技能项保留标准名、原始命中词、来源片段、置信度。
- 新增人工修正与建议值分离接口：
  - `POST /api/candidates/{id}/parsed-profile/apply`
  - `PUT /api/candidates/{id}/parsed-profile/manual-fields`
- 为扫描 PDF 预留 OCR 分支与外部解析服务接入点；当前批次不强依赖第三方 OCR 落地，但代码结构必须可扩展。

## Public API / Interface Changes

- `POST /api/interviews`
  - 请求增加：
    - `meetingType`
    - `meetingUrl`
    - `meetingId`
    - `meetingPassword`
    - `interviewStageCode`
    - `interviewStageLabel`
    - `departmentId`
- `GET /api/interviews/mine`
  - 返回当前面试官的面试列表与评价状态
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `POST /api/candidates/{id}/parsed-profile/apply`
- `PUT /api/candidates/{id}/parsed-profile/manual-fields`

## Test Plan

- 候选人详情可编辑基础信息，保存后详情与列表同步刷新。
- 解析结果字段可以回填到编辑表单，保存后保留人工值。
- 创建面试时能保存并返回会议字段。
- 面试官收到 `INTERVIEW_ASSIGNED` 通知，并能在“我的面试”页看到对应任务。
- 通知 API 可查询、已读状态可更新。
- 若上一轮面试未完成或未评价，流程推进受到校验。
- 部门工作台能按阶段查看候选人，并看到相关轮次与结果。
- 解析结果返回新的结构化模型，文本 PDF 仍可正常解析；扫描 PDF 至少进入可识别的待 OCR/扩展分支。

## Assumptions

- 新实施文档使用 `docs/plan3-implementation.md`，不覆盖 `plan3.md`。
- 全量实施不代表一次性并行改所有文件，仍按四阶段串行推进。
- 腾讯会议只支持手工录入，不做自动创建会议。
- 通知先只做站内通知。
- OCR 或独立 Python 解析服务本批次以“接口与扩展点落地”为主，不强制完成真实第三方接入。
