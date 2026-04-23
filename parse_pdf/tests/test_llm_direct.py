"""
Direct LLM connectivity test.

This test does not require FastAPI. It only checks whether the current
LLM settings in parse_pdf/.env can successfully call the configured model.
"""

import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from src.config import settings
from src.llm_client import llm_client


def test_llm_connectivity():
    print("=== LLM Direct Test ===")
    print(f"base_url={settings.normalized_llm_base_url}")
    print(f"model={settings.resolved_llm_model}")
    print(f"is_ark_provider={settings.is_ark_provider}")

    response = llm_client.client.chat.completions.create(
        model=llm_client.model,
        messages=[
            {"role": "system", "content": "You are a concise assistant."},
            {
                "role": "user",
                "content": "说一下你能做些什么，并简单打个招呼。用标准的json格式返回",
            },

        ],
        response_format={"type": "json_object"},
        temperature=0.3,
    )

    content = response.choices[0].message.content
    print("raw_content=")
    print(content)

    assert content, "LLM returned empty content"
    return content


if __name__ == "__main__":
    result = test_llm_connectivity()
    print("=== Final Result ===")
    print(result)
