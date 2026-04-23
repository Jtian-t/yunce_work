from pydantic import BaseModel, Field


class AgentAction(BaseModel):
    tool_name: str = Field(..., description="Tool name to invoke next")
    reason: str = Field(default="", description="Why this tool is selected")
    done: bool = Field(default=False, description="Whether the goal is already complete")
