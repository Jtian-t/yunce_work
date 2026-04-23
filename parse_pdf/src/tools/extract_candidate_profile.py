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


KNOWN_SKILLS = [
    "Java",
    "Python",
    "Spring Boot",
    "SpringCloud",
    "SpringMVC",
    "Spring",
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
    "LangChain",
    "RAG",
    "Tool Calls",
    "Agent",
    "Docker",
    "Maven",
    "Git",
    "JVM",
    "XXL-Job",
]

SKILL_LINE_PREFIXES = ("Java", "计算机基础", "数据库", "Redis", "框架", "JVM", "AI Agent", "工具", "技术栈")


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
            "如果某个字段没有足够证据，请返回空值或空数组，不要编造。"
            "对于技能，请优先返回标准技术名，不要返回大段整句说明。"
            "对于项目经历，不要把技能描述或教育内容误判成项目。"
            "对于工作经历，不要把学校经历误判成公司经历。\n\n"
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
            confidence=0.8,
            source="rule+llm",
        )
    if candidate.projects:
        context.fields["projectSummary"] = ParseFieldEvidence(
            value=candidate.projects[0].description or candidate.projects[0].name,
            confidence=0.72,
            source="rule+llm",
        )
    if candidate.work_experience:
        context.fields["experience"] = ParseFieldEvidence(
            value="；".join(
                filter(None, [f"{item.company} {item.position}".strip() for item in candidate.work_experience])
            ),
            confidence=0.75,
            source="rule+llm",
        )
    if candidate.education:
        context.fields["education"] = ParseFieldEvidence(
            value="；".join(filter(None, [f"{item.school} {item.degree}".strip() for item in candidate.education])),
            confidence=0.8,
            source="rule+llm",
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
            ParseSkill(raw_term=skill, normalized_name=skill, source_snippet="resume_profile", confidence=0.82)
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


def _apply_rule_based_fallbacks(context: ParseAgentContext, candidate: CandidateInfo) -> None:
    full_text = "\n".join(block.content for block in context.blocks if block.content)
    lines = [line.strip() for line in full_text.splitlines() if line.strip()]

    if not candidate.name:
        name_match = re.search(r"(?:姓名|name)\s*[:：]\s*([^\n]+)", full_text, re.IGNORECASE)
        if name_match:
            candidate.name = name_match.group(1).strip()
            context.fields["name"] = ParseFieldEvidence(value=candidate.name, confidence=0.98, source="regex")

    candidate.skills = _clean_skills(candidate.skills, lines)
    if not candidate.summary:
        candidate.summary = _extract_summary(context.sections, lines)
    candidate.education = _merge_education(candidate.education, lines)
    candidate.projects = _merge_projects(candidate.projects, context.sections, lines)
    candidate.work_experience = _merge_experiences(candidate.work_experience, lines, candidate.education)


def _clean_skills(existing: list[str], lines: list[str]) -> list[str]:
    seen: list[str] = []
    for skill in existing:
        normalized = _normalize_skill_text(skill)
        if normalized and normalized not in seen:
            seen.append(normalized)

    for line in lines:
        if not line.startswith(SKILL_LINE_PREFIXES):
            continue
        for skill in KNOWN_SKILLS:
            if skill.lower() in line.lower() and skill not in seen:
                seen.append(skill)
    return seen


def _normalize_skill_text(value: str) -> str:
    text = value.strip()
    if not text:
        return ""
    for skill in sorted(KNOWN_SKILLS, key=len, reverse=True):
        if skill.lower() in text.lower():
            return skill
    return ""


def _extract_summary(sections: dict[str, str], lines: list[str]) -> str:
    summary_text = sections.get("summary", "").strip()
    if summary_text:
        return summary_text.replace("\n", "")
    for index, line in enumerate(lines):
        if line.lower() in {"自我评价", "self-evaluation", "个人总结", "summary"}:
            return "".join(lines[index + 1 : index + 3]).strip()
    return ""


def _merge_education(existing: list[Education], lines: list[str]) -> list[Education]:
    merged: list[Education] = []
    seen = set()
    for item in existing:
        key = (item.school, item.degree, item.duration)
        if item.school and key not in seen:
            seen.add(key)
            merged.append(item)

    for index, line in enumerate(lines):
        if not (("大学" in line or "学院" in line) and any(token in line for token in ("本科", "硕士", "博士"))):
            continue
        period = lines[index - 1] if index > 0 and re.search(r"\d{4}", lines[index - 1]) else ""
        school = ""
        degree = ""
        for token in line.split():
            if not school:
                school = token
            if token in {"本科", "硕士", "博士"}:
                degree = token
                break
        major = lines[index + 1] if index + 1 < len(lines) and len(lines[index + 1]) <= 20 else ""
        entry = Education(school=school, degree=degree, major=major, duration=period)
        key = (entry.school, entry.degree, entry.duration)
        if entry.school and key not in seen:
            seen.add(key)
            merged.append(entry)
    return merged


def _merge_projects(existing: list[Project], sections: dict[str, str], lines: list[str]) -> list[Project]:
    merged: list[Project] = []
    seen = set()

    for item in existing:
        if _looks_like_valid_project_name(item.name):
            key = item.name
            if key not in seen:
                seen.add(key)
                item.tech_stack = _dedupe_skills(item.tech_stack)
                merged.append(item)

    project_section = sections.get("projects", "")
    section_lines = [line.strip() for line in project_section.splitlines() if line.strip()]
    for index, line in enumerate(section_lines):
        if not _looks_like_valid_project_name(line):
            continue
        detail = section_lines[index + 1] if index + 1 < len(section_lines) else ""
        project = Project(
            name=line,
            role="",
            description=detail,
            tech_stack=[skill for skill in KNOWN_SKILLS if skill.lower() in detail.lower()],
        )
        if project.name not in seen:
            seen.add(project.name)
            project.tech_stack = _dedupe_skills(project.tech_stack)
            merged.append(project)

    for index, line in enumerate(lines):
        if not _looks_like_valid_project_name(line):
            continue
        window = " ".join(lines[index + 1 : index + 4])
        if not any(keyword in window for keyword in ("项目背景", "技术栈", "核心工作内容", "项目")):
            continue
        project = Project(
            name=line,
            role="",
            description=window,
            tech_stack=[skill for skill in KNOWN_SKILLS if skill.lower() in window.lower()],
        )
        if project.name not in seen:
            seen.add(project.name)
            project.tech_stack = _dedupe_skills(project.tech_stack)
            merged.append(project)

    return merged[:5]


def _looks_like_valid_project_name(line: str) -> bool:
    if not line or len(line) < 5 or len(line) > 40:
        return False
    if any(keyword in line for keyword in ("技能", "技术栈", "工具", "专业技能", "教育", "大学", "学院", "工作经历")):
        return False
    if re.search(r"^\d{4}", line):
        return False
    return any(keyword in line for keyword in ("系统", "平台", "项目", "Agent"))


def _merge_experiences(existing: list[WorkExperience], lines: list[str], education: list[Education]) -> list[WorkExperience]:
    merged: list[WorkExperience] = []
    seen = set()
    education_schools = {item.school for item in education if item.school}

    for item in existing:
        if (
            item.company
            and item.company not in education_schools
            and not any(word in item.company for word in ("大学", "学院", "本科", "硕士", "博士"))
        ):
            key = (item.company, item.position, item.duration)
            if key not in seen:
                seen.add(key)
                merged.append(item)

    for index, line in enumerate(lines):
        if not re.search(r"\d{4}[-./]\d{2}\s*[~-]\s*(?:\d{4}[-./]\d{2}|至今)", line):
            continue
        window = lines[index + 1 : index + 5]
        company = next((item for item in window if any(word in item for word in ("公司", "科技", "集团", "有限公司"))), "")
        if not company or company in education_schools:
            continue
        if any(word in company for word in ("大学", "学院", "本科", "硕士", "博士")):
            continue
        position = ""
        description = ""
        if company in window:
            company_index = window.index(company)
            if company_index + 1 < len(window):
                position = window[company_index + 1]
            if company_index + 2 < len(window):
                description = window[company_index + 2]
        entry = WorkExperience(company=company, position=position, duration=line, description=description)
        key = (entry.company, entry.position, entry.duration)
        if key not in seen:
            seen.add(key)
            merged.append(entry)
    return merged


def _dedupe_skills(skills: list[str]) -> list[str]:
    result: list[str] = []
    for skill in skills:
        normalized = _normalize_skill_text(skill)
        if normalized and normalized not in result:
            result.append(normalized)
    return result
