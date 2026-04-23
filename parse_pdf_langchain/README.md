# parse_pdf_langchain

`parse_pdf_langchain` 是新的 LangChain 版简历解析与辅助决策服务。

目标：

- 将简历解析结果收敛为适合 Java 和前端联调的精简 JSON
- 兼容现有接口：`/api/resume/parse-report`、`/api/resume/parse-report/upload`、`/api/resume/decision-report`
- 结合岗位要求和多轮面试反馈，输出业务决策导向的辅助决策 JSON

## 快速开始

1. 安装依赖

```bash
pip install -r requirements.txt
```

2. 配置环境变量

```bash
copy .env.example .env
```

需要设置：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL` 或 `LLM_ENDPOINT_ID`

3. 启动服务

```bash
uvicorn src.main:app --host 0.0.0.0 --port 8002 --reload
```

## 主要接口

- `POST /api/resume/parse`
- `POST /api/resume/parse-report`
- `POST /api/resume/parse-report/upload`
- `POST /api/resume/analyze`
- `POST /api/resume/decision-report`

## 输出特点

- `parse-report` 保持 Java 当前 `ParseReportResponse` 可直接消费
- `decision-report` 保持前端辅助决策卡片可直接消费
- 解析内容默认精简，不再返回冗长原文堆砌
