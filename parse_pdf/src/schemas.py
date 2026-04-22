
from typing import List, Optional
from pydantic import BaseModel, Field


# ============ 简历解析相关 ============

class Education(BaseModel):
    school: str = Field(default="", description="学校")
    major: str = Field(default="", description="专业")
    degree: str = Field(default="", description="学历")
    duration: str = Field(default="", description="时间段")


class WorkExperience(BaseModel):
    company: str = Field(default="", description="公司")
    position: str = Field(default="", description="职位")
    duration: str = Field(default="", description="时间")
    description: str = Field(default="", description="职责描述")


class Project(BaseModel):
    name: str = Field(default="", description="项目名")
    role: str = Field(default="", description="角色")
    description: str = Field(default="", description="描述")
    tech_stack: List[str] = Field(default_factory=list, description="技术栈")


class CandidateInfo(BaseModel):
    name: str = Field(default="", description="姓名")
    phone: str = Field(default="", description="电话")
    email: str = Field(default="", description="邮箱")
    education: List[Education] = Field(default_factory=list, description="教育经历")
    work_experience: List[WorkExperience] = Field(default_factory=list, description="工作经历")
    projects: List[Project] = Field(default_factory=list, description="项目经验")
    skills: List[str] = Field(default_factory=list, description="技能列表")
    summary: str = Field(default="", description="个人总结")


class ResumeParseRequest(BaseModel):
    resume_text: str = Field(..., description="简历文本内容")


# ============ 分析建议相关 ============

class InterviewFeedback(BaseModel):
    round: int = Field(..., description="面试轮次")
    interviewer: str = Field(..., description="面试官")
    feedback: str = Field(..., description="反馈内容")
    score: Optional[int] = Field(default=None, description="评分 0-100")
    pros: Optional[List[str]] = Field(default_factory=list, description="优点")
    cons: Optional[List[str]] = Field(default_factory=list, description="缺点")


class ResumeAnalyzeRequest(BaseModel):
    candidate_info: CandidateInfo = Field(..., description="候选人信息")
    job_requirements: str = Field(..., description="岗位要求")
    interview_feedbacks: List[InterviewFeedback] = Field(default_factory=list, description="面试反馈列表")


class AnalysisResult(BaseModel):
    experience_score: int = Field(..., description="经验评分 0-100")
    skill_match_score: int = Field(..., description="技能匹配度 0-100")
    overall_score: int = Field(..., description="综合评分 0-100")
    recommendation_reason: str = Field(..., description="推荐理由")
    risk_points: List[str] = Field(default_factory=list, description="风险点")
    interview_questions: List[str] = Field(default_factory=list, description="建议面试问题")
    feedback_summary: str = Field(default="", description="历史反馈总结")

