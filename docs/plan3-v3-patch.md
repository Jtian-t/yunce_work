# 计划3第三版补丁：部门面试协调与角色化工作台

## Summary

本补丁在现有“我的面试 / 我的通知 / 候选人详情 / 部门待办”能力基础上，补齐第三版的部门协调视图与人员模型。

目标固定为：

- HR 可按部门和员工查看面试、通知并做最终安排
- 部门总监可查看本人部门整体面试情况，并建议面试官
- 普通面试官仍只保留个人视图
- 前端只升级 `我的面试` 和 `我的通知` 两个入口，不新增独立大页面

## 关键设计

### 角色规则

- `HR`：可看全局，可筛部门和员工
- `DEPARTMENT_LEAD`：只看本人部门，可筛本部门员工
- `INTERVIEWER`：只看自己，无部门协调标签

### 数据模型

- 继续使用 `platform_user + user_roles + department`
- `User` 新增：
  - `canInterview`
  - `employmentStatus`
  - `displayOrder`
- `DepartmentFeedback` 新增：
  - `suggestedInterviewerId`
  - `suggestedInterviewerName`

### 业务链路

- 总监在部门反馈页选择建议面试官
- HR 在候选人详情页最终选择面试官并填写会议链接
- 系统创建面试计划后：
  - 保存部门、轮次、面试官、会议信息
  - 给最终面试官发送 `INTERVIEW_ASSIGNED`
  - 候选人状态进入“等待本轮面试结果”

## 接口变更

- `GET /api/interviews/mine`
  - 支持 `scope=my|department`
  - 支持 `departmentId`
  - 支持 `userId`
- `GET /api/notifications`
  - 支持 `scope=my|department`
  - 支持 `departmentId`
  - 支持 `userId`
  - 支持 `type`
- `GET /api/lookups/department-members`
  - 返回部门员工、角色、可面试状态、在职状态
- `GET /api/lookups/users?role=INTERVIEWER`
  - 仅返回可用面试官

## 前端改造

- `我的面试`
  - 增加 `我的 / 部门协调` 双标签
  - `部门协调` 提供部门筛选、员工筛选、汇总卡片
- `我的通知`
  - 增加 `我的 / 部门协调` 双标签
  - `部门协调` 提供部门筛选、员工筛选、未读与面试分配统计
- `候选人详情`
  - 安排面试区展示部门建议面试官
  - HR 最终选择面试官时只显示本部门可用面试官
- `部门反馈`
  - 建议面试官从自由文本升级为结构化下拉选择

## 验收标准

- HR 可筛选不同部门和员工查看面试与通知
- 产品总监可查看产品部整体面试情况
- 面试官只看到自己的面试与通知
- 不可面试人员不会出现在 HR 安排面试下拉中
- 总监建议面试官后，HR 仍可改选
- HR 安排面试后，通知发送到最终选中的面试官
- 候选人状态进入等待本轮结果
