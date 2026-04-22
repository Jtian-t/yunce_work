import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import { ArrowLeft, CalendarClock, ExternalLink, FileText, MapPin, UserRound, ClipboardCheck } from "lucide-react";
import { useData, type CandidateDetail, type InterviewPlan } from "../context/DataContext";

export function InterviewerInterviewDetail() {
  const { id } = useParams();
  const { loadInterviewPlan, loadCandidateDetail, submitInterviewEvaluation } = useData();
  const [interview, setInterview] = useState<InterviewPlan | null>(null);
  const [candidate, setCandidate] = useState<CandidateDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<"PASS" | "FAIL">("PASS");
  const [score, setScore] = useState("85");
  const [evaluation, setEvaluation] = useState("");
  const [strengths, setStrengths] = useState("");
  const [weaknesses, setWeaknesses] = useState("");
  const [suggestion, setSuggestion] = useState("");

  useEffect(() => {
    if (!id) {
      setError("缺少面试 ID");
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    loadInterviewPlan(Number(id))
      .then(async (plan) => {
        if (!active) {
          return;
        }
        setInterview(plan);
        const detail = await loadCandidateDetail(plan.candidateId);
        if (!active) {
          return;
        }
        setCandidate(detail);
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载面试详情失败");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [id, loadCandidateDetail, loadInterviewPlan]);

  const latestEvaluation = useMemo(() => interview?.evaluations?.[0] ?? null, [interview]);
  const readOnly = Boolean(interview?.evaluationSubmitted);

  async function handleSubmitEvaluation() {
    if (!interview) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await submitInterviewEvaluation(interview.id, {
        result,
        score: Number(score),
        evaluation,
        strengths: strengths || undefined,
        weaknesses: weaknesses || undefined,
        suggestion: suggestion || undefined,
      });
      const refreshed = await loadInterviewPlan(interview.id);
      setInterview(refreshed);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "提交面试评价失败");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="p-6 text-gray-600">正在加载面试详情...</div>;
  }

  if (!interview || !candidate) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error ?? "未找到面试详情"}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      <Link to="/interviews/mine" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900">
        <ArrowLeft className="h-5 w-5" />
        返回我的面试
      </Link>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid gap-6 xl:grid-cols-[0.95fr,1.05fr]">
        <div className="space-y-6">
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-2xl font-semibold text-gray-900">{candidate.name}</h2>
                <p className="mt-1 text-sm text-gray-500">{candidate.position}</p>
              </div>
              <span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">
                {interview.interviewStageLabel ?? interview.roundLabel}
              </span>
            </div>

            <div className="mt-6 grid gap-3 text-sm text-gray-700">
              <div className="flex items-center gap-3">
                <CalendarClock className="h-4 w-4 text-gray-400" />
                <span>{new Date(interview.scheduledAt).toLocaleString("zh-CN")}</span>
              </div>
              <div className="flex items-center gap-3">
                <UserRound className="h-4 w-4 text-gray-400" />
                <span>面试官：{interview.interviewer}</span>
              </div>
              {candidate.location && (
                <div className="flex items-center gap-3">
                  <MapPin className="h-4 w-4 text-gray-400" />
                  <span>{candidate.location}</span>
                </div>
              )}
              <div className="flex items-center gap-3">
                <FileText className="h-4 w-4 text-gray-400" />
                <span>{candidate.experience ?? "经验待补充"} · {candidate.education ?? "学历待补充"}</span>
              </div>
            </div>

            {interview.meetingUrl && (
              <a
                href={interview.meetingUrl}
                target="_blank"
                rel="noreferrer"
                className="mt-5 inline-flex items-center gap-2 rounded-lg border border-blue-300 bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100"
              >
                <ExternalLink className="h-4 w-4" />
                进入会议
              </a>
            )}

            <div className="mt-6 rounded-xl border border-gray-200 bg-gray-50 p-4">
              <div className="text-sm font-medium text-gray-900">候选人摘要</div>
              <div className="mt-2 text-sm leading-6 text-gray-700">
                {candidate.projectSummary || candidate.skillsSummary || "当前暂无更多候选人摘要。"}
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <ClipboardCheck className="h-5 w-5 text-emerald-600" />
              面试评价
            </div>

            {readOnly ? (
              <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
                这场面试已经提交过评价，HR 侧会根据该结果继续推进下一轮或转入人才库。
              </div>
            ) : (
              <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
                面试官只能查看分配给自己的安排并提交评价，不能修改面试时间、链接或面试官。
              </div>
            )}

            <div className="mt-5 space-y-4">
              <div className="grid gap-3 md:grid-cols-2">
                <select
                  value={result}
                  onChange={(event) => setResult(event.target.value as "PASS" | "FAIL")}
                  disabled={readOnly}
                  className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                >
                  <option value="PASS">通过</option>
                  <option value="FAIL">不通过</option>
                </select>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={score}
                  onChange={(event) => setScore(event.target.value)}
                  disabled={readOnly}
                  className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                  placeholder="评分（0-100）"
                />
              </div>

              <textarea
                value={evaluation}
                onChange={(event) => setEvaluation(event.target.value)}
                disabled={readOnly}
                className="min-h-28 w-full rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                placeholder="请填写面试评价，HR 会依据该评价决定推进下一轮还是转入人才库。"
              />
              <textarea
                value={strengths}
                onChange={(event) => setStrengths(event.target.value)}
                disabled={readOnly}
                className="min-h-20 w-full rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                placeholder="优势亮点"
              />
              <textarea
                value={weaknesses}
                onChange={(event) => setWeaknesses(event.target.value)}
                disabled={readOnly}
                className="min-h-20 w-full rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                placeholder="风险点 / 不足"
              />
              <textarea
                value={suggestion}
                onChange={(event) => setSuggestion(event.target.value)}
                disabled={readOnly}
                className="min-h-20 w-full rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                placeholder="给 HR 的建议，例如建议下一轮重点追问什么，或建议淘汰。"
              />

              {!readOnly && (
                <button
                  type="button"
                  disabled={saving || !evaluation.trim()}
                  onClick={() => void handleSubmitEvaluation()}
                  className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {saving ? "提交中..." : "提交面试评价"}
                </button>
              )}
            </div>
          </div>

          {latestEvaluation && (
            <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
              <div className="text-lg font-semibold text-gray-900">已提交评价</div>
              <div className="mt-3 text-sm text-gray-700">结果：{latestEvaluation.result} · 评分：{latestEvaluation.score}</div>
              <div className="mt-2 text-sm text-gray-700">{latestEvaluation.evaluation}</div>
              {latestEvaluation.suggestion && <div className="mt-2 text-sm text-blue-700">建议：{latestEvaluation.suggestion}</div>}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
