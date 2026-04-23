from __future__ import annotations

import base64
import json
import logging

from fastapi import APIRouter, Body, File, Header, HTTPException, Request, UploadFile

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
logger = logging.getLogger(__name__)


async def _resolve_request(model_cls, parsed_request, raw_request: Request, default_value):
    if parsed_request is not None:
        return parsed_request

    raw_body = await raw_request.body()
    if not raw_body:
        return default_value

    try:
        payload = json.loads(raw_body.decode("utf-8"))
        return model_cls.model_validate(payload)
    except Exception as exc:
        logger.warning("Failed to parse raw request body for %s: %s", model_cls.__name__, exc)
        return default_value


def _decode_header_base64(value: str | None) -> str | None:
    if not value:
        return None
    try:
        return base64.b64decode(value).decode("utf-8")
    except Exception as exc:  # pragma: no cover
        logger.warning("Failed to decode base64 header value: %s", exc)
        return None


@router.post("/parse", response_model=CandidateInfo)
async def parse_resume(raw_request: Request, request: ResumeParseRequest | None = Body(default=None)):
    try:
        effective_request = await _resolve_request(
            ResumeParseRequest,
            request,
            raw_request,
            ResumeParseRequest(resume_text=""),
        )
        return ResumeService.parse_resume(effective_request.resume_text)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse failed: {exc}") from exc


@router.post("/parse-report", response_model=ParseReport)
async def parse_resume_report(raw_request: Request, request: ResumeParseReportRequest | None = Body(default=None)):
    try:
        effective_request = await _resolve_request(
            ResumeParseReportRequest,
            request,
            raw_request,
            ResumeParseReportRequest(),
        )
        logger.info(
            "parse-report received: text_len=%s file_url=%s file_path=%s file_name=%s base64_len=%s hint=%s",
            len(effective_request.resume_text or ""),
            bool(effective_request.resume_file_url),
            bool(effective_request.resume_file_path),
            effective_request.resume_file_name,
            len(effective_request.resume_file_base64 or ""),
            effective_request.hint,
        )
        return ResumeService.parse_resume_report(effective_request)
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


@router.post("/parse-report/raw", response_model=ParseReport)
async def parse_resume_report_raw(
    raw_request: Request,
    x_resume_file_name: str | None = Header(default=None),
    x_parse_hint: str | None = Header(default=None),
    x_resume_file_name_base64: str | None = Header(default=None),
    x_parse_hint_base64: str | None = Header(default=None),
):
    try:
        content = await raw_request.body()
        file_name = _decode_header_base64(x_resume_file_name_base64) or x_resume_file_name or "upload.pdf"
        hint = _decode_header_base64(x_parse_hint_base64) or x_parse_hint
        logger.info(
            "parse-report raw received: bytes=%s file_name=%s hint=%s",
            len(content),
            file_name,
            hint,
        )
        return ResumeService.parse_resume_report(
            ResumeParseReportRequest(
                resume_file_name=file_name,
                hint=hint,
            ),
            uploaded_file_name=file_name,
            uploaded_file_content=content,
        )
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume parse raw failed: {exc}") from exc


@router.post("/analyze", response_model=AnalysisResult)
async def analyze_resume(raw_request: Request, request: ResumeAnalyzeRequest | None = Body(default=None)):
    try:
        effective_request = await _resolve_request(
            ResumeAnalyzeRequest,
            request,
            raw_request,
            ResumeAnalyzeRequest(candidate_info=CandidateInfo()),
        )
        return ResumeService.analyze_candidate(
            candidate_info=effective_request.candidate_info,
            job_requirements=effective_request.job_requirements,
            interview_feedbacks=effective_request.interview_feedbacks,
        )
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume analyze failed: {exc}") from exc


@router.post("/decision-report", response_model=DecisionReport)
async def decision_report(raw_request: Request, request: ResumeDecisionReportRequest | None = Body(default=None)):
    try:
        effective_request = await _resolve_request(
            ResumeDecisionReportRequest,
            request,
            raw_request,
            ResumeDecisionReportRequest(),
        )
        return ResumeService.generate_decision_report(effective_request)
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=500, detail=f"Resume decision report failed: {exc}") from exc
