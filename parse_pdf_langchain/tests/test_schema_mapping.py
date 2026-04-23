from src.schemas import (
    ParseFieldEvidence,
    ParseProjectSummary,
    ParseReport,
    ResumeProfile,
    build_parse_report,
    profile_from_parse_report,
)


def test_build_parse_report_keeps_java_compatible_fields():
    profile = ResumeProfile(
        name="张三",
        target_position="Java后端工程师",
        phone="13800138000",
        email="candidate@example.com",
        education="本科，计算机科学与技术",
        experience_years="3年",
        location="上海",
        source="简历上传",
        skills_summary="熟悉 Java、Spring Boot、MySQL、Redis。",
        project_summary="参与电商后台开发，负责接口实现与缓存优化。",
        skill_keywords=["Java", "Spring Boot", "MySQL", "Redis"],
        project_highlights=["参与电商后台开发，负责接口实现与缓存优化。"],
    )

    report = build_parse_report(profile, "resume text", "resume.pdf")

    assert report.fields["name"].value == "张三"
    assert report.fields["skillsSummary"].value.startswith("熟悉 Java")
    assert report.fields["projectSummary"].value.startswith("参与电商后台")
    assert report.fields["experience"].value == "3年"
    assert report.extracted_skills == ["Java", "Spring Boot", "MySQL", "Redis"]


def test_profile_can_be_resolved_from_parse_report():
    report = ParseReport(
        summary="done",
        extracted_skills=["Java", "Redis"],
        project_experiences=[ParseProjectSummary(title="项目经历", summary="负责订单系统开发")],
        fields={
            "name": ParseFieldEvidence(value="李四"),
            "experience": ParseFieldEvidence(value="5年"),
            "skillsSummary": ParseFieldEvidence(value="熟悉 Java、Redis"),
            "projectSummary": ParseFieldEvidence(value="负责订单系统开发"),
        },
    )

    profile = profile_from_parse_report(report)

    assert profile.name == "李四"
    assert profile.experience_years == "5年"
    assert profile.skill_keywords == ["Java", "Redis"]
