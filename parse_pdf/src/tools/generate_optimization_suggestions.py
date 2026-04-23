from __future__ import annotations

import json

from pydantic import BaseModel, Field

from src.agent_runtime.context import DecisionAgentContext
from src.llm_client import llm_client
from src.schemas import DecisionReport


class _DecisionFinalizer(BaseModel):
    optimization_suggestions: list[str] = Field(default_factory=list)
    recommended_action: str = ""
    reasoning_summary: str = ""
    conclusion: str = ""


def generate_optimization_suggestions(context: DecisionAgentContext) -> None:
    if "fit_score" not in context.metadata:
        raise ValueError("Job fit score must be generated before finalizing decision")

    fit_score = context.metadata["fit_score"]
    feedback_aggregate = context.feedback_aggregate
    candidate_info = context.candidate_info or context.request.resolved_candidate_info()
    if candidate_info is None:
        raise ValueError("Candidate information is required")

    prompt_payload = {
        "fit_score": fit_score,
        "candidate_info": candidate_info.model_dump(),
        "feedback_summary": feedback_aggregate.summary if feedback_aggregate else "",
        "round_summaries": [item.model_dump() for item in (feedback_aggregate.round_summaries if feedback_aggregate else [])],
        "job_requirements": context.request.job_requirements,
    }
    finalizer = llm_client.chat_with_structured_output(
        system_prompt="You finalize hiring suggestions and return JSON only.",
        user_prompt=(
            "请根据已有岗位匹配评分，生成最终决策建议 JSON。\n"
            "要求：\n"
            "1. 只返回 JSON。\n"
            "2. optimization_suggestions 返回数组，每条建议要可执行。\n"
            "3. conclusion、recommended_action、reasoning_summary 都要简洁清晰。\n\n"
            f"{json.dumps(prompt_payload, ensure_ascii=False, indent=2)}"
        ),
        output_model=_DecisionFinalizer,
        max_retries=2,
    )
    if not finalizer.conclusion:
        finalizer.conclusion = fit_score["conclusion"]
    if not finalizer.recommended_action:
        finalizer.recommended_action = fit_score["recommended_action"]
    if not finalizer.reasoning_summary:
        finalizer.reasoning_summary = fit_score["reasoning_summary"]

    context.decision_report = DecisionReport(
        conclusion=finalizer.conclusion or fit_score["conclusion"],
        recommendation_score=fit_score["recommendation_score"],
        recommendation_level=fit_score["recommendation_level"],
        recommended_action=finalizer.recommended_action or fit_score["recommended_action"],
        strengths=fit_score["strengths"],
        risks=fit_score["risks"],
        missing_information=fit_score["missing_information"],
        supporting_evidence=fit_score["supporting_evidence"],
        reasoning_summary=finalizer.reasoning_summary or fit_score["reasoning_summary"],
        optimization_suggestions=finalizer.optimization_suggestions,
        interview_round_summaries=feedback_aggregate.round_summaries if feedback_aggregate else [],
    )
