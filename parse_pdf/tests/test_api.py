
"""
API 测试脚本
使用前请先启动服务: uvicorn src.main:app --reload
"""
import httpx

BASE_URL = "http://localhost:8000"


def test_parse_resume():
    """测试简历解析接口"""
    with open("tests/sample_resume.txt", "r", encoding="utf-8") as f:
        resume_text = f.read()

    response = httpx.post(
        f"{BASE_URL}/api/resume/parse",
        json={"resume_text": resume_text}
    )
    print("=== 简历解析结果 ===")
    print(response.json())
    return response.json()


def test_analyze_candidate(candidate_info):
    """测试分析建议接口"""
    job_requirements = """
    岗位职责：
    1. 5年以上Java开发经验
    2. 熟悉Spring Boot、Redis、MySQL
    3. 有高并发系统经验优先
    4. 有电商平台经验优先
    """

    interview_feedbacks = [
        {
            "round": 1,
            "interviewer": "李面试官",
            "feedback": "候选人技术基础扎实，对Redis理解深入，但项目经验细节描述不够清晰",
            "score": 80,
            "pros": ["技术基础好", "沟通顺畅"],
            "cons": ["项目细节不够"]
        }
    ]

    response = httpx.post(
        f"{BASE_URL}/api/resume/analyze",
        json={
            "candidate_info": candidate_info,
            "job_requirements": job_requirements,
            "interview_feedbacks": interview_feedbacks
        }
    )
    print("\n=== 分析建议结果 ===")
    print(response.json())


if __name__ == "__main__":
    print("请先确保服务已启动，然后运行:")
    print("1. pip install -r requirements.txt")
    print("2. 复制 .env.example 为 .env 并配置 LLM API")
    print("3. uvicorn src.main:app --reload")
    print("4. 取消注释下面的代码进行测试\n")

    # candidate = test_parse_resume()
    # test_analyze_candidate(candidate)

