from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

from src.schemas import (
    CandidateInfo,
    DecisionReport,
    InterviewFeedback,
    InterviewRoundSummary,
    ParseFieldEvidence,
    ParseIssue,
    ParseReport,
    ResumeBlock,
    ResumeDecisionReportRequest,
    ResumeSourceRequest,
)


@dataclass
class ResumeSource:
    kind: str
    file_name: str = ""
    mime_type: str = ""
    raw_bytes: bytes = b""
    text: str = ""
    source_uri: str = ""


@dataclass
class FeedbackAggregate:
    summary: str = ""
    positives: List[str] = field(default_factory=list)
    negatives: List[str] = field(default_factory=list)
    missing_information: List[str] = field(default_factory=list)
    round_summaries: List[InterviewRoundSummary] = field(default_factory=list)


@dataclass
class ParseAgentContext:
    request: ResumeSourceRequest
    source: Optional[ResumeSource] = None
    blocks: List[ResumeBlock] = field(default_factory=list)
    sections: Dict[str, str] = field(default_factory=dict)
    issues: List[ParseIssue] = field(default_factory=list)
    fields: Dict[str, ParseFieldEvidence] = field(default_factory=dict)
    highlights: List[str] = field(default_factory=list)
    extraction_mode: str = "UNKNOWN"
    ocr_required: bool = False
    candidate_info: Optional[CandidateInfo] = None
    parse_report: Optional[ParseReport] = None
    step_logs: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class DecisionAgentContext:
    request: ResumeDecisionReportRequest
    candidate_info: Optional[CandidateInfo] = None
    parse_report: Optional[ParseReport] = None
    feedbacks: List[InterviewFeedback] = field(default_factory=list)
    feedback_aggregate: Optional[FeedbackAggregate] = None
    decision_report: Optional[DecisionReport] = None
    step_logs: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
