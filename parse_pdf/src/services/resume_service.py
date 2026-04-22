
from typing import List
from src.schemas import (
    CandidateInfo,
    InterviewFeedback,
    AnalysisResult
)
from src.agents.resume_parser import ResumeParserAgent
from src.agents.recommendation import RecommendationAgent


class ResumeService:
    @staticmethod
    def parse_resume(resume_text: str) -> CandidateInfo:
        """
        解析简历文本

        Args:
            resume_text: 简历文本

        Returns:
            结构化的候选人信息
        """
        return ResumeParserAgent.parse(resume_text)

    @staticmethod
    def analyze_candidate(
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback]
    ) -> AnalysisResult:
        """
        分析候选人并给出建议

        Args:
            candidate_info: 候选人信息
            job_requirements: 岗位要求
            interview_feedbacks: 面试反馈列表

        Returns:
            分析结果
        """
        return RecommendationAgent.analyze(
            candidate_info=candidate_info,
            job_requirements=job_requirements,
            interview_feedbacks=interview_feedbacks
        )

