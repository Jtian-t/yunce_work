from src.llm_client import llm_client
from src.schemas import CandidateInfo


RESUME_PARSER_SYSTEM_PROMPT = """
你是一名专业的中文简历解析助手。
请从输入的简历文本中提取候选人的结构化信息，并严格输出符合 CandidateInfo Schema 的 JSON。

要求：
1. 只提取简历中明确出现的信息，不要编造。
2. 缺失字段返回空字符串或空数组。
3. education、work_experience、projects 必须返回数组。
4. skills 尽量完整提取，包括编程语言、框架、中间件、数据库、云平台、工具和业务技能。
5. summary 用 1 到 3 句话总结候选人的背景、方向和主要优势。
6. 输出必须是纯 JSON，不要附带任何解释文本。
"""


class ResumeParserAgent:
    @staticmethod
    def parse(resume_text: str) -> CandidateInfo:
        user_prompt = f"""
请解析下面的简历文本，并输出 CandidateInfo 结构化 JSON：

{resume_text}
"""
        return llm_client.chat_with_structured_output(
            system_prompt=RESUME_PARSER_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            output_model=CandidateInfo,
            max_retries=3,
        )
