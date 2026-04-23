from __future__ import annotations

from src.agent_runtime.context import ParseAgentContext


def detect_ocr_need(context: ParseAgentContext) -> None:
    text = "\n".join(block.content for block in context.blocks).strip()
    char_count = len(text)
    printable_count = sum(1 for char in text if char.isalnum() or "\u4e00" <= char <= "\u9fff" or char in " \n-_:;,.()")
    printable_ratio = (printable_count / char_count) if char_count else 0.0

    context.metadata["ocr_checked"] = True
    context.metadata["raw_text_length"] = char_count
    context.metadata["printable_ratio"] = printable_ratio

    context.ocr_required = bool(
        "pdf" in (context.source.mime_type.lower() if context.source else "")
        and (char_count < 120 or printable_ratio < 0.65)
    )
