import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { ArrowLeft, CheckCircle, Download, FileSearch, Loader2, XCircle } from "lucide-react";
import { useData, type CandidateDetail, type LookupUser } from "../context/DataContext";

export function DepartmentFeedback() {
  const { id } = useParams();
  const navigate = useNavigate();
  const {
    loadCandidateDetail,
    loadUsersByRole,
    submitDepartmentFeedback,
    getDepartmentTaskByCandidateId,
    downloadResume,
    previewResume,
  } = useData();

  const [candidate, setCandidate] = useState<CandidateDetail | null>(null);
  const [pageLoading, setPageLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [passed, setPassed] = useState<boolean | null>(null);
  const [scheduleInterview, setScheduleInterview] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [interviewers, setInterviewers] = useState<LookupUser[]>([]);
  const [suggestedInterviewerId, setSuggestedInterviewerId] = useState<number | "">("");

  const candidateId = Number(id);
  const task = Number.isNaN(candidateId) ? undefined : getDepartmentTaskByCandidateId(candidateId);
  const suggestedInterviewer = interviewers.find((item) => item.id === Number(suggestedInterviewerId));

  useEffect(() => {
    if (!id || Number.isNaN(candidateId)) {
      setPageError("候选人 ID 无效");
      setPageLoading(false);
      return;
    }

    let active = true;
    setPageLoading(true);
    setPageError(null);

    loadCandidateDetail(candidateId)
      .then((detail) => {
        if (active) {
          setCandidate(detail);
        }
      })
      .catch((requestError) => {
        if (active) {
          setPageError(requestError instanceof Error ? requestError.message : "加载候选人详情失败");
        }
      })
      .finally(() => {
        if (active) {
          setPageLoading(false);
        }
      });

    loadUsersByRole("INTERVIEWER")
      .then((items) => {
        if (active) {
          setInterviewers(items);
        }
      })
      .catch(() => {
        if (active) {
          setInterviewers([]);
        }
      });

    return () => {
      active = false;
    };
  }, [id, candidateId, loadCandidateDetail, loadUsersByRole]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (passed === null || !candidate) {
      return;
    }
    if (!passed && !rejectReason) {
      setPageError("请选择不通过原因");
      return;
    }

    setSubmitting(true);
    setPageError(null);
    try {
      await submitDepartmentFeedback({
        candidateId: candidate.id,
        passed,
        feedback,
        rejectReason: passed ? undefined : rejectReason,
        nextStep: passed ? (scheduleInterview ? "安排一面" : "待安排面试") : "-",
        suggestedInterviewer: passed && scheduleInterview ? suggestedInterviewer?.displayName : undefined,
        suggestedInterviewerId: passed && scheduleInterview && suggestedInterviewer ? suggestedInterviewer.id : undefined,
        suggestedInterviewerName: passed && scheduleInterview ? suggestedInterviewer?.displayName : undefined,
      });
      navigate("/dept");
    } catch (requestError) {
      setPageError(requestError instanceof Error ? requestError.message : "提交反馈失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (pageLoading) {
    return (
      <div className="min-h-[50vh] p-6 flex items-center justify-center text-gray-600">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        正在加载候选人信息...
      </div>
    );
  }

  if (!candidate) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {pageError ?? "未找到候选人信息"}
        </div>
      </div>
    );
  }

  const skills = candidate.skillsSummary
    ? candidate.skillsSummary.split(/[,，、]/).map((item) => item.trim()).filter(Boolean)
    : [];
  const projects = candidate.projectSummary
    ? candidate.projectSummary.split(/\n|；|;/).map((item) => item.trim()).filter(Boolean)
    : [];

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <Link to="/dept" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900">
        <ArrowLeft className="h-5 w-5" />
        返回待办列表
      </Link>

      {pageError && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{pageError}</div>}

      <div className="rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 p-8 text-white shadow-lg">
        <h1 className="text-2xl font-bold">候选人简历筛选</h1>
        <p className="mt-2 text-sm text-blue-100">
          当前任务：{task ? `${task.department} / ${task.reviewer}` : "未加载到待办信息"}，请结合候选人信息提交部门反馈。
        </p>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="flex h-16 w-16 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 text-2xl font-semibold text-blue-700">
              {candidate.name[0]}
            </div>
            <div>
              <h2 className="text-2xl font-bold text-gray-900">{candidate.name}</h2>
              <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-600">
                <span>岗位：<span className="font-medium text-gray-900">{candidate.position}</span></span>
                <span>经验：<span className="font-medium text-gray-900">{candidate.experience ?? "-"}</span></span>
                <span>学历：<span className="font-medium text-gray-900">{candidate.education ?? "-"}</span></span>
              </div>
              <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-600">
                <span>电话：{candidate.phone ?? "-"}</span>
                <span>邮箱：{candidate.email ?? "-"}</span>
                <span>来源：{candidate.source}</span>
              </div>
            </div>
          </div>

          {candidate.latestResume ? (
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() =>
                  void previewResume(candidate.id).catch((requestError) => {
                    setPageError(requestError instanceof Error ? requestError.message : "简历预览失败");
                  })
                }
                className="inline-flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100"
              >
                <FileSearch className="h-4 w-4" />
                查看简历
              </button>
              <button
                type="button"
                onClick={() =>
                  void downloadResume(
                    candidate.id,
                    candidate.latestResume?.originalFileName ?? `${candidate.name}-resume.pdf`
                  )
                }
                className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                <Download className="h-4 w-4" />
                下载简历
              </button>
            </div>
          ) : (
            <div className="text-sm text-gray-500">暂无已上传简历</div>
          )}
        </div>

        <div className="mb-6">
          <div className="mb-3 text-sm font-semibold text-gray-700">技能标签</div>
          <div className="flex flex-wrap gap-2">
            {skills.length > 0 ? (
              skills.map((skill) => (
                <span key={skill} className="rounded-lg bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700">
                  {skill}
                </span>
              ))
            ) : (
              <span className="text-sm text-gray-500">暂无技能摘要</span>
            )}
          </div>
        </div>

        <div>
          <div className="mb-3 text-sm font-semibold text-gray-700">项目经历</div>
          <ul className="space-y-2">
            {projects.length > 0 ? (
              projects.map((project) => (
                <li key={project} className="text-sm text-gray-700">
                  • {project}
                </li>
              ))
            ) : (
              <li className="text-sm text-gray-500">暂无项目摘要</li>
            )}
          </ul>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h3 className="mb-6 text-lg font-semibold text-gray-900">反馈表单</h3>

        <div className="mb-6">
          <label className="mb-3 block text-sm font-medium text-gray-700">
            是否通过筛选 <span className="text-red-500">*</span>
          </label>
          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => setPassed(true)}
              className={`flex flex-1 items-center justify-center gap-2 rounded-lg border-2 px-6 py-4 transition-all ${
                passed === true
                  ? "border-green-500 bg-green-50 text-green-700"
                  : "border-gray-300 bg-white text-gray-700 hover:border-green-300"
              }`}
            >
              <CheckCircle className="h-5 w-5" />
              <span className="font-medium">通过筛选</span>
            </button>
            <button
              type="button"
              onClick={() => setPassed(false)}
              className={`flex flex-1 items-center justify-center gap-2 rounded-lg border-2 px-6 py-4 transition-all ${
                passed === false
                  ? "border-red-500 bg-red-50 text-red-700"
                  : "border-gray-300 bg-white text-gray-700 hover:border-red-300"
              }`}
            >
              <XCircle className="h-5 w-5" />
              <span className="font-medium">不通过</span>
            </button>
          </div>
        </div>

        {passed === true && (
          <div className="mb-6 space-y-4 rounded-lg border border-green-200 bg-green-50 p-4">
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="checkbox"
                checked={scheduleInterview}
                onChange={(event) => setScheduleInterview(event.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-blue-600"
              />
              <span className="text-sm font-medium text-gray-900">建议安排面试</span>
            </label>

            {scheduleInterview && (
              <label className="block text-sm text-gray-700">
                推荐面试官
                <select
                  value={suggestedInterviewerId}
                  onChange={(event) => setSuggestedInterviewerId(event.target.value ? Number(event.target.value) : "")}
                  className="mt-1 w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5"
                >
                  <option value="">选择面试官</option>
                  {interviewers.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.displayName}
                      {item.departmentName ? ` - ${item.departmentName}` : ""}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>
        )}

        <div className="mb-6">
          <label className="mb-2 block text-sm font-medium text-gray-700">
            反馈意见 <span className="text-red-500">*</span>
          </label>
          <textarea
            value={feedback}
            onChange={(event) => setFeedback(event.target.value)}
            rows={5}
            placeholder="请结合岗位要求，说明候选人的匹配点、风险点和建议动作"
            className="w-full resize-none rounded-lg border border-gray-300 px-4 py-3"
            required
          />
        </div>

        {passed === false && (
          <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4">
            <label className="mb-2 block text-sm font-medium text-gray-700">
              不通过原因 <span className="text-red-500">*</span>
            </label>
            <div className="space-y-2">
              {[
                "工作经验不符合要求",
                "技能不匹配",
                "学历要求不符",
                "薪资期望过高",
                "稳定性存在风险",
                "其他原因",
              ].map((reason) => (
                <label key={reason} className="flex cursor-pointer items-center gap-2">
                  <input
                    type="radio"
                    name="rejectReason"
                    value={reason}
                    checked={rejectReason === reason}
                    onChange={(event) => setRejectReason(event.target.value)}
                    className="h-4 w-4 border-gray-300 text-blue-600"
                  />
                  <span className="text-sm text-gray-700">{reason}</span>
                </label>
              ))}
            </div>
          </div>
        )}

        <div className="flex gap-4 border-t border-gray-200 pt-4">
          <Link
            to="/dept"
            className="flex-1 rounded-lg border border-gray-300 px-6 py-3 text-center font-medium text-gray-700 hover:bg-gray-50"
          >
            取消
          </Link>
          <button
            type="submit"
            disabled={submitting || passed === null || !feedback || (passed === false && !rejectReason)}
            className="inline-flex flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            提交反馈
          </button>
        </div>
      </form>
    </div>
  );
}
