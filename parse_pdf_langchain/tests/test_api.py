from fastapi.testclient import TestClient

from src.main import app
from src.schemas import DecisionReport, ParseReport, ParseFieldEvidence
from src.services.resume_service import ResumeService

client = TestClient(app)


def test_parse_report_endpoint(monkeypatch):
    fake_report = ParseReport(
        summary="已完成提取",
        extracted_skills=["Java"],
        fields={"name": ParseFieldEvidence(value="张三")},
    )

    monkeypatch.setattr(ResumeService, "parse_resume_report", classmethod(lambda cls, request, **kwargs: fake_report))

    response = client.post("/api/resume/parse-report", json={"resume_text": "张三 Java"})
    assert response.status_code == 200
    assert response.json()["summary"] == "已完成提取"
    assert response.json()["fields"]["name"]["value"] == "张三"


def test_decision_report_endpoint(monkeypatch):
    fake_report = DecisionReport(
        conclusion="建议保守推进",
        recommendation_score=68,
        recommendation_level="保守推进",
        recommended_action="安排补充技术面",
        strengths=["技术栈匹配"],
        risks=["项目主导信息不足"],
        missing_information=["缺少系统设计深度证据"],
        supporting_evidence=["简历显示具备 Java 后端经验"],
        optimization_suggestions=["补问系统设计案例"],
        reasoning_summary="综合简历和反馈后，建议保守推进。",
    )

    monkeypatch.setattr(ResumeService, "generate_decision_report", classmethod(lambda cls, request: fake_report))

    response = client.post(
        "/api/resume/decision-report",
        json={
            "job_requirements": "Java 后端工程师",
            "interview_feedbacks": [],
            "parse_report": {
                "summary": "done",
                "highlights": [],
                "extractedSkills": ["Java"],
                "projectExperiences": [],
                "skills": [],
                "projects": [],
                "experiences": [],
                "educations": [],
                "rawBlocks": [],
                "fields": {
                    "name": {"value": "张三", "confidence": 0.9, "source": "llm"}
                },
                "issues": [],
                "extractionMode": "LANGCHAIN_STRUCTURED",
                "ocrRequired": False
            }
        },
    )
    assert response.status_code == 200
    assert response.json()["conclusion"] == "建议保守推进"
    assert response.json()["optimizationSuggestions"] == ["补问系统设计案例"]
