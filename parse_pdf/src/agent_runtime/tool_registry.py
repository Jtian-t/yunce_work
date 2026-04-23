from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable, Dict


ToolHandler = Callable[..., Any]


@dataclass
class RegisteredTool:
    name: str
    description: str
    handler: ToolHandler


class ToolRegistry:
    def __init__(self):
        self._tools: Dict[str, RegisteredTool] = {}

    def register(self, name: str, description: str, handler: ToolHandler) -> None:
        self._tools[name] = RegisteredTool(name=name, description=description, handler=handler)

    def execute(self, name: str, context: Any) -> Any:
        tool = self._tools.get(name)
        if tool is None:
            raise KeyError(f"Unknown tool: {name}")
        return tool.handler(context)

    def contains(self, name: str) -> bool:
        return name in self._tools

    def descriptions(self) -> str:
        return "\n".join(f"- {tool.name}: {tool.description}" for tool in self._tools.values())
