from fastapi import APIRouter, HTTPException

from src.schemas import AnalysisResult, CandidateInfo, ResumeAnalyzeRequest, ResumeParseRequest
from src.services.resume_service import ResumeService

router = APIRouter(prefix="/api/resume", tags=["resume"])


@router.post("/parse", response_model=CandidateInfo)
async def parse_resume(request: ResumeParseRequest):
    try:
        return ResumeService.parse_resume(request.resume_text)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse failed: {exc}") from exc


@router.post("/analyze", response_model=AnalysisResult)
async def analyze_resume(request: ResumeAnalyzeRequest):
    try:
        return ResumeService.analyze_candidate(
            candidate_info=request.candidate_info,
            job_requirements=request.job_requirements,
            interview_feedbacks=request.interview_feedbacks,
        )
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume analyze failed: {exc}") from exc
