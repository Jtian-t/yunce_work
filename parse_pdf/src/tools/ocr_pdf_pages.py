from __future__ import annotations

from src.agent_runtime.context import ParseAgentContext
from src.schemas import ParseIssue, ResumeBlock


def ocr_pdf_pages(context: ParseAgentContext) -> None:
    context.metadata["ocr_completed"] = True
    if not context.ocr_required or context.source is None:
        return

    try:
        import fitz  # type: ignore
        import numpy as np  # type: ignore
        from rapidocr_onnxruntime import RapidOCR  # type: ignore
    except ImportError as exc:  # pragma: no cover - depends on local env
        context.issues.append(ParseIssue(severity="WARN", message="OCR dependency missing; using text extraction result as fallback."))
        context.extraction_mode = "TEXT_PDF_OCR_SKIPPED"
        return

    engine = RapidOCR()
    document = fitz.open(stream=context.source.raw_bytes, filetype="pdf")
    ocr_blocks: list[ResumeBlock] = []
    try:
        for index, page in enumerate(document, start=1):
            pix = page.get_pixmap(dpi=180, alpha=False)
            channels = pix.n
            image = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, channels)
            result, _ = engine(image)
            lines = []
            for item in result or []:
                if len(item) >= 2 and item[1]:
                    lines.append(str(item[1]).strip())
            content = "\n".join(line for line in lines if line)
            if content:
                ocr_blocks.append(ResumeBlock(page_number=index, block_type="OCR_TEXT", title=f"ocr_page_{index}", content=content))
    finally:
        document.close()

    if ocr_blocks:
        context.blocks = ocr_blocks
        context.extraction_mode = "OCR_PDF"
