from pathlib import Path

from pydantic import Field, computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    llm_api_key: str
    llm_base_url: str
    llm_model: str = Field(default="")
    llm_endpoint_id: str = Field(default="")
    llm_timeout_seconds: float = Field(default=120.0)

    model_config = SettingsConfigDict(
        env_file=(
            Path(__file__).parent.parent / ".env",
            Path(__file__).parent.parent.parent / "parse_pdf" / ".env",
        ),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @computed_field
    @property
    def normalized_llm_base_url(self) -> str:
        base_url = self.llm_base_url.strip().rstrip("/")
        if base_url.endswith("/api/coding/v3"):
            return base_url[: -len("/api/coding/v3")] + "/api/v3"
        return base_url

    @computed_field
    @property
    def resolved_llm_model(self) -> str:
        endpoint_id = self.llm_endpoint_id.strip()
        model = self.llm_model.strip()
        return endpoint_id or model


settings = Settings()
