from __future__ import annotations

from pathlib import Path
from urllib.parse import urlparse

import httpx

from src.agent_runtime.context import ParseAgentContext, ResumeSource
from src.schemas import ParseIssue


def load_resume_source(context: ParseAgentContext) -> None:
    request = context.request
    if request.resume_text:
        context.source = ResumeSource(
            kind="text",
            file_name=request.resume_file_name or "resume.txt",
            mime_type="text/plain",
            text=request.resume_text,
        )
        return

    if request.resume_file_path:
        path = Path(request.resume_file_path)
        raw_bytes = path.read_bytes()
        context.source = ResumeSource(
            kind="file",
            file_name=path.name,
            mime_type=_guess_mime_type(path.name, raw_bytes),
            raw_bytes=raw_bytes,
            source_uri=str(path),
        )
        return

    if request.resume_file_url:
        response = httpx.get(request.resume_file_url, timeout=60.0)
        response.raise_for_status()
        parsed = urlparse(request.resume_file_url)
        file_name = Path(parsed.path).name or request.resume_file_name or "resume.pdf"
        raw_bytes = response.content
        context.source = ResumeSource(
            kind="url",
            file_name=file_name,
            mime_type=response.headers.get("content-type", _guess_mime_type(file_name, raw_bytes)),
            raw_bytes=raw_bytes,
            source_uri=request.resume_file_url,
        )
        return

    raise ValueError("At least one of resume_text, resume_file_path, or resume_file_url must be provided")


def _guess_mime_type(file_name: str, raw_bytes: bytes) -> str:
    lowered = file_name.lower()
    if lowered.endswith(".pdf") or raw_bytes.startswith(b"%PDF"):
        return "application/pdf"
    if lowered.endswith(".txt"):
        return "text/plain"
    return "application/octet-stream"
