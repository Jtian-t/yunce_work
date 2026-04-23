from src.schemas import CandidateInfo, ResumeParseReportRequest, candidate_info_from_parse_report
from src.services.resume_service import ResumeService


class ResumeParserAgent:
    @staticmethod
    def parse(resume_text: str) -> CandidateInfo:
        report = ResumeService.parse_resume_report(ResumeParseReportRequest(resume_text=resume_text))
        return candidate_info_from_parse_report(report)
