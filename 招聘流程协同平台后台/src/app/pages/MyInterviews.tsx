import { useEffect, useState } from "react";
import { ExternalLink, FileSearch } from "lucide-react";
import { Link } from "react-router";
import { useData, type InterviewPlan } from "../context/DataContext";

export function MyInterviews() {
  const { loadMyInterviews } = useData();
  const [interviews, setInterviews] = useState<InterviewPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    loadMyInterviews()
      .then(setInterviews)
      .catch((requestError) => {
        setError(requestError instanceof Error ? requestError.message : "加载我的面试失败");
      })
      .finally(() => setLoading(false));
  }, [loadMyInterviews]);

  return (
    <div className="p-6 space-y-6">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-gray-900">我的面试</h2>
        <p className="mt-1 text-sm text-gray-500">查看分配给当前账号的面试任务与评价状态。</p>
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="text-sm text-gray-500">加载中...</div>
        ) : interviews.length === 0 ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">暂无面试任务</div>
        ) : (
          interviews.map((interview) => (
            <div key={interview.id} className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-lg font-semibold text-gray-900">{interview.candidateName ?? "候选人"}</div>
                  <div className="text-sm text-gray-600">{interview.position ?? "-"} · {interview.interviewStageLabel ?? interview.roundLabel}</div>
                  <div className="mt-1 text-sm text-gray-600">{new Date(interview.scheduledAt).toLocaleString("zh-CN")}</div>
                  <div className="mt-1 text-xs text-gray-500">{interview.evaluationSubmitted ? "已提交评价" : "待提交评价"}</div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {interview.meetingUrl && (
                    <a
                      href={interview.meetingUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 rounded-lg border border-blue-300 bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-100"
                    >
                      <ExternalLink className="h-4 w-4" />
                      进入会议
                    </a>
                  )}
                  <Link
                    to={`/candidates/${interview.candidateId}`}
                    className="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                  >
                    <FileSearch className="h-4 w-4" />
                    候选人详情
                  </Link>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
