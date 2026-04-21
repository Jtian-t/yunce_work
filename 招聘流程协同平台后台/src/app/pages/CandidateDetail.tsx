import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import {
  ArrowLeft,
  Calendar,
  Download,
  FileSearch,
  Mail,
  MapPin,
  Phone,
  User,
} from "lucide-react";
import {
  useData,
  type CandidateDetail as CandidateDetailType,
  type CandidateFeedback,
  type InterviewPlan,
  type LookupDepartment,
  type LookupUser,
  type TimelineEvent,
} from "../context/DataContext";

function toLocalInputValue(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function CandidateDetail() {
  const { id } = useParams();
  const {
    loadCandidateDetail,
    loadCandidateTimeline,
    loadCandidateFeedbacks,
    loadCandidateInterviews,
    downloadResume,
    previewResume,
    loadDepartments,
    loadUsersByRole,
    advanceCandidate,
  } = useData();
  const [candidate, setCandidate] = useState<CandidateDetailType | null>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [feedbacks, setFeedbacks] = useState<CandidateFeedback[]>([]);
  const [interviews, setInterviews] = useState<InterviewPlan[]>([]);
  const [departments, setDepartments] = useState<LookupDepartment[]>([]);
  const [departmentLeads, setDepartmentLeads] = useState<LookupUser[]>([]);
  const [interviewers, setInterviewers] = useState<LookupUser[]>([]);
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [reviewerId, setReviewerId] = useState<number | "">("");
  const [interviewerId, setInterviewerId] = useState<number | "">("");
  const [roundLabel, setRoundLabel] = useState("一面");
  const [scheduledAt, setScheduledAt] = useState(toLocalInputValue(new Date(Date.now() + 24 * 60 * 60 * 1000)));
  const [endsAt, setEndsAt] = useState(toLocalInputValue(new Date(Date.now() + 25 * 60 * 60 * 1000)));
  const [actionNote, setActionNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!id) {
      setError("缺少候选人 ID");
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    Promise.all([
      loadCandidateDetail(Number(id)),
      loadCandidateTimeline(Number(id)),
      loadCandidateFeedbacks(Number(id)),
      loadCandidateInterviews(Number(id)),
      loadDepartments(),
      loadUsersByRole("DEPARTMENT_LEAD"),
      loadUsersByRole("INTERVIEWER"),
    ])
      .then(([detail, timelineData, feedbackData, interviewData, departmentOptions, leadOptions, interviewerOptions]) => {
        if (!active) {
          return;
        }
        setCandidate(detail);
        setTimeline(timelineData);
        setFeedbacks(feedbackData);
        setInterviews(interviewData);
        setDepartments(departmentOptions);
        setDepartmentLeads(leadOptions);
        setInterviewers(interviewerOptions);
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载候选人详情失败");
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
  }, [id, loadCandidateDetail, loadCandidateTimeline, loadCandidateFeedbacks, loadCandidateInterviews, loadDepartments, loadUsersByRole, refreshKey]);

  const filteredDepartmentLeads = useMemo(() => {
    if (!departmentId) {
      return departmentLeads;
    }
    return departmentLeads.filter((item) => item.departmentId === Number(departmentId));
  }, [departmentId, departmentLeads]);

  async function runAction(payload: Parameters<typeof advanceCandidate>[1]) {
    if (!candidate) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await advanceCandidate(candidate.id, payload);
      setActionNote("");
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "推进候选人失败");
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return <div className="p-6 text-gray-600">正在加载候选人详情...</div>;
  }

  if (!candidate) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error ?? "未找到候选人"}
        </div>
      </div>
    );
  }

  const skillList = candidate.skillsSummary
    ? candidate.skillsSummary.split(/[,，]/).map((item) => item.trim()).filter(Boolean)
    : [];
  const projectList = candidate.projectSummary
    ? candidate.projectSummary.split(/\n|；|;/).map((item) => item.trim()).filter(Boolean)
    : [];

  return (
    <div className="p-6 space-y-6">
      <Link to="/candidates" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900">
        <ArrowLeft className="h-5 w-5" />
        返回候选人列表
      </Link>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[0.95fr,1.05fr]">
        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="mb-6 flex flex-col items-center text-center">
              <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-blue-100 text-2xl font-semibold text-blue-700">
                {candidate.name[0]}
              </div>
              <h2 className="text-xl font-bold text-gray-900">{candidate.name}</h2>
              <p className="mt-1 text-gray-600">{candidate.position}</p>
              <span className="mt-3 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">{candidate.status}</span>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3 text-gray-700">
                <Phone className="h-4 w-4 text-gray-400" />
                <span>{candidate.phone ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <Mail className="h-4 w-4 text-gray-400" />
                <span>{candidate.email ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <MapPin className="h-4 w-4 text-gray-400" />
                <span>{candidate.location ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <Calendar className="h-4 w-4 text-gray-400" />
                <span>推荐日期：{candidate.submittedDate}</span>
              </div>
            </div>

            <div className="mt-6 border-t border-gray-200 pt-4">
              <div className="mb-3 text-sm font-medium text-gray-700">简历附件</div>
              {candidate.latestResume ? (
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => void previewResume(candidate.id)}
                    className="inline-flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100"
                  >
                    <FileSearch className="h-4 w-4" />
                    查看简历
                  </button>
                  <button
                    type="button"
                    onClick={() => void downloadResume(candidate.id, candidate.latestResume?.originalFileName ?? `${candidate.name}-resume.pdf`)}
                    className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                  >
                    <Download className="h-4 w-4" />
                    下载简历
                  </button>
                </div>
              ) : (
                <div className="text-sm text-gray-500">暂无简历附件</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">技能与项目</h3>
            <div className="mt-4">
              <div className="mb-2 text-sm font-medium text-gray-700">技能标签</div>
              <div className="flex flex-wrap gap-2">
                {skillList.length > 0 ? (
                  skillList.map((skill) => (
                    <span key={skill} className="rounded-lg bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700">
                      {skill}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-gray-500">暂无技能摘要</span>
                )}
              </div>
            </div>
            <div className="mt-5">
              <div className="mb-2 text-sm font-medium text-gray-700">项目经历</div>
              <div className="space-y-2 text-sm text-gray-700">
                {projectList.length > 0 ? projectList.map((project) => <div key={project}>• {project}</div>) : <div className="text-gray-500">暂无项目摘要</div>}
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">HR 推进一步</h3>
            <p className="mt-1 text-sm text-gray-500">根据当前状态展示合法动作，所有流转都会写入时间线。</p>

            <div className="mt-5 space-y-5">
              {(candidate.statusCode === "NEW" || candidate.statusCode === "TIMEOUT" || candidate.statusCode === "IN_DEPT_REVIEW") && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">分发或退回简历池</div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <select
                      value={departmentId}
                      onChange={(event) => setDepartmentId(event.target.value ? Number(event.target.value) : "")}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="">选择部门</option>
                      {departments.map((department) => (
                        <option key={department.id} value={department.id}>
                          {department.name}
                        </option>
                      ))}
                    </select>
                    <select
                      value={reviewerId}
                      onChange={(event) => setReviewerId(event.target.value ? Number(event.target.value) : "")}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="">选择负责人</option>
                      {filteredDepartmentLeads.map((reviewer) => (
                        <option key={reviewer.id} value={reviewer.id}>
                          {reviewer.displayName}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-3">
                    <button
                      type="button"
                      disabled={actionLoading || !departmentId || !reviewerId}
                      onClick={() =>
                        void runAction({
                          action: "ASSIGN_TO_DEPARTMENT",
                          departmentId: Number(departmentId),
                          reviewerId: Number(reviewerId),
                        })
                      }
                      className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      分发到部门
                    </button>
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MOVE_TO_POOL", note: actionNote })}
                      className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      退回简历池
                    </button>
                    {candidate.statusCode === "IN_DEPT_REVIEW" || candidate.statusCode === "TIMEOUT" ? (
                      <button
                        type="button"
                        disabled={actionLoading}
                        onClick={() => void runAction({ action: "REMIND_REVIEWER" })}
                        className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-medium text-amber-700 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        催办负责人
                      </button>
                    ) : null}
                  </div>
                </div>
              )}

              {(candidate.statusCode === "PENDING_INTERVIEW" || candidate.statusCode === "INTERVIEWING") && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">安排面试</div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <select
                      value={interviewerId}
                      onChange={(event) => setInterviewerId(event.target.value ? Number(event.target.value) : "")}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="">选择面试官</option>
                      {interviewers.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.displayName}
                        </option>
                      ))}
                    </select>
                    <input
                      value={roundLabel}
                      onChange={(event) => setRoundLabel(event.target.value)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                      placeholder="一面 / 二面 / HR 面"
                    />
                    <input
                      type="datetime-local"
                      value={scheduledAt}
                      onChange={(event) => setScheduledAt(event.target.value)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    />
                    <input
                      type="datetime-local"
                      value={endsAt}
                      onChange={(event) => setEndsAt(event.target.value)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    />
                  </div>
                  <button
                    type="button"
                    disabled={actionLoading || !interviewerId}
                    onClick={() =>
                      void runAction({
                        action: "SCHEDULE_INTERVIEW",
                        interviewerId: Number(interviewerId),
                        roundLabel,
                        scheduledAt: new Date(scheduledAt).toISOString(),
                        endsAt: new Date(endsAt).toISOString(),
                      })
                    }
                    className="mt-3 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    安排面试
                  </button>
                </div>
              )}

              {candidate.statusCode === "INTERVIEW_PASSED" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">Offer 阶段</div>
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={() => void runAction({ action: "ADVANCE_TO_OFFER_PENDING" })}
                    className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    推进到待发 Offer
                  </button>
                </div>
              )}

              {candidate.statusCode === "OFFER_PENDING" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">发放 Offer</div>
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={() => void runAction({ action: "MARK_OFFER_SENT" })}
                    className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    标记已发 Offer
                  </button>
                </div>
              )}

              {candidate.statusCode === "OFFER_SENT" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">Offer 结果</div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_HIRED" })}
                      className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      标记录用
                    </button>
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_REJECTED", note: actionNote })}
                      className="rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      标记淘汰
                    </button>
                  </div>
                </div>
              )}

              {!["HIRED", "REJECTED"].includes(candidate.statusCode) && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-2 font-medium text-gray-900">人工淘汰备注</div>
                  <textarea
                    value={actionNote}
                    onChange={(event) => setActionNote(event.target.value)}
                    className="min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                    placeholder="填写淘汰原因、退回原因或其他说明"
                  />
                  {candidate.statusCode !== "OFFER_SENT" && (
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_REJECTED", note: actionNote })}
                      className="mt-3 rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      人工淘汰
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">流程时间线</h3>
            <div className="mt-5 space-y-5">
              {timeline.length > 0 ? (
                timeline.map((item) => (
                  <div key={item.id} className="flex gap-4">
                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700">
                      {item.statusLabel[0]}
                    </div>
                    <div className="flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <div className="font-semibold text-gray-900">{item.sourceAction}</div>
                          <div className="text-sm text-gray-500">操作人：{item.actorName}</div>
                        </div>
                        <div className="text-sm text-gray-500">{new Date(item.occurredAt).toLocaleString("zh-CN")}</div>
                      </div>
                      <div className="mt-2 text-sm text-gray-700">{item.note || item.statusLabel}</div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-500">暂无流程事件</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">部门反馈记录</h3>
            <div className="mt-4 space-y-4">
              {feedbacks.length > 0 ? (
                feedbacks.map((feedback) => (
                  <div key={feedback.id} className="rounded-lg border border-gray-200 p-4">
                    <div className="mb-3 flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100">
                          <User className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{feedback.reviewer}</div>
                          <div className="text-sm text-gray-500">{new Date(feedback.createdAt).toLocaleString("zh-CN")}</div>
                        </div>
                      </div>
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-medium ${
                          feedback.decision === "PASS" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                        }`}
                      >
                        {feedback.decision === "PASS" ? "通过" : "不通过"}
                      </span>
                    </div>
                    <p className="text-sm text-gray-700">{feedback.feedback}</p>
                    {feedback.nextStep && <div className="mt-2 text-sm font-medium text-blue-600">下一步：{feedback.nextStep}</div>}
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-500">暂无部门反馈</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">面试记录</h3>
            <div className="mt-4 space-y-4">
              {interviews.length > 0 ? (
                interviews.map((interview) => (
                  <div key={interview.id} className="rounded-lg border border-gray-200 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <div className="font-semibold text-gray-900">{interview.roundLabel}</div>
                        <div className="mt-1 text-sm text-gray-600">
                          面试官：{interview.interviewer} · {new Date(interview.scheduledAt).toLocaleString("zh-CN")}
                        </div>
                      </div>
                      <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">{interview.status}</span>
                    </div>
                    {interview.evaluations.length > 0 ? (
                      <div className="mt-4 space-y-3 border-t border-gray-100 pt-4">
                        {interview.evaluations.map((evaluation) => (
                          <div key={evaluation.id} className="space-y-1 text-sm text-gray-700">
                            <div className="font-medium text-gray-900">
                              {evaluation.interviewer} · 评分 {evaluation.score}
                            </div>
                            <div>{evaluation.evaluation}</div>
                            {evaluation.suggestion && <div className="text-blue-600">建议：{evaluation.suggestion}</div>}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="mt-3 text-sm text-gray-500">当前还没有面试评价</div>
                    )}
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-500">暂无面试记录</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
