"""
API smoke tests for the resume parsing sidecar.

The target base URL can be overridden with BASE_URL.
"""

import os
from pathlib import Path

import httpx


BASE_URL = os.getenv("BASE_URL", "http://127.0.0.1:8000")


def _print_response(response: httpx.Response):
    print(f"status_code={response.status_code}")
    print(f"content_type={response.headers.get('content-type', '')}")
    try:
        data = response.json()
        print(data)
        return data
    except ValueError:
        text = response.text.strip()
        print(text if text else "<empty response body>")
        return None


def test_parse_resume():
    resume_text = (Path(__file__).parent / "sample_resume.txt").read_text(encoding="utf-8")
    response = httpx.post(
        f"{BASE_URL}/api/resume/parse",
        json={"resume_text": resume_text},
        timeout=90.0,
    )
    print("=== Legacy Parse Result ===")
    data = _print_response(response)
    response.raise_for_status()
    return data


def test_parse_report_from_pdf():
    pdf_path = Path(__file__).resolve().parents[1] / "金天祥java后端开发简历.pdf"
    response = httpx.post(
        f"{BASE_URL}/api/resume/parse-report",
        json={"resume_file_path": str(pdf_path), "resume_file_name": pdf_path.name},
        timeout=180.0,
    )
    print("=== PDF Parse Report ===")
    data = _print_response(response)
    response.raise_for_status()
    return data


def test_decision_report(candidate_info):
    response = httpx.post(
        f"{BASE_URL}/api/resume/decision-report",
        json={
            "candidate_info": candidate_info,
            "job_requirements": (
                "Java 后端开发岗位，需要熟悉 Spring Boot、MySQL、Redis、Kafka，"
                "具备中大型系统设计与接口开发经验，有良好的问题定位能力。"
            ),
            "interview_feedbacks": [
                {
                    "round": 1,
                    "interviewer": "技术一面",
                    "feedback": "Java 基础比较扎实，接口设计能力不错，对常见中间件有实践经验。",
                    "score": 78,
                    "pros": ["Java 基础扎实", "接口设计思路清晰"],
                    "cons": ["高并发场景经验还需要继续确认"],
                },
                {
                    "round": 2,
                    "interviewer": "项目复盘面",
                    "feedback": "项目经历较完整，但系统容量评估和性能压测细节回答不够深入。",
                    "score": 72,
                    "pros": ["项目背景真实", "能说清主导模块"],
                    "cons": ["容量设计细节不足", "性能压测经验不足"],
                },
            ],
        },
        timeout=180.0,
    )
    print("=== Decision Report ===")
    data = _print_response(response)
    response.raise_for_status()
    return data


if __name__ == "__main__":
    print(f"Using base URL: {BASE_URL}")
    candidate = test_parse_resume()
    test_parse_report_from_pdf()
    test_decision_report(candidate)
