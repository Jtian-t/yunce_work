"""
Direct in-process validation for the PDF parsing agent flow.

This script avoids FastAPI startup and validates the agent runtime
against the local PDF fixture.
"""

import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from src.schemas import ResumeParseReportRequest  # noqa: E402
from src.services.resume_service import ResumeService  # noqa: E402


def test_parse_local_pdf():
    pdf_path = PROJECT_ROOT / "金天祥java后端开发简历.pdf"
    report = ResumeService.parse_resume_report(
        ResumeParseReportRequest(
            resume_file_path=str(pdf_path),
            resume_file_name=pdf_path.name,
        )
    )
    print("=== Parse Report ===")
    print(report.model_dump_json(indent=2, by_alias=True))

    assert report.summary
    assert report.fields
    assert report.fields["name"].value == "金天祥"
    assert len(report.educations) >= 2
    assert len(report.raw_blocks) >= 10


if __name__ == "__main__":
    test_parse_local_pdf()
