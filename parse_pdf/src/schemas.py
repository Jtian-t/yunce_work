from typing import List, Optional

from pydantic import BaseModel, Field


class Education(BaseModel):
    school: str = Field(default="", description="毕业院校")
    major: str = Field(default="", description="专业")
    degree: str = Field(default="", description="学历或学位")
    duration: str = Field(default="", description="时间范围")


class WorkExperience(BaseModel):
    company: str = Field(default="", description="公司名称")
    position: str = Field(default="", description="岗位名称")
    duration: str = Field(default="", description="时间范围")
    description: str = Field(default="", description="职责、产出或业绩描述")


class Project(BaseModel):
    name: str = Field(default="", description="项目名称")
    role: str = Field(default="", description="承担角色")
    description: str = Field(default="", description="项目描述")
    tech_stack: List[str] = Field(default_factory=list, description="技术栈")


class CandidateInfo(BaseModel):
    name: str = Field(default="", description="候选人姓名")
    phone: str = Field(default="", description="手机号")
    email: str = Field(default="", description="邮箱")
    education: List[Education] = Field(default_factory=list, description="教育经历")
    work_experience: List[WorkExperience] = Field(default_factory=list, description="工作经历")
    projects: List[Project] = Field(default_factory=list, description="项目经历")
    skills: List[str] = Field(default_factory=list, description="技能列表")
    summary: str = Field(default="", description="候选人摘要")


class ResumeParseRequest(BaseModel):
    resume_text: str = Field(..., description="简历文本内容")


class InterviewFeedback(BaseModel):
    round: int = Field(..., description="面试轮次，1 表示一面")
    interviewer: str = Field(..., description="面试官姓名")
    feedback: str = Field(..., description="原始评价内容")
    score: Optional[int] = Field(default=None, description="评分，0 到 100")
    pros: List[str] = Field(default_factory=list, description="优点")
    cons: List[str] = Field(default_factory=list, description="不足或风险点")


class ResumeAnalyzeRequest(BaseModel):
    candidate_info: CandidateInfo = Field(..., description="结构化候选人信息")
    job_requirements: str = Field(default="", description="岗位要求摘要")
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list, description="面试反馈列表")


class AnalysisResult(BaseModel):
    experience_score: int = Field(..., description="经验匹配分，0 到 100")
    skill_match_score: int = Field(..., description="技能匹配分，0 到 100")
    overall_score: int = Field(..., description="综合建议分，0 到 100")
    recommendation_reason: str = Field(..., description="综合分析结论")
    risk_points: List[str] = Field(default_factory=list, description="主要风险点")
    interview_questions: List[str] = Field(default_factory=list, description="建议继续追问的问题")
    feedback_summary: str = Field(default="", description="结合面试反馈的总结")
