from __future__ import annotations

from dataclasses import dataclass
from typing import List

from src.agent_runtime.actions import AgentAction
from src.agent_runtime.context import DecisionAgentContext, ParseAgentContext
from src.agent_runtime.tool_registry import ToolRegistry
from src.llm_client import llm_client
from src.schemas import DecisionReport, ParseReport
from src.tools.aggregate_interview_feedback import aggregate_interview_feedback
from src.tools.detect_ocr_need import detect_ocr_need
from src.tools.extract_candidate_profile import extract_candidate_profile
from src.tools.extract_pdf_blocks import extract_pdf_blocks
from src.tools.generate_optimization_suggestions import generate_optimization_suggestions
from src.tools.load_resume_source import load_resume_source
from src.tools.normalize_resume_sections import normalize_resume_sections
from src.tools.ocr_pdf_pages import ocr_pdf_pages
from src.tools.score_job_fit import score_job_fit


PARSE_TOOL_SEQUENCE = [
    "load_resume_source",
    "extract_pdf_blocks",
    "detect_ocr_need",
    "ocr_pdf_pages",
    "normalize_resume_sections",
    "extract_candidate_profile",
]

DECISION_TOOL_SEQUENCE = [
    "aggregate_interview_feedback",
    "score_job_fit",
    "generate_optimization_suggestions",
]


@dataclass
class ParseAgentRunner:
    max_steps: int = 8

    def __post_init__(self):
        self.registry = ToolRegistry()
        self.registry.register("load_resume_source", "Load resume text, local file, or remote file URL.", load_resume_source)
        self.registry.register("extract_pdf_blocks", "Extract text blocks from PDF or text input.", extract_pdf_blocks)
        self.registry.register("detect_ocr_need", "Decide whether OCR fallback is required.", detect_ocr_need)
        self.registry.register("ocr_pdf_pages", "Run OCR on PDF pages when text extraction quality is low.", ocr_pdf_pages)
        self.registry.register("normalize_resume_sections", "Organize raw blocks into resume sections and field hints.", normalize_resume_sections)
        self.registry.register("extract_candidate_profile", "Generate structured candidate profile from normalized sections.", extract_candidate_profile)

    def run(self, context: ParseAgentContext) -> ParseReport:
        for _ in range(self.max_steps):
            action = self._choose_next_action(context)
            if action.done:
                break
            self.registry.execute(action.tool_name, context)
            context.step_logs.append(f"{action.tool_name}: {action.reason}")
            if context.parse_report is not None:
                return context.parse_report
        if context.parse_report is None:
            self.registry.execute("extract_candidate_profile", context)
        if context.parse_report is None:
            raise RuntimeError("Parse agent did not produce a parse report")
        return context.parse_report

    def _choose_next_action(self, context: ParseAgentContext) -> AgentAction:
        fallback = self._fallback_action(context)
        viable_tools = [name for name in PARSE_TOOL_SEQUENCE if self._is_viable(context, name)]
        if len(viable_tools) <= 1:
            return fallback
        prompt = (
            "You are coordinating a resume parsing agent. "
            "Choose the next tool from the list and return JSON only.\n"
            f"Available tools:\n{self.registry.descriptions()}\n"
            f"Current state:\n"
            f"- source_loaded={context.source is not None}\n"
            f"- blocks={len(context.blocks)}\n"
            f"- ocr_required={context.ocr_required}\n"
            f"- sections={list(context.sections.keys())}\n"
            f"- candidate_ready={context.candidate_info is not None}\n"
            f"- report_ready={context.parse_report is not None}\n"
            f"Return JSON with tool_name, reason, done."
        )
        try:
            action = llm_client.chat_with_structured_output(
                system_prompt="Return JSON only.",
                user_prompt=prompt,
                output_model=AgentAction,
                max_retries=1,
            )
            if action.done:
                return action
            if not self.registry.contains(action.tool_name) or not self._is_viable(context, action.tool_name):
                return fallback
            return action
        except Exception:
            return fallback

    def _fallback_action(self, context: ParseAgentContext) -> AgentAction:
        if context.source is None:
            return AgentAction(tool_name="load_resume_source", reason="Source has not been loaded yet.")
        if not context.blocks:
            return AgentAction(tool_name="extract_pdf_blocks", reason="No extracted blocks are available.")
        if "ocr_checked" not in context.metadata:
            return AgentAction(tool_name="detect_ocr_need", reason="OCR requirement has not been evaluated.")
        if context.ocr_required and not context.metadata.get("ocr_completed"):
            return AgentAction(tool_name="ocr_pdf_pages", reason="OCR fallback is required.")
        if not context.sections:
            return AgentAction(tool_name="normalize_resume_sections", reason="Resume sections are not prepared.")
        if context.candidate_info is None or context.parse_report is None:
            return AgentAction(tool_name="extract_candidate_profile", reason="Structured profile has not been produced.")
        return AgentAction(tool_name="extract_candidate_profile", reason="Parsing is complete.", done=True)

    def _is_viable(self, context: ParseAgentContext, tool_name: str) -> bool:
        if tool_name == "load_resume_source":
            return context.source is None
        if tool_name == "extract_pdf_blocks":
            return context.source is not None and not context.blocks
        if tool_name == "detect_ocr_need":
            return bool(context.blocks) and "ocr_checked" not in context.metadata
        if tool_name == "ocr_pdf_pages":
            return context.ocr_required and not context.metadata.get("ocr_completed")
        if tool_name == "normalize_resume_sections":
            return bool(context.blocks) and not context.sections
        if tool_name == "extract_candidate_profile":
            return bool(context.sections) and context.parse_report is None
        return False


@dataclass
class DecisionAgentRunner:
    max_steps: int = 6

    def __post_init__(self):
        self.registry = ToolRegistry()
        self.registry.register("aggregate_interview_feedback", "Summarize and normalize interview feedback.", aggregate_interview_feedback)
        self.registry.register("score_job_fit", "Estimate job fit from candidate profile, JD, and feedback.", score_job_fit)
        self.registry.register("generate_optimization_suggestions", "Produce final decision report and suggestions.", generate_optimization_suggestions)

    def run(self, context: DecisionAgentContext) -> DecisionReport:
        for _ in range(self.max_steps):
            action = self._choose_next_action(context)
            if action.done:
                break
            self.registry.execute(action.tool_name, context)
            context.step_logs.append(f"{action.tool_name}: {action.reason}")
            if context.decision_report is not None:
                return context.decision_report
        if context.decision_report is None:
            self.registry.execute("generate_optimization_suggestions", context)
        if context.decision_report is None:
            raise RuntimeError("Decision agent did not produce a decision report")
        return context.decision_report

    def _choose_next_action(self, context: DecisionAgentContext) -> AgentAction:
        fallback = self._fallback_action(context)
        viable_tools = [name for name in DECISION_TOOL_SEQUENCE if self._is_viable(context, name)]
        if len(viable_tools) <= 1:
            return fallback
        prompt = (
            "You are coordinating a hiring decision agent. "
            "Choose the next tool from the list and return JSON only.\n"
            f"Available tools:\n{self.registry.descriptions()}\n"
            f"Current state:\n"
            f"- has_candidate={context.candidate_info is not None}\n"
            f"- has_parse_report={context.parse_report is not None}\n"
            f"- feedback_count={len(context.feedbacks)}\n"
            f"- feedback_aggregate_ready={context.feedback_aggregate is not None}\n"
            f"- decision_ready={context.decision_report is not None}\n"
            "Return JSON with tool_name, reason, done."
        )
        try:
            action = llm_client.chat_with_structured_output(
                system_prompt="Return JSON only.",
                user_prompt=prompt,
                output_model=AgentAction,
                max_retries=1,
            )
            if action.done:
                return action
            if not self.registry.contains(action.tool_name) or not self._is_viable(context, action.tool_name):
                return fallback
            return action
        except Exception:
            return fallback

    def _fallback_action(self, context: DecisionAgentContext) -> AgentAction:
        if context.feedback_aggregate is None:
            return AgentAction(tool_name="aggregate_interview_feedback", reason="Feedback aggregate is missing.")
        if "fit_score" not in context.metadata:
            return AgentAction(tool_name="score_job_fit", reason="Job fit scoring has not been generated.")
        if context.decision_report is None:
            return AgentAction(tool_name="generate_optimization_suggestions", reason="Final decision report is missing.")
        return AgentAction(tool_name="generate_optimization_suggestions", reason="Decision is complete.", done=True)

    def _is_viable(self, context: DecisionAgentContext, tool_name: str) -> bool:
        if tool_name == "aggregate_interview_feedback":
            return context.feedback_aggregate is None
        if tool_name == "score_job_fit":
            return context.feedback_aggregate is not None and "fit_score" not in context.metadata
        if tool_name == "generate_optimization_suggestions":
            return "fit_score" in context.metadata and context.decision_report is None
        return False
