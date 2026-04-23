from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, alias_generator=to_camel)


class Education(BaseModel):
    school: str = Field(default="", description="School name")
    major: str = Field(default="", description="Major")
    degree: str = Field(default="", description="Degree")
    duration: str = Field(default="", description="Duration")


class WorkExperience(BaseModel):
    company: str = Field(default="", description="Company")
    position: str = Field(default="", description="Position")
    duration: str = Field(default="", description="Duration")
    description: str = Field(default="", description="Description")


class Project(BaseModel):
    name: str = Field(default="", description="Project name")
    role: str = Field(default="", description="Role")
    description: str = Field(default="", description="Description")
    tech_stack: List[str] = Field(default_factory=list, description="Tech stack")


class CandidateInfo(BaseModel):
    name: str = Field(default="", description="Candidate name")
    phone: str = Field(default="", description="Phone")
    email: str = Field(default="", description="Email")
    education: List[Education] = Field(default_factory=list)
    work_experience: List[WorkExperience] = Field(default_factory=list)
    projects: List[Project] = Field(default_factory=list)
    skills: List[str] = Field(default_factory=list)
    summary: str = Field(default="", description="Summary")


class ResumeParseRequest(BaseModel):
    resume_text: str = Field(..., description="Resume text")


class InterviewFeedback(BaseModel):
    round: int = Field(..., description="Interview round")
    interviewer: str = Field(..., description="Interviewer")
    feedback: str = Field(..., description="Feedback text")
    score: Optional[int] = Field(default=None, description="Optional score")
    pros: List[str] = Field(default_factory=list)
    cons: List[str] = Field(default_factory=list)


class ResumeAnalyzeRequest(BaseModel):
    candidate_info: CandidateInfo
    job_requirements: str = Field(default="")
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list)


class AnalysisResult(BaseModel):
    experience_score: int
    skill_match_score: int
    overall_score: int
    recommendation_reason: str
    risk_points: List[str] = Field(default_factory=list)
    interview_questions: List[str] = Field(default_factory=list)
    feedback_summary: str = Field(default="")


class ResumeSourceRequest(BaseModel):
    resume_text: Optional[str] = None
    resume_file_url: Optional[str] = None
    resume_file_path: Optional[str] = None
    resume_file_name: Optional[str] = None
    hint: Optional[str] = None


class ResumeParseReportRequest(ResumeSourceRequest):
    pass


class ResumeBlock(CamelModel):
    page_number: int = 1
    block_type: str = "TEXT"
    title: str = ""
    content: str = ""
    bbox: List[float] = Field(default_factory=list)


class ParseFieldEvidence(CamelModel):
    value: str
    confidence: float = 0.0
    source: str = ""


class ParseIssue(CamelModel):
    severity: str
    message: str


class ParseProjectSummary(CamelModel):
    title: str
    summary: str


class ParseSkill(CamelModel):
    raw_term: str
    normalized_name: str
    source_snippet: str
    confidence: float = 0.0


class ParseProjectDetail(CamelModel):
    project_name: str
    period: Optional[str] = None
    role: Optional[str] = None
    tech_stack: List[str] = Field(default_factory=list)
    responsibilities: List[str] = Field(default_factory=list)
    achievements: List[str] = Field(default_factory=list)
    summary: str = ""


class ParseExperience(CamelModel):
    company: str
    role: str
    period: str
    summary: str


class ParseEducation(CamelModel):
    school: str
    degree: str
    period: str
    summary: str


class ParseRawBlock(CamelModel):
    block_type: str
    title: str
    content: str


class OptimizationSuggestion(CamelModel):
    title: str
    detail: str
    priority: str = "medium"


class InterviewRoundSummary(CamelModel):
    round: int
    interviewer: str
    score: Optional[int] = None
    verdict: str = ""
    positives: List[str] = Field(default_factory=list)
    negatives: List[str] = Field(default_factory=list)


class DecisionDimensionScore(CamelModel):
    name: str
    score: int
    rationale: str


class ParseReport(CamelModel):
    summary: str
    highlights: List[str] = Field(default_factory=list)
    extracted_skills: List[str] = Field(default_factory=list)
    project_experiences: List[ParseProjectSummary] = Field(default_factory=list)
    skills: List[ParseSkill] = Field(default_factory=list)
    projects: List[ParseProjectDetail] = Field(default_factory=list)
    experiences: List[ParseExperience] = Field(default_factory=list)
    educations: List[ParseEducation] = Field(default_factory=list)
    raw_blocks: List[ParseRawBlock] = Field(default_factory=list)
    fields: Dict[str, ParseFieldEvidence] = Field(default_factory=dict)
    issues: List[ParseIssue] = Field(default_factory=list)
    extraction_mode: str = "UNKNOWN"
    ocr_required: bool = False


class DecisionReport(CamelModel):
    conclusion: str
    recommendation_score: int
    recommendation_level: str
    recommended_action: str
    strengths: List[str] = Field(default_factory=list)
    risks: List[str] = Field(default_factory=list)
    missing_information: List[str] = Field(default_factory=list)
    supporting_evidence: List[str] = Field(default_factory=list)
    reasoning_summary: str
    optimization_suggestions: List[str] = Field(default_factory=list)
    interview_round_summaries: List[InterviewRoundSummary] = Field(default_factory=list)


class ResumeDecisionReportRequest(BaseModel):
    candidate_info: Optional[CandidateInfo] = None
    parse_report: Optional[ParseReport] = None
    job_requirements: str = Field(default="")
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list)

    def resolved_candidate_info(self) -> Optional[CandidateInfo]:
        if self.candidate_info is not None:
            return self.candidate_info
        if self.parse_report is None:
            return None
        return candidate_info_from_parse_report(self.parse_report)


def candidate_info_from_parse_report(report: ParseReport) -> CandidateInfo:
    experiences = [
        WorkExperience(
            company=item.company,
            position=item.role,
            duration=item.period,
            description=item.summary,
        )
        for item in report.experiences
    ]
    projects = [
        Project(
            name=item.project_name,
            role=item.role or "",
            description=item.summary,
            tech_stack=item.tech_stack,
        )
        for item in report.projects
    ]
    education = [
        Education(
            school=item.school,
            major=item.summary,
            degree=item.degree,
            duration=item.period,
        )
        for item in report.educations
    ]
    return CandidateInfo(
        name=report.fields.get("name", ParseFieldEvidence(value="", confidence=0.0, source="")).value,
        phone=report.fields.get("phone", ParseFieldEvidence(value="", confidence=0.0, source="")).value,
        email=report.fields.get("email", ParseFieldEvidence(value="", confidence=0.0, source="")).value,
        education=education,
        work_experience=experiences,
        projects=projects,
        skills=report.extracted_skills,
        summary=report.summary,
    )


def analysis_result_from_decision_report(report: DecisionReport) -> AnalysisResult:
    experience_score = max(0, min(100, report.recommendation_score - 5))
    skill_score = max(0, min(100, report.recommendation_score))
    return AnalysisResult(
        experience_score=experience_score,
        skill_match_score=skill_score,
        overall_score=report.recommendation_score,
        recommendation_reason=report.conclusion,
        risk_points=report.risks,
        interview_questions=report.optimization_suggestions or report.missing_information,
        feedback_summary=report.reasoning_summary,
    )
