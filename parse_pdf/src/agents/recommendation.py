import json
from typing import List

from src.llm_client import llm_client
from src.schemas import AnalysisResult, CandidateInfo, InterviewFeedback


RECOMMENDATION_SYSTEM_PROMPT = """
你是一名招聘决策分析助手。
请结合候选人的简历结构化信息、岗位要求和已有面试反馈，输出符合 AnalysisResult Schema 的 JSON。

输出要求：
1. experience_score：评估候选人的经历与岗位要求的匹配程度，0 到 100。
2. skill_match_score：评估候选人的技能与岗位要求的匹配程度，0 到 100。
3. overall_score：综合建议分，0 到 100。
4. recommendation_reason：给出清晰、可执行的综合判断。
5. risk_points：列出 2 到 5 个主要风险点，没有明显风险时可以为空数组。
6. interview_questions：给出建议继续追问的问题，没有时返回空数组。
7. feedback_summary：总结面试反馈对岗位适配度的影响。
8. 输出必须是纯 JSON，不要附带解释文本。
"""


class RecommendationAgent:
    @staticmethod
    def _format_feedbacks(feedbacks: List[InterviewFeedback]) -> str:
        if not feedbacks:
            return "暂无面试反馈"

        blocks: List[str] = []
        for feedback in feedbacks:
            lines = [f"第 {feedback.round} 轮 - {feedback.interviewer}"]
            lines.append(f"评价：{feedback.feedback}")
            if feedback.score is not None:
                lines.append(f"评分：{feedback.score}/100")
            if feedback.pros:
                lines.append(f"优点：{', '.join(feedback.pros)}")
            if feedback.cons:
                lines.append(f"不足：{', '.join(feedback.cons)}")
            blocks.append("\n".join(lines))
        return "\n\n".join(blocks)

    @staticmethod
    def analyze(
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback],
    ) -> AnalysisResult:
        feedbacks_text = RecommendationAgent._format_feedbacks(interview_feedbacks)
        candidate_json = json.dumps(candidate_info.model_dump(), ensure_ascii=False, indent=2)
        user_prompt = f"""
请根据下面的信息输出 AnalysisResult JSON：

候选人信息：
{candidate_json}

岗位要求：
{job_requirements or '暂无岗位要求摘要，请结合候选人投递岗位和反馈进行保守分析。'}

面试反馈：
{feedbacks_text}
"""

        return llm_client.chat_with_structured_output(
            system_prompt=RECOMMENDATION_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            output_model=AnalysisResult,
            max_retries=3,
        )
