from __future__ import annotations

import io
from dataclasses import dataclass
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

import fitz
import httpx

from src.schemas import ResumeParseReportRequest

try:
    import numpy as np
    from rapidocr_onnxruntime import RapidOCR
except Exception:  # pragma: no cover
    np = None
    RapidOCR = None


@dataclass
class LoadedResume:
    file_name: str
    text: str
    source_label: str
    ocr_required: bool = False


def load_resume(
    request: ResumeParseReportRequest,
    *,
    uploaded_file_name: Optional[str] = None,
    uploaded_file_content: Optional[bytes] = None,
) -> LoadedResume:
    if uploaded_file_content is not None:
        file_name = uploaded_file_name or request.resume_file_name or "upload.pdf"
        return _from_bytes(uploaded_file_content, file_name, source_label="简历上传")

    if request.resume_text:
        return LoadedResume(
            file_name=request.resume_file_name or "resume.txt",
            text=request.resume_text,
            source_label=_detect_source_label(request.hint),
            ocr_required=False,
        )

    if request.resume_file_path:
        path = Path(request.resume_file_path)
        return _from_bytes(path.read_bytes(), path.name, source_label=_detect_source_label(request.hint))

    if request.resume_file_url:
        response = httpx.get(request.resume_file_url, timeout=60.0)
        response.raise_for_status()
        parsed = urlparse(request.resume_file_url)
        file_name = request.resume_file_name or Path(parsed.path).name or "resume.pdf"
        return _from_bytes(response.content, file_name, source_label=_detect_source_label(request.hint))

    raise ValueError("At least one of resume_text, resume_file_path, or resume_file_url must be provided")


def _from_bytes(content: bytes, file_name: str, source_label: str) -> LoadedResume:
    lower_name = file_name.lower()
    if lower_name.endswith(".pdf"):
        text, used_ocr = _extract_pdf_text(content)
        return LoadedResume(file_name=file_name, text=text, source_label=source_label, ocr_required=used_ocr)

    text = content.decode("utf-8", errors="ignore")
    return LoadedResume(file_name=file_name, text=text, source_label=source_label, ocr_required=False)


def _extract_pdf_text(content: bytes) -> tuple[str, bool]:
    document = fitz.open(stream=content, filetype="pdf")
    try:
        text = "\n".join(page.get_text("text") for page in document).strip()
        if text:
            return text, False

        if RapidOCR is None or np is None:
            return "", True

        ocr_engine = RapidOCR()
        page_texts = []
        for page in document:
            pix = page.get_pixmap(dpi=160, alpha=False)
            image = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)
            result, _ = ocr_engine(image)
            if result:
                page_texts.append("\n".join(item[1] for item in result if len(item) > 1))
        return "\n".join(page_texts).strip(), True
    finally:
        document.close()


def _detect_source_label(hint: Optional[str]) -> str:
    if not hint:
        return "简历上传"
    lowered = hint.lower()
    if "boss" in lowered:
        return "BOSS直聘"
    if "猎聘" in hint:
        return "猎聘"
    if "拉勾" in hint:
        return "拉勾"
    if "内推" in hint:
        return "内推"
    return "简历上传"
