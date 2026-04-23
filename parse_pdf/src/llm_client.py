import re
from typing import Type, TypeVar

from openai import APITimeoutError, APIConnectionError, BadRequestError, NotFoundError, OpenAI
from pydantic import BaseModel

from src.config import settings

T = TypeVar("T", bound=BaseModel)


class LLMClient:
    def __init__(self):
        effective_timeout = max(float(settings.llm_timeout_seconds), 180.0)
        self.client = OpenAI(
            base_url=settings.normalized_llm_base_url,
            api_key=settings.llm_api_key,
            timeout=effective_timeout,
            max_retries=0,
        )
        self.model = settings.resolved_llm_model

    def _extract_json_from_text(self, text: str) -> str:
        json_match = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", text)
        if json_match:
            return json_match.group(1)
        return text.strip()

    def _extract_content(self, response) -> str:
        message = response.choices[0].message
        content = message.content
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            text_parts = []
            for item in content:
                text = getattr(item, "text", None)
                if text:
                    text_parts.append(text)
            return "\n".join(text_parts).strip()
        return ""

    def _create_chat_completion(self, system_prompt: str, user_prompt: str):
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
        try:
            return self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.3,
                response_format={"type": "json_object"},
            )
        except BadRequestError as exc:
            message = str(exc)
            if "json_object" not in message and "response_format" not in message:
                raise
            return self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.3,
            )

    def _ark_hint(self) -> str:
        if not settings.is_ark_provider:
            return ""
        if settings.llm_endpoint_id.strip():
            return (
                " Ark provider detected. Current request uses LLM_ENDPOINT_ID="
                f"{settings.llm_endpoint_id.strip()}."
            )
        return (
            " Ark provider detected. For Doubao, `model` usually needs the Ark "
            "inference endpoint ID, not the raw model name. Please set "
            "`LLM_ENDPOINT_ID=ep-...` in parse_pdf/.env."
        )

    def _validate_settings(self) -> None:
        if not self.model:
            raise ValueError(
                "Missing model configuration. Set LLM_ENDPOINT_ID for Ark/Doubao "
                "or LLM_MODEL for other providers."
            )

    def chat_with_structured_output(
        self,
        system_prompt: str,
        user_prompt: str,
        output_model: Type[T],
        max_retries: int = 3,
    ) -> T:
        self._validate_settings()

        for attempt in range(max_retries):
            try:
                response = self._create_chat_completion(system_prompt, user_prompt)

                content = self._extract_content(response)
                if not content:
                    raise ValueError("LLM returned empty content")

                json_str = self._extract_json_from_text(content)
                return output_model.model_validate_json(json_str)
            except NotFoundError as exc:
                if attempt == max_retries - 1:
                    raise RuntimeError(
                        "LLM endpoint/model not found. "
                        f"base_url={settings.normalized_llm_base_url}, "
                        f"model={self.model}.{self._ark_hint()}"
                    ) from exc
            except (APITimeoutError, APIConnectionError) as exc:
                if attempt == max_retries - 1:
                    raise RuntimeError(
                        "LLM upstream request failed. "
                        f"base_url={settings.normalized_llm_base_url}, "
                        f"model={self.model}, reason={exc}.{self._ark_hint()}"
                    ) from exc
            except Exception as exc:
                if attempt == max_retries - 1:
                    raise RuntimeError(
                        f"LLM request failed with base_url={settings.normalized_llm_base_url}, "
                        f"model={self.model}: {exc}.{self._ark_hint()}"
                    ) from exc

        raise ValueError("Failed to get structured output from LLM")


llm_client = LLMClient()
