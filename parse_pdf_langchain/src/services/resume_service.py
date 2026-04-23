from __future__ import annotations

from src.chains.decision_chain import DecisionChain
from src.chains.resume_parse_chain import ResumeParseChain
from src.schemas import (
    AnalysisResult,
    CandidateInfo,
    DecisionReport,
    ParseIssue,
    ParseReport,
    ResumeProfile,
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
        if (
            uploaded_file_content is None
            and not request.resume_text
            and not request.resume_file_path
            and not request.resume_file_url
            and not request.resume_file_base64
        ):
            report = build_parse_report(
                profile=ResumeProfile(source="简历上传"),
                raw_text="",
                file_name=request.resume_file_name or "resume.txt",
                ocr_required=False,
            )
            report.summary = "未收到简历内容，返回空白解析结果。"
            report.issues.append(ParseIssue(severity="WARN", message="请求体为空或未包含简历内容"))
            return report

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
        if (
            not profile.name
            and not profile.skills_summary
            and not request.job_requirements
            and not request.interview_feedbacks
        ):
            return DecisionReport(
                conclusion="当前缺少候选人信息，暂无法生成有效决策。",
                recommendation_score=0,
                recommendation_level="建议补充信息",
                recommended_action="请先补充简历解析结果、岗位要求或面试反馈，再重新生成辅助决策。",
                strengths=[],
                risks=["候选人基础信息缺失"],
                missing_information=["简历解析结果", "岗位要求", "面试反馈"],
                supporting_evidence=["请求体为空或缺少有效字段"],
                reasoning_summary="由于输入信息缺失，当前只能返回空白决策结果。",
                optimization_suggestions=["先完成简历解析，再发起辅助决策"],
                interview_round_summaries=[],
            )
        return cls.decision_chain.invoke(
            resume_profile=profile,
            job_requirements=request.job_requirements,
            interview_feedbacks=request.interview_feedbacks,
        )
