from __future__ import annotations

from typing import List

from src.agent_runtime.context import ParseAgentContext
from src.schemas import ParseIssue, ResumeBlock


def extract_pdf_blocks(context: ParseAgentContext) -> None:
    if context.source is None:
        raise ValueError("Resume source is not loaded")

    if "pdf" not in context.source.mime_type.lower():
        text = context.source.text or context.source.raw_bytes.decode("utf-8", errors="ignore")
        context.blocks = [ResumeBlock(page_number=1, block_type="TEXT", title="raw_text", content=text)]
        context.extraction_mode = "PLAIN_TEXT"
        return

    try:
        import fitz  # type: ignore
    except ImportError as exc:  # pragma: no cover - depends on local env
        context.issues.append(ParseIssue(severity="WARN", message="PyMuPDF is not installed; PDF text extraction unavailable."))
        raise RuntimeError("PyMuPDF is required to parse PDF files") from exc

    blocks: List[ResumeBlock] = []
    document = fitz.open(stream=context.source.raw_bytes, filetype="pdf")
    total_text = []
    try:
        for index, page in enumerate(document, start=1):
            page_text = page.get_text("text") or ""
            total_text.append(page_text)
            for raw_block in page.get_text("blocks"):
                x0, y0, x1, y1, text, *_ = raw_block
                cleaned = (text or "").strip()
                if not cleaned:
                    continue
                blocks.append(
                    ResumeBlock(
                        page_number=index,
                        block_type="TEXT",
                        title=f"page_{index}",
                        content=cleaned,
                        bbox=[float(x0), float(y0), float(x1), float(y1)],
                    )
                )
            if page_text.strip() and not any(block.page_number == index for block in blocks):
                blocks.append(ResumeBlock(page_number=index, block_type="TEXT", title=f"page_{index}", content=page_text.strip()))
    finally:
        document.close()

    if not blocks:
        blocks = [ResumeBlock(page_number=1, block_type="TEXT", title="raw_pdf_text", content="\n".join(total_text).strip())]

    context.blocks = blocks
    context.extraction_mode = "TEXT_PDF"
