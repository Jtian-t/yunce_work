from __future__ import annotations

from src.agent_runtime.context import DecisionAgentContext, FeedbackAggregate
from src.schemas import InterviewRoundSummary


def aggregate_interview_feedback(context: DecisionAgentContext) -> None:
    round_summaries = []
    positives: list[str] = []
    negatives: list[str] = []
    missing_information: list[str] = []

    for feedback in context.feedbacks:
        verdict = "positive"
        if feedback.score is not None and feedback.score < 60:
            verdict = "negative"
        elif feedback.cons and not feedback.pros:
            verdict = "negative"
        elif feedback.cons:
            verdict = "mixed"
        positives.extend(feedback.pros)
        negatives.extend(feedback.cons)
        if not feedback.pros and not feedback.cons:
            missing_information.append(f"第 {feedback.round} 轮缺少明确优缺点拆分")
        round_summaries.append(
            InterviewRoundSummary(
                round=feedback.round,
                interviewer=feedback.interviewer,
                score=feedback.score,
                verdict=verdict,
                positives=feedback.pros,
                negatives=feedback.cons,
            )
        )

    summary_parts = []
    if positives:
        summary_parts.append("面试正向反馈集中在：" + "、".join(dict.fromkeys(positives)))
    if negatives:
        summary_parts.append("主要风险点包括：" + "、".join(dict.fromkeys(negatives)))
    if not summary_parts:
        summary_parts.append("当前面评信息较少，建议增加具体优缺点描述。")

    context.feedback_aggregate = FeedbackAggregate(
        summary="；".join(summary_parts),
        positives=list(dict.fromkeys(positives)),
        negatives=list(dict.fromkeys(negatives)),
        missing_information=list(dict.fromkeys(missing_information)),
        round_summaries=round_summaries,
    )
