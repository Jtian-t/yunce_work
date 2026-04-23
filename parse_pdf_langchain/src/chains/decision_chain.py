from __future__ import annotations

import json
import re

from langchain.output_parsers import PydanticOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from src.config import settings
from src.prompts.decision_prompt import SYSTEM_PROMPT, USER_PROMPT
from src.schemas import DecisionReport, InterviewFeedback, ResumeProfile


class DecisionChain:
    def __init__(self) -> None:
        self.parser = PydanticOutputParser(pydantic_object=DecisionReport)
        self.prompt = ChatPromptTemplate.from_messages(
            [
                ("system", SYSTEM_PROMPT),
                ("human", USER_PROMPT),
            ]
        )
        self.llm = ChatOpenAI(
            model_name=settings.resolved_llm_model,
            openai_api_key=settings.llm_api_key,
            openai_api_base=settings.normalized_llm_base_url,
            temperature=0.1,
            timeout=max(float(settings.llm_timeout_seconds), 60.0),
            max_retries=1,
        )

    def invoke(
        self,
        *,
        resume_profile: ResumeProfile,
        job_requirements: str,
        interview_feedbacks: list[InterviewFeedback],
    ) -> DecisionReport:
        feedback_payload = [item.model_dump() for item in interview_feedbacks]
        messages = self.prompt.format_messages(
            job_requirements=job_requirements or "未提供岗位要求，请基于通用招聘视角评估。",
            resume_profile=json.dumps(resume_profile.model_dump(by_alias=True), ensure_ascii=False, indent=2),
            interview_feedbacks=json.dumps(feedback_payload, ensure_ascii=False, indent=2),
            format_instructions=self.parser.get_format_instructions(),
        )
        response = self.llm.invoke(messages)
        content = self._extract_content(response.content)
        try:
            report = self.parser.parse(content)
        except Exception:
            report = DecisionReport.model_validate_json(self._extract_json(content))
        return self._normalize_report(report)

    def _extract_content(self, content) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts = []
            for item in content:
                text = getattr(item, "text", None)
                if text:
                    parts.append(text)
            return "\n".join(parts).strip()
        return str(content)

    def _extract_json(self, text: str) -> str:
        fenced = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", text)
        if fenced:
            return fenced.group(1)
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            return text[start : end + 1]
        return json.dumps({})

    def _normalize_report(self, report: DecisionReport) -> DecisionReport:
        if not report.recommendation_level:
            score = report.recommendation_score
            if score >= 85:
                report.recommendation_level = "强烈推荐"
            elif score >= 72:
                report.recommendation_level = "建议推进"
            elif score >= 58:
                report.recommendation_level = "保守推进"
            elif score >= 45:
                report.recommendation_level = "建议补充信息"
            else:
                report.recommendation_level = "暂缓推进"
        report.recommendation_score = max(0, min(100, report.recommendation_score))
        return report
