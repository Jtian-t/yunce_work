import re
from typing import Type, TypeVar

from openai import OpenAI
from pydantic import BaseModel

from src.config import settings

T = TypeVar("T", bound=BaseModel)


class LLMClient:
    def __init__(self):
        self.client = OpenAI(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
        )
        self.model = settings.llm_model

    def _extract_json_from_text(self, text: str) -> str:
        json_match = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", text)
        if json_match:
            return json_match.group(1)
        return text.strip()

    def chat_with_structured_output(
        self,
        system_prompt: str,
        user_prompt: str,
        output_model: Type[T],
        max_retries: int = 3,
    ) -> T:
        for attempt in range(max_retries):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=0.3,
                )

                content = response.choices[0].message.content
                if not content:
                    raise ValueError("LLM returned empty content")

                json_str = self._extract_json_from_text(content)
                return output_model.model_validate_json(json_str)
            except Exception:
                if attempt == max_retries - 1:
                    raise

        raise ValueError("Failed to get structured output from LLM")


llm_client = LLMClient()
