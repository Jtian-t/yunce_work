from __future__ import annotations

from typing import List

from src.agent_runtime.context import DecisionAgentContext, ParseAgentContext, ResumeSource
from src.agent_runtime.runner import DecisionAgentRunner, ParseAgentRunner
from src.schemas import (
    AnalysisResult,
    CandidateInfo,
    DecisionReport,
    InterviewFeedback,
    ParseReport,
    ResumeAnalyzeRequest,
    ResumeDecisionReportRequest,
    ResumeParseReportRequest,
    ResumeParseRequest,
    analysis_result_from_decision_report,
    candidate_info_from_parse_report,
)


class ResumeService:
    parse_runner = ParseAgentRunner()
    decision_runner = DecisionAgentRunner()

    @classmethod
    def parse_resume(cls, resume_text: str) -> CandidateInfo:
        report = cls.parse_resume_report(ResumeParseReportRequest(resume_text=resume_text))
        return candidate_info_from_parse_report(report)

    @classmethod
    def parse_resume_report(
        cls,
        request: ResumeParseReportRequest,
        *,
        uploaded_file_name: str | None = None,
        uploaded_file_content: bytes | None = None,
    ) -> ParseReport:
        context = ParseAgentContext(request=request)
        if uploaded_file_content is not None:
            context.source = ResumeSource(
                kind="upload",
                file_name=uploaded_file_name or request.resume_file_name or "upload.pdf",
                mime_type="application/pdf",
                raw_bytes=uploaded_file_content,
            )
            context.request.resume_file_name = context.source.file_name
        return cls.parse_runner.run(context)

    @classmethod
    def analyze_candidate(
        cls,
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks: List[InterviewFeedback],
    ) -> AnalysisResult:
        report = cls.generate_decision_report(
            ResumeDecisionReportRequest(
                candidate_info=candidate_info,
                job_requirements=job_requirements,
                interview_feedbacks=interview_feedbacks,
            )
        )
        return analysis_result_from_decision_report(report)

    @classmethod
    def generate_decision_report(cls, request: ResumeDecisionReportRequest) -> DecisionReport:
        context = DecisionAgentContext(
            request=request,
            candidate_info=request.resolved_candidate_info(),
            parse_report=request.parse_report,
            feedbacks=request.interview_feedbacks,
        )
        return cls.decision_runner.run(context)
