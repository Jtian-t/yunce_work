
from fastapi import APIRouter, HTTPException
from src.schemas import (
    ResumeParseRequest,
    CandidateInfo,
    ResumeAnalyzeRequest,
    AnalysisResult
)
from src.services.resume_service import ResumeService

router = APIRouter(prefix="/api/resume", tags=["resume"])


@router.post("/parse", response_model=CandidateInfo)
async def parse_resume(request: ResumeParseRequest):
    """
    解析简历文本，提取结构化信息
    """
    try:
        result = ResumeService.parse_resume(request.resume_text)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"解析失败: {str(e)}")


@router.post("/analyze", response_model=AnalysisResult)
async def analyze_resume(request: ResumeAnalyzeRequest):
    """
    根据候选人信息、岗位要求和面试反馈生成分析建议
    """
    try:
        result = ResumeService.analyze_candidate(
            candidate_info=request.candidate_info,
            job_requirements=request.job_requirements,
            interview_feedbacks=request.interview_feedbacks
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"分析失败: {str(e)}")

