from typing import List

from src.schemas import AnalysisResult, CandidateInfo, InterviewFeedback, ResumeDecisionReportRequest, analysis_result_from_decision_report
from src.services.resume_service import ResumeService


class RecommendationAgent:
    @staticmethod
    def analyze(
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback],
    ) -> AnalysisResult:
        report = ResumeService.generate_decision_report(
            ResumeDecisionReportRequest(
                candidate_info=candidate_info,
                job_requirements=job_requirements,
                interview_feedbacks=interview_feedbacks,
            )
        )
        return analysis_result_from_decision_report(report)
