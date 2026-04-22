from typing import List

from src.agents.recommendation import RecommendationAgent
from src.agents.resume_parser import ResumeParserAgent
from src.schemas import AnalysisResult, CandidateInfo, InterviewFeedback


class ResumeService:
    @staticmethod
    def parse_resume(resume_text: str) -> CandidateInfo:
        return ResumeParserAgent.parse(resume_text)

    @staticmethod
    def analyze_candidate(
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback],
    ) -> AnalysisResult:
        return RecommendationAgent.analyze(
            candidate_info=candidate_info,
            job_requirements=job_requirements,
            interview_feedbacks=interview_feedbacks,
        )
