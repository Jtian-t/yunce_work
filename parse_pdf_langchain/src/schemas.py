from __future__ import annotations

from typing import Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, alias_generator=to_camel)


class Education(BaseModel):
    school: str = ""
    major: str = ""
    degree: str = ""
    duration: str = ""


class WorkExperience(BaseModel):
    company: str = ""
    position: str = ""
    duration: str = ""
    description: str = ""


class Project(BaseModel):
    name: str = ""
    role: str = ""
    description: str = ""
    tech_stack: List[str] = Field(default_factory=list)


class CandidateInfo(BaseModel):
    name: str = ""
    phone: str = ""
    email: str = ""
    education: List[Education] = Field(default_factory=list)
    work_experience: List[WorkExperience] = Field(default_factory=list)
    projects: List[Project] = Field(default_factory=list)
    skills: List[str] = Field(default_factory=list)
    summary: str = ""


class ResumeParseRequest(BaseModel):
    resume_text: str


class InterviewFeedback(BaseModel):
    round: int
    interviewer: str
    feedback: str
    score: Optional[int] = None
    pros: List[str] = Field(default_factory=list)
    cons: List[str] = Field(default_factory=list)


class ResumeAnalyzeRequest(BaseModel):
    candidate_info: CandidateInfo
    job_requirements: str = ""
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list)


class AnalysisResult(BaseModel):
    experience_score: int
    skill_match_score: int
    overall_score: int
    recommendation_reason: str
    risk_points: List[str] = Field(default_factory=list)
    interview_questions: List[str] = Field(default_factory=list)
    feedback_summary: str = ""


class ResumeSourceRequest(BaseModel):
    resume_text: Optional[str] = None
    resume_file_url: Optional[str] = None
    resume_file_path: Optional[str] = None
    resume_file_name: Optional[str] = None
    hint: Optional[str] = None


class ResumeParseReportRequest(ResumeSourceRequest):
    pass


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
    extraction_mode: str = "LANGCHAIN_STRUCTURED"
    ocr_required: bool = False


class InterviewRoundSummary(CamelModel):
    round: int
    interviewer: str
    score: Optional[int] = None
    verdict: str = ""
    positives: List[str] = Field(default_factory=list)
    negatives: List[str] = Field(default_factory=list)


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


class ResumeProfile(CamelModel):
    name: str = ""
    target_position: str = ""
    phone: str = ""
    email: str = ""
    education: str = ""
    experience_years: str = "0年"
    location: str = ""
    source: str = "简历上传"
    skills_summary: str = ""
    project_summary: str = ""
    skill_keywords: List[str] = Field(default_factory=list)
    project_highlights: List[str] = Field(default_factory=list)


class ResumeDecisionReportRequest(BaseModel):
    resume_profile: Optional[ResumeProfile] = None
    candidate_info: Optional[CandidateInfo] = None
    parse_report: Optional[ParseReport] = None
    job_requirements: str = ""
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list)

    def resolved_profile(self) -> ResumeProfile:
        if self.resume_profile is not None:
            return self.resume_profile
        if self.parse_report is not None:
            return profile_from_parse_report(self.parse_report)
        if self.candidate_info is not None:
            return profile_from_candidate_info(self.candidate_info)
        return ResumeProfile()


def _build_field(value: str, confidence: float = 0.85, source: str = "langchain") -> ParseFieldEvidence:
    return ParseFieldEvidence(value=value, confidence=confidence if value else 0.0, source=source)


def build_parse_report(profile: ResumeProfile, raw_text: str, file_name: str, ocr_required: bool = False) -> ParseReport:
    extracted_skills = list(dict.fromkeys([item.strip() for item in profile.skill_keywords if item.strip()]))[:8]
    if not extracted_skills and profile.skills_summary:
        extracted_skills = [
            item.strip()
            for item in profile.skills_summary.replace("、", ",").replace("，", ",").split(",")
            if item.strip()
        ][:8]

    project_items = list(dict.fromkeys([item.strip() for item in profile.project_highlights if item.strip()]))[:3]
    if not project_items and profile.project_summary:
        project_items = [profile.project_summary]

    fields = {
        "name": _build_field(profile.name, 0.98 if profile.name else 0.0),
        "targetPosition": _build_field(profile.target_position, 0.82),
        "phone": _build_field(profile.phone, 0.96 if profile.phone else 0.0),
        "email": _build_field(profile.email, 0.96 if profile.email else 0.0),
        "education": _build_field(profile.education, 0.85),
        "experience": _build_field(profile.experience_years, 0.82),
        "experienceYears": _build_field(profile.experience_years, 0.82),
        "location": _build_field(profile.location, 0.80),
        "source": _build_field(profile.source, 0.75),
        "skillsSummary": _build_field(profile.skills_summary, 0.88),
        "projectSummary": _build_field(profile.project_summary, 0.84),
    }

    highlights = [
        item
        for item in [
            "识别到完整联系方式" if profile.phone or profile.email else "",
            "识别到核心技术栈" if extracted_skills else "",
            "识别到代表性项目经历" if project_items else "",
        ]
        if item
    ]

    raw_blocks = []
    if raw_text.strip():
        raw_blocks.append(
            ParseRawBlock(
                block_type="TEXT",
                title=file_name or "resume",
                content=raw_text[:1200],
            )
        )

    return ParseReport(
        summary="已完成候选人关键信息提取，可用于表单预填和后续评估。",
        highlights=highlights,
        extracted_skills=extracted_skills,
        project_experiences=[
            ParseProjectSummary(
                title=item if len(item) <= 18 else "项目经历",
                summary=item,
            )
            for item in project_items
        ],
        skills=[
            ParseSkill(
                raw_term=item,
                normalized_name=item,
                source_snippet=profile.skills_summary or "resume_profile",
                confidence=0.82,
            )
            for item in extracted_skills
        ],
        projects=[
            ParseProjectDetail(
                project_name=item if len(item) <= 18 else "代表项目",
                tech_stack=extracted_skills[:5],
                responsibilities=[item],
                summary=item,
            )
            for item in project_items
        ],
        experiences=[
            ParseExperience(
                company="",
                role=profile.target_position,
                period=profile.experience_years,
                summary=profile.project_summary,
            )
        ]
        if profile.experience_years or profile.project_summary
        else [],
        educations=[
            ParseEducation(
                school="",
                degree="",
                period="",
                summary=profile.education,
            )
        ]
        if profile.education
        else [],
        raw_blocks=raw_blocks,
        fields={key: value for key, value in fields.items() if value.value},
        issues=[],
        extraction_mode="LANGCHAIN_STRUCTURED",
        ocr_required=ocr_required,
    )


def profile_from_parse_report(report: ParseReport) -> ResumeProfile:
    fields = report.fields
    return ResumeProfile(
        name=fields.get("name", ParseFieldEvidence(value="")).value,
        target_position=fields.get("targetPosition", ParseFieldEvidence(value="")).value,
        phone=fields.get("phone", ParseFieldEvidence(value="")).value,
        email=fields.get("email", ParseFieldEvidence(value="")).value,
        education=fields.get("education", ParseFieldEvidence(value="")).value,
        experience_years=fields.get(
            "experienceYears",
            fields.get("experience", ParseFieldEvidence(value="0年")),
        ).value
        or "0年",
        location=fields.get("location", ParseFieldEvidence(value="")).value,
        source=fields.get("source", ParseFieldEvidence(value="简历上传")).value or "简历上传",
        skills_summary=fields.get("skillsSummary", ParseFieldEvidence(value="")).value,
        project_summary=fields.get("projectSummary", ParseFieldEvidence(value="")).value,
        skill_keywords=report.extracted_skills,
        project_highlights=[item.summary for item in report.project_experiences if item.summary],
    )


def profile_from_candidate_info(candidate_info: CandidateInfo) -> ResumeProfile:
    education = ""
    if candidate_info.education:
        first = candidate_info.education[0]
        education = "，".join(item for item in [first.degree, first.major] if item)
    experience_years = candidate_info.work_experience[0].duration if candidate_info.work_experience else "0年"
    project_summary = candidate_info.projects[0].description if candidate_info.projects else ""
    return ResumeProfile(
        name=candidate_info.name,
        phone=candidate_info.phone,
        email=candidate_info.email,
        education=education,
        experience_years=experience_years or "0年",
        skills_summary="、".join(candidate_info.skills[:8]),
        project_summary=project_summary,
        skill_keywords=candidate_info.skills[:8],
        project_highlights=[item.description or item.name for item in candidate_info.projects[:3] if item.description or item.name],
    )


def candidate_info_from_parse_report(report: ParseReport) -> CandidateInfo:
    profile = profile_from_parse_report(report)
    return CandidateInfo(
        name=profile.name,
        phone=profile.phone,
        email=profile.email,
        education=[
            Education(
                school="",
                major=profile.education,
                degree="",
                duration="",
            )
        ]
        if profile.education
        else [],
        work_experience=[
            WorkExperience(
                company="",
                position=profile.target_position,
                duration=profile.experience_years,
                description=profile.project_summary,
            )
        ]
        if profile.experience_years or profile.project_summary
        else [],
        projects=[
            Project(
                name=item.title,
                description=item.summary,
                tech_stack=report.extracted_skills[:5],
            )
            for item in report.project_experiences
        ],
        skills=report.extracted_skills,
        summary="；".join(
            item
            for item in [profile.skills_summary, profile.project_summary]
            if item
        ),
    )


def analysis_result_from_decision_report(report: DecisionReport) -> AnalysisResult:
    return AnalysisResult(
        experience_score=max(0, min(100, report.recommendation_score - 5)),
        skill_match_score=max(0, min(100, report.recommendation_score)),
        overall_score=report.recommendation_score,
        recommendation_reason=report.conclusion,
        risk_points=report.risks,
        interview_questions=report.optimization_suggestions or report.missing_information,
        feedback_summary=report.reasoning_summary,
    )
