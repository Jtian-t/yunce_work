from __future__ import annotations

import json
import re

from langchain.output_parsers import PydanticOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from src.config import settings
from src.prompts.resume_parse_prompt import SYSTEM_PROMPT, USER_PROMPT
from src.schemas import ResumeProfile


class ResumeParseChain:
    def __init__(self) -> None:
        self.parser = PydanticOutputParser(pydantic_object=ResumeProfile)
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

    def invoke(self, *, resume_text: str, hint: str = "") -> ResumeProfile:
        messages = self.prompt.format_messages(
            hint=hint or "无额外提示，默认来源为简历上传。",
            resume_text=resume_text[:18000],
            format_instructions=self.parser.get_format_instructions(),
        )
        response = self.llm.invoke(messages)
        content = self._extract_content(response.content)
        try:
            return self.parser.parse(content)
        except Exception:
            return ResumeProfile.model_validate_json(self._extract_json(content))

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
