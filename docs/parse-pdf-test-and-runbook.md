# parse_pdf 测试与运行手册

## 1. 环境准备

项目路径：

- `D:\claude_program\Codex\云测作业\parse_pdf`

推荐 Python 环境：

- `conda activate parse_pdf_clean`

安装依赖：

```powershell
cd D:\claude_program\Codex\云测作业\parse_pdf
python -m pip install -r requirements.txt
```

确保 `.env` 已配置：

- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_ENDPOINT_ID` 或 `LLM_MODEL`

## 2. 不启动 FastAPI 的本地测试流程

适合先验证“PDF 解析模块本身能不能跑通”。

执行：

```powershell
conda activate parse_pdf_clean
cd D:\claude_program\Codex\云测作业\parse_pdf
python .\tests\test_agent_pdf_flow.py
```

当前脚本会直接读取：

- `D:\claude_program\Codex\云测作业\parse_pdf\金天祥java后端开发简历.pdf`

脚本会打印：

- `Parse Report`
- `summary`
- `fields`
- `educations`
- `rawBlocks`

脚本内当前关键断言：

- `report.summary` 不为空
- `report.fields` 不为空
- `report.fields["name"].value == "金天祥"`
- `len(report.educations) >= 2`
- `len(report.raw_blocks) >= 10`

如果这里通过，说明：

- PDF 文件已经能读出来
- Agent 运行链路已经能产出结构化结果

## 3. 启动 FastAPI 后的接口测试流程

先启动服务：

```powershell
conda activate parse_pdf_clean
cd D:\claude_program\Codex\云测作业\parse_pdf
python -m uvicorn src.main:app --host 127.0.0.1 --port 8000 --reload
```

再执行接口测试：

```powershell
conda activate parse_pdf_clean
cd D:\claude_program\Codex\云测作业\parse_pdf
python .\tests\test_api.py
```

`test_api.py` 会测试三段流程：

1. 旧接口 `/api/resume/parse`
2. 新接口 `/api/resume/parse-report`
3. 新接口 `/api/resume/decision-report`

如果端口不是 `8000`，可以先设置：

```powershell
$env:BASE_URL="http://127.0.0.1:8001"
python .\tests\test_api.py
```

## 4. 程序主链路

### 4.1 简历解析链路

入口：

- `POST /api/resume/parse-report`
- `ResumeService.parse_resume_report(...)`

执行顺序：

1. `load_resume_source`
2. `extract_pdf_blocks`
3. `detect_ocr_need`
4. `ocr_pdf_pages`
5. `normalize_resume_sections`
6. `extract_candidate_profile`

最终输出：

- `ParseReport`

### 4.2 决策分析链路

入口：

- `POST /api/resume/decision-report`
- `ResumeService.generate_decision_report(...)`

执行顺序：

1. `aggregate_interview_feedback`
2. `score_job_fit`
3. `generate_optimization_suggestions`

最终输出：

- `DecisionReport`

## 5. 当前已知问题

当前版本已经能稳定完成：

- PDF 文本提取
- 姓名、手机号、邮箱识别
- 教育经历识别
- 基于简历和面评生成岗位建议

但仍有待继续优化：

- 工作经历提取还偏保守
- 项目经历标题识别在复杂版式下仍可能误判
- 技能标准化还有进一步压缩空间

## 6. 推荐排查顺序

如果结果不对，建议按这个顺序查：

1. 先跑 `test_agent_pdf_flow.py`
2. 再看 `Parse Report` 里的 `rawBlocks`
3. 再确认 `.env` 中模型配置是否可用
4. 最后再跑 `test_api.py`

这样可以快速区分：

- 是 PDF 没读出来
- 是 Agent 规则抽取有问题
- 还是大模型调用失败
