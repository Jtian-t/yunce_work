
from src.schemas import CandidateInfo
from src.llm_client import llm_client


RESUME_PARSER_SYSTEM_PROMPT = """你是一个专业的简历解析助手。请从以下简历文本中提取候选人信息，严格按照JSON格式输出。

注意事项：
1. 若没有工作经验，work_experience 返回空数组 []
2. 若没有项目经验，projects 返回空数组 []
3. 若某个字段缺失，留空字符串 "" 或空数组 []
4. 不要编造信息，只提取简历中明确提到的内容
5. 技能列表(skills)尽可能完整提取，包括技术栈、工具、语言等

JSON Schema:
{
  "name": "姓名",
  "phone": "电话",
  "email": "邮箱",
  "education": [
    {"school": "学校", "major": "专业", "degree": "学历", "duration": "时间段"}
  ],
  "work_experience": [
    {"company": "公司", "position": "职位", "duration": "时间", "description": "职责描述"}
  ],
  "projects": [
    {"name": "项目名", "role": "角色", "description": "描述", "tech_stack": ["技术栈"]}
  ],
  "skills": ["技能1", "技能2"],
  "summary": "个人总结或自我介绍"
}
"""


class ResumeParserAgent:
    @staticmethod
    def parse(resume_text: str) -> CandidateInfo:
        """
        解析简历文本，返回结构化信息

        Args:
            resume_text: 简历文本内容

        Returns:
            CandidateInfo 对象
        """
        user_prompt = f"请解析以下简历：\n\n{resume_text}"

        return llm_client.chat_with_structured_output(
            system_prompt=RESUME_PARSER_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            output_model=CandidateInfo,
            max_retries=3
        )

