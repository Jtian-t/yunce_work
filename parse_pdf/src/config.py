
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    llm_api_key: str = ""
    llm_base_url: str = "https://api.openai.com/v1"
    llm_model: str = "gpt-4-turbo-preview"

    class Config:
        env_file = ".env"


settings = Settings()

