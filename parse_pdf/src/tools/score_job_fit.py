from __future__ import annotations

import json

from pydantic import BaseModel, Field

from src.agent_runtime.context import DecisionAgentContext
from src.llm_client import llm_client


class _DecisionScoring(BaseModel):
    recommendation_score: int = Field(..., ge=0, le=100)
    recommendation_level: str = ""
    conclusion: str = ""
    strengths: list[str] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    missing_information: list[str] = Field(default_factory=list)
    supporting_evidence: list[str] = Field(default_factory=list)
    recommended_action: str = ""
    reasoning_summary: str = ""


def score_job_fit(context: DecisionAgentContext) -> None:
    if context.candidate_info is None and context.parse_report is not None:
        context.candidate_info = context.request.resolved_candidate_info()
    if context.candidate_info is None:
        raise ValueError("Candidate information is required for scoring")

    feedback_summary = context.feedback_aggregate.summary if context.feedback_aggregate else "No interview feedback provided."
    candidate_json = json.dumps(context.candidate_info.model_dump(), ensure_ascii=False, indent=2)
    scoring = llm_client.chat_with_structured_output(
        system_prompt="You evaluate job fit and return JSON only.",
        user_prompt=(
            "请根据岗位要求、候选人简历和面试反馈，输出一个标准 JSON 对象。\n"
            "要求：\n"
            "1. 只返回 JSON。\n"
            "2. recommendation_score 为 0 到 100 的整数。\n"
            "3. strengths、risks、missing_information、supporting_evidence 都返回字符串数组。\n"
            "4. 如果信息不足，请在 missing_information 中说明，不要编造经历。\n\n"
            f"岗位要求:\n{context.request.job_requirements or '未提供岗位要求，请给出通用评估。'}\n\n"
            f"候选人信息:\n{candidate_json}\n\n"
            f"面试反馈汇总:\n{feedback_summary}"
        ),
        output_model=_DecisionScoring,
        max_retries=2,
    )
    if not scoring.recommendation_level:
        scoring.recommendation_level = _score_to_level(scoring.recommendation_score)
    if not scoring.conclusion:
        scoring.conclusion = "已根据简历、岗位要求和面试反馈生成岗位匹配结论。"
    if not scoring.recommended_action:
        scoring.recommended_action = "建议结合缺失信息继续追问关键能力，再决定是否推进到下一轮。"
    if not scoring.reasoning_summary:
        scoring.reasoning_summary = "综合简历证据、岗位要求和面试反馈，已完成岗位匹配评分。"
    context.metadata["fit_score"] = scoring.model_dump()


def _score_to_level(score: int) -> str:
    if score >= 85:
        return "strong_yes"
    if score >= 70:
        return "yes_with_followup"
    if score >= 55:
        return "needs_more_signal"
    return "no"
