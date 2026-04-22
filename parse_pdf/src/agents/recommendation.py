
from typing import List
from src.schemas import CandidateInfo, InterviewFeedback, AnalysisResult
from src.llm_client import llm_client


RECOMMENDATION_SYSTEM_PROMPT = """你是一个专业的招聘顾问。请根据候选人信息、岗位要求和已有的面试反馈，给出综合分析建议。

请严格按照以下JSON格式输出：
{
  "experience_score": 85,
  "skill_match_score": 90,
  "overall_score": 87,
  "recommendation_reason": "推荐理由...",
  "risk_points": ["风险点1", "风险点2"],
  "interview_questions": ["问题1", "问题2"],
  "feedback_summary": "历史反馈总结..."
}

评分说明：
- experience_score: 0-100，根据工作/项目经验评定
- skill_match_score: 0-100，根据技能与岗位要求的匹配度评定
- overall_score: 0-100，综合评分

注意：
- 如果没有面试反馈，feedback_summary 可以写 "暂无面试反馈"
- 面试问题应具有针对性，帮助进一步验证候选人能力
- 风险点要客观指出可能存在的问题
"""


class RecommendationAgent:
    @staticmethod
    def _format_feedbacks(feedbacks: List[InterviewFeedback]) -> str:
        """将面试反馈列表格式化为可读文本"""
        if not feedbacks:
            return "暂无面试反馈"

        result = []
        for fb in feedbacks:
            lines = [f"--- 第{fb.round}轮面试 - {fb.interviewer} ---"]
            lines.append(f"反馈：{fb.feedback}")
            if fb.score is not None:
                lines.append(f"评分：{fb.score}/100")
            if fb.pros:
                lines.append(f"优点：{', '.join(fb.pros)}")
            if fb.cons:
                lines.append(f"缺点：{', '.join(fb.cons)}")
            result.append("\n".join(lines))

        return "\n\n".join(result)

    @staticmethod
    def analyze(
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback]
    ) -> AnalysisResult:
        """
        生成分析建议

        Args:
            candidate_info: 候选人信息
            job_requirements: 岗位要求
            interview_feedbacks: 面试反馈列表

        Returns:
            AnalysisResult 对象
        """
        feedbacks_text = RecommendationAgent._format_feedbacks(interview_feedbacks)

        user_prompt = f"""请进行分析：

候选人信息：
{candidate_info.model_dump_json(indent=2)}

岗位要求：
{job_requirements}

已完成的面试反馈：
{feedbacks_text}
"""

        return llm_client.chat_with_structured_output(
            system_prompt=RECOMMENDATION_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            output_model=AnalysisResult,
            max_retries=3
        )

