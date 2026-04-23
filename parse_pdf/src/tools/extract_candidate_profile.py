from __future__ import annotations

import json
import re

from src.agent_runtime.context import ParseAgentContext
from src.llm_client import llm_client
from src.schemas import (
    CandidateInfo,
    Education,
    ParseEducation,
    ParseExperience,
    ParseFieldEvidence,
    ParseProjectDetail,
    ParseProjectSummary,
    ParseRawBlock,
    ParseReport,
    ParseSkill,
    Project,
    WorkExperience,
)


def extract_candidate_profile(context: ParseAgentContext) -> None:
    sections_json = json.dumps(context.sections, ensure_ascii=False, indent=2)
    extracted_fields = json.dumps(
        {key: value.model_dump() for key, value in context.fields.items()},
        ensure_ascii=False,
        indent=2,
    )
    candidate = llm_client.chat_with_structured_output(
        system_prompt=(
            "You extract structured candidate information from resumes. "
            "Return JSON only that matches the CandidateInfo schema."
        ),
        user_prompt=(
            "请根据已经提取出的简历片段和字段证据，生成标准 JSON。"
            "请尽量补全姓名、联系方式、个人总结、工作经历、项目经历、教育经历和技能。"
            "如果某个字段没有足够证据，请返回空值或空数组，不要编造。\n\n"
            f"字段证据:\n{extracted_fields}\n\n"
            f"简历分段内容:\n{sections_json}"
        ),
        output_model=CandidateInfo,
        max_retries=2,
    )

    if not candidate.name and "name" in context.fields:
        candidate.name = context.fields["name"].value
    if not candidate.phone and "phone" in context.fields:
        candidate.phone = context.fields["phone"].value
    if not candidate.email and "email" in context.fields:
        candidate.email = context.fields["email"].value
    _apply_rule_based_fallbacks(context, candidate)

    context.candidate_info = candidate
    context.fields.setdefault(
        "name",
        ParseFieldEvidence(value=candidate.name, confidence=0.75 if candidate.name else 0.0, source="llm"),
    )
    if candidate.summary:
        context.fields["summary"] = ParseFieldEvidence(value=candidate.summary, confidence=0.72, source="llm")
    if candidate.skills:
        context.fields["skillsSummary"] = ParseFieldEvidence(
            value=", ".join(candidate.skills),
            confidence=0.72,
            source="llm",
        )
    if candidate.projects:
        context.fields["projectSummary"] = ParseFieldEvidence(
            value=candidate.projects[0].description or candidate.projects[0].name,
            confidence=0.68,
            source="llm",
        )
    if candidate.work_experience:
        context.fields["experience"] = ParseFieldEvidence(
            value="；".join(
                filter(
                    None,
                    [
                        f"{item.company} {item.position}".strip()
                        for item in candidate.work_experience
                    ],
                )
            ),
            confidence=0.7,
            source="llm",
        )
    if candidate.education:
        context.fields["education"] = ParseFieldEvidence(
            value="；".join(filter(None, [f"{item.school} {item.degree}".strip() for item in candidate.education])),
            confidence=0.7,
            source="llm",
        )

    context.parse_report = ParseReport(
        summary=candidate.summary or "Structured resume parsing completed.",
        highlights=list(dict.fromkeys(context.highlights + _candidate_highlights(candidate))),
        extracted_skills=candidate.skills,
        project_experiences=[
            ParseProjectSummary(title=project.name or "未命名项目", summary=project.description or project.role or "")
            for project in candidate.projects
        ],
        skills=[
            ParseSkill(raw_term=skill, normalized_name=skill, source_snippet="resume_profile", confidence=0.75)
            for skill in candidate.skills
        ],
        projects=[
            ParseProjectDetail(
                project_name=project.name,
                period=None,
                role=project.role,
                tech_stack=project.tech_stack,
                responsibilities=[project.description] if project.description else [],
                achievements=[],
                summary=project.description or project.role or "",
            )
            for project in candidate.projects
        ],
        experiences=[
            ParseExperience(
                company=item.company,
                role=item.position,
                period=item.duration,
                summary=item.description,
            )
            for item in candidate.work_experience
        ],
        educations=[
            ParseEducation(
                school=item.school,
                degree=item.degree,
                period=item.duration,
                summary=item.major,
            )
            for item in candidate.education
        ],
        raw_blocks=[
            ParseRawBlock(block_type=block.block_type, title=block.title, content=block.content)
            for block in context.blocks
        ],
        fields=context.fields,
        issues=context.issues,
        extraction_mode=context.extraction_mode,
        ocr_required=context.ocr_required,
    )


def _candidate_highlights(candidate: CandidateInfo) -> list[str]:
    highlights = []
    if candidate.skills:
        highlights.append(f"识别出 {len(candidate.skills)} 项技能关键词。")
    if candidate.work_experience:
        highlights.append(f"识别出 {len(candidate.work_experience)} 段工作经历。")
    if candidate.projects:
        highlights.append(f"识别出 {len(candidate.projects)} 个项目经历。")
    if candidate.education:
        highlights.append("识别到教育背景信息。")
    return highlights


KNOWN_SKILLS = [
    "Java",
    "Python",
    "Spring",
    "Spring Boot",
    "SpringMVC",
    "SpringCloud",
    "MyBatis",
    "Mybatis",
    "MyBatisPlus",
    "MySQL",
    "Redis",
    "Redisson",
    "RabbitMQ",
    "Kafka",
    "Nacos",
    "Sentinel",
    "Seata",
    "JVM",
    "Docker",
    "Maven",
    "Git",
    "LangChain",
    "RAG",
    "Agent",
    "Tool Calls",
    "XXL-Job",
]


def _apply_rule_based_fallbacks(context: ParseAgentContext, candidate: CandidateInfo) -> None:
    full_text = "\n".join(block.content for block in context.blocks if block.content)

    if not candidate.name:
        name_match = re.search(r"(?:姓名|Name)\s*[:：]\s*([^\n]+)", full_text, re.IGNORECASE)
        if name_match:
            candidate.name = name_match.group(1).strip()
            context.fields["name"] = ParseFieldEvidence(value=candidate.name, confidence=0.98, source="regex")

    if not candidate.skills:
        seen = []
        lowered = full_text.lower()
        for skill in KNOWN_SKILLS:
            if skill.lower() in lowered and skill not in seen:
                seen.append(skill)
        candidate.skills = seen

    if not candidate.summary:
        summary_match = re.search(r"(?:自我评价|个人总结|Summary)\s*\n(.+)", full_text, re.IGNORECASE)
        if summary_match:
            candidate.summary = summary_match.group(1).strip()

    if not candidate.education:
        education_entries = []
        lines = [line.strip() for line in full_text.splitlines() if line.strip()]
        for index, line in enumerate(lines):
            if ("大学" in line or "学院" in line) and ("本科" in line or "硕士" in line or "博士" in line):
                period = lines[index - 1] if index > 0 and re.search(r"\d{4}", lines[index - 1]) else ""
                school_degree = line.split()
                school = school_degree[0]
                degree = ""
                for token in school_degree[1:]:
                    if token in {"本科", "硕士", "博士"}:
                        degree = token
                        break
                major = lines[index + 1] if index + 1 < len(lines) and len(lines[index + 1]) <= 20 else ""
                education_entries.append(
                    Education(school=school, degree=degree, major=major, duration=period)
                )
        if education_entries:
            candidate.education = education_entries

    if not candidate.projects:
        project_entries = []
        lines = [line.strip() for line in full_text.splitlines() if line.strip()]
        for index, line in enumerate(lines):
            if len(line) > 4 and len(line) <= 40 and not re.search(r"\d{4}", line):
                if "系统" in line or "平台" in line or "项目" in line or "Agent" in line:
                    if line not in {"项目经验", "Project Experience", "校园经历"}:
                        detail = lines[index + 1] if index + 1 < len(lines) else ""
                        tech_stack = [skill for skill in KNOWN_SKILLS if skill.lower() in detail.lower()]
                        project_entries.append(
                            Project(
                                name=line,
                                role="",
                                description=detail,
                                tech_stack=tech_stack,
                            )
                        )
        if project_entries:
            deduped = []
            seen_names = set()
            for item in project_entries:
                if item.name in seen_names:
                    continue
                seen_names.add(item.name)
                deduped.append(item)
            candidate.projects = deduped[:3]

    if not candidate.work_experience:
        experience_entries = []
        lines = [line.strip() for line in full_text.splitlines() if line.strip()]
        for index, line in enumerate(lines):
            if re.search(r"\d{4}[-./]\d{2}\s*[~-]\s*(?:\d{4}[-./]\d{2}|至今)", line):
                if index + 1 < len(lines):
                    next_line = lines[index + 1]
                    if any(keyword in next_line for keyword in ("公司", "科技", "集团", "有限公司")):
                        experience_entries.append(
                            WorkExperience(
                                company=next_line,
                                position=lines[index + 2] if index + 2 < len(lines) else "",
                                duration=line,
                                description=lines[index + 3] if index + 3 < len(lines) else "",
                            )
                        )
        if experience_entries:
            candidate.work_experience = experience_entries
