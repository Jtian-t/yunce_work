from __future__ import annotations

from fastapi import APIRouter, File, HTTPException, UploadFile

from src.schemas import (
    AnalysisResult,
    CandidateInfo,
    DecisionReport,
    ParseReport,
    ResumeAnalyzeRequest,
    ResumeDecisionReportRequest,
    ResumeParseReportRequest,
    ResumeParseRequest,
)
from src.services.resume_service import ResumeService

router = APIRouter(prefix="/api/resume", tags=["resume"])


@router.post("/parse", response_model=CandidateInfo)
async def parse_resume(request: ResumeParseRequest):
    try:
        return ResumeService.parse_resume(request.resume_text)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse failed: {exc}") from exc


@router.post("/parse-report", response_model=ParseReport)
async def parse_resume_report(request: ResumeParseReportRequest):
    try:
        return ResumeService.parse_resume_report(request)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse report failed: {exc}") from exc


@router.post("/parse-report/upload", response_model=ParseReport)
async def parse_resume_report_upload(file: UploadFile = File(...)):
    try:
        return ResumeService.parse_resume_report(
            ResumeParseReportRequest(resume_file_name=file.filename),
            uploaded_file_name=file.filename,
            uploaded_file_content=await file.read(),
        )
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse upload failed: {exc}") from exc


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


@router.post("/decision-report", response_model=DecisionReport)
async def decision_report(request: ResumeDecisionReportRequest):
    try:
        return ResumeService.generate_decision_report(request)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume decision report failed: {exc}") from exc
