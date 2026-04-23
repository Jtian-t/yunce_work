from __future__ import annotations

import re
from collections import OrderedDict

from src.agent_runtime.context import ParseAgentContext
from src.schemas import ParseFieldEvidence, ParseIssue


SECTION_KEYWORDS = OrderedDict(
    [
        ("education", ["教育经历", "教育背景", "学历", "education"]),
        ("work_experience", ["工作经历", "实习经历", "职业经历", "experience"]),
        ("projects", ["项目经历", "项目经验", "projects"]),
        ("skills", ["技能", "专业技能", "技能特长", "skills", "tech stack"]),
        ("summary", ["个人总结", "个人评价", "自我评价", "summary", "profile"]),
    ]
)


def normalize_resume_sections(context: ParseAgentContext) -> None:
    text = "\n".join(block.content for block in context.blocks if block.content).strip()
    if not text:
        raise ValueError("No text extracted from resume")

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    sections: OrderedDict[str, list[str]] = OrderedDict((key, []) for key in SECTION_KEYWORDS)
    sections["overview"] = []
    current = "overview"

    for line in lines:
        lowered = line.lower()
        matched = False
        for section_name, keywords in SECTION_KEYWORDS.items():
            if any(keyword.lower() in lowered for keyword in keywords):
                current = section_name
                matched = True
                break
        if matched:
            continue
        sections.setdefault(current, []).append(line)

    context.sections = {key: "\n".join(values).strip() for key, values in sections.items() if values}

    phone_match = re.search(r"(?<!\d)(1[3-9]\d{9})(?!\d)", text)
    email_match = re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", text)
    name_match = re.search(r"(?:姓名|Name)\s*[:：]\s*([^\n]+)", text, re.IGNORECASE)
    if phone_match:
        context.fields["phone"] = ParseFieldEvidence(value=phone_match.group(1), confidence=0.95, source="regex")
    if email_match:
        context.fields["email"] = ParseFieldEvidence(value=email_match.group(0), confidence=0.95, source="regex")
    if name_match:
        context.fields["name"] = ParseFieldEvidence(value=name_match.group(1).strip(), confidence=0.98, source="regex")

    first_line = lines[0] if lines else ""
    if "name" not in context.fields and first_line and len(first_line) <= 20 and re.search(r"[\u4e00-\u9fffA-Za-z]", first_line):
        context.fields["name"] = ParseFieldEvidence(value=first_line, confidence=0.65, source="header_guess")

    if not any(section in context.sections for section in ("education", "work_experience", "projects", "skills")):
        context.issues.append(
            ParseIssue(
                severity="WARN",
                message="Resume sections are weakly structured; extraction quality may be lower.",
            )
        )

    context.highlights = [
        "Initial text extraction and section normalization completed.",
        f"Extracted {len(context.blocks)} raw content blocks from the resume source.",
    ]
