from __future__ import annotations

from src.chains.decision_chain import DecisionChain
from src.chains.resume_parse_chain import ResumeParseChain
from src.schemas import (
    AnalysisResult,
    CandidateInfo,
    DecisionReport,
    ParseReport,
    ResumeAnalyzeRequest,
    ResumeDecisionReportRequest,
    ResumeParseReportRequest,
    analysis_result_from_decision_report,
    build_parse_report,
    candidate_info_from_parse_report,
)
from src.utils.pdf_loader import load_resume


class ResumeService:
    parse_chain = ResumeParseChain()
    decision_chain = DecisionChain()

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
        loaded = load_resume(
            request,
            uploaded_file_name=uploaded_file_name,
            uploaded_file_content=uploaded_file_content,
        )
        profile = cls.parse_chain.invoke(
            resume_text=loaded.text,
            hint=request.hint or loaded.source_label,
        )
        if not profile.source:
            profile.source = loaded.source_label
        return build_parse_report(
            profile=profile,
            raw_text=loaded.text,
            file_name=loaded.file_name,
            ocr_required=loaded.ocr_required,
        )

    @classmethod
    def analyze_candidate(
        cls,
        candidate_info: CandidateInfo,
        job_requirements: str,
        interview_feedbacks,
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
        profile = request.resolved_profile()
        return cls.decision_chain.invoke(
            resume_profile=profile,
            job_requirements=request.job_requirements,
            interview_feedbacks=request.interview_feedbacks,
        )
