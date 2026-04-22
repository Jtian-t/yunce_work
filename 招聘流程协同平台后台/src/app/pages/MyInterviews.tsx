import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  CalendarClock,
  ChevronDown,
  ChevronRight,
  ClipboardCheck,
  ExternalLink,
  FileSearch,
  Users,
} from "lucide-react";
import { Link } from "react-router";
import { useData, type DepartmentMember, type InterviewPlan, type LookupDepartment } from "../context/DataContext";

type TabKey = "my" | "department";

export function MyInterviews() {
  const { currentUser, loadDepartments, loadDepartmentMembers, loadMyInterviews } = useData();
  const [tab, setTab] = useState<TabKey>("my");
  const [departments, setDepartments] = useState<LookupDepartment[]>([]);
  const [members, setMembers] = useState<DepartmentMember[]>([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | "">("");
  const [selectedUserId, setSelectedUserId] = useState<number | "">("");
  const [interviews, setInterviews] = useState<InterviewPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCompleted, setShowCompleted] = useState(false);

  const roles = currentUser?.roles ?? [];
  const canCoordination = roles.includes("HR") || roles.includes("DEPARTMENT_LEAD");
  const isLead = roles.includes("DEPARTMENT_LEAD") && !roles.includes("HR");

  useEffect(() => {
    let active = true;
    setError(null);

    Promise.all([
      loadMyInterviews({ scope: "my" }),
      canCoordination ? loadDepartments() : Promise.resolve([] as LookupDepartment[]),
    ])
      .then(([myInterviews, departmentOptions]) => {
        if (!active) {
          return;
        }
        setInterviews(myInterviews);
        setDepartments(departmentOptions);
        if (canCoordination) {
          if (isLead) {
            const ownDepartment = departmentOptions.find((item) => item.name === currentUser?.department);
            setSelectedDepartmentId(ownDepartment?.id ?? "");
          } else if (departmentOptions.length > 0) {
            setSelectedDepartmentId(departmentOptions[0].id);
          }
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载面试工作台失败");
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
  }, [canCoordination, currentUser?.department, isLead, loadDepartments, loadMyInterviews]);

  useEffect(() => {
    if (!canCoordination || !selectedDepartmentId) {
      setMembers([]);
      return;
    }

    let active = true;
    loadDepartmentMembers(Number(selectedDepartmentId))
      .then((items) => {
        if (active) {
          setMembers(items);
        }
      })
      .catch(() => {
        if (active) {
          setMembers([]);
        }
      });

    return () => {
      active = false;
    };
  }, [canCoordination, loadDepartmentMembers, selectedDepartmentId]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    const query =
      tab === "my"
        ? { scope: "my" as const }
        : {
            scope: "department" as const,
            departmentId: selectedDepartmentId ? Number(selectedDepartmentId) : undefined,
            userId: selectedUserId ? Number(selectedUserId) : undefined,
          };

    loadMyInterviews(query)
      .then((items) => {
        if (active) {
          setInterviews(items);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载面试工作台失败");
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
  }, [loadMyInterviews, selectedDepartmentId, selectedUserId, tab]);

  const roundStats = useMemo(() => {
    const stats = new Map<string, number>();
    interviews.forEach((item) => {
      const key = item.interviewStageLabel ?? item.roundLabel;
      stats.set(key, (stats.get(key) ?? 0) + 1);
    });
    return Array.from(stats.entries()).slice(0, 3);
  }, [interviews]);

  const pendingCount = interviews.filter((item) => !item.evaluationSubmitted).length;
  const pendingInterviews = useMemo(() => interviews.filter((item) => !item.evaluationSubmitted), [interviews]);
  const completedInterviews = useMemo(() => interviews.filter((item) => item.evaluationSubmitted), [interviews]);
  const selectedDepartmentName = departments.find((item) => item.id === Number(selectedDepartmentId))?.name ?? "全部部门";

  return (
    <div className="space-y-6 p-6">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-semibold text-gray-900">我的面试</h2>
            <p className="mt-1 text-sm text-gray-500">
              {tab === "my"
                ? "查看当前账号名下的面试任务和评价状态。"
                : `查看 ${selectedDepartmentName} 的面试协调情况，并按员工筛选。`}
            </p>
          </div>
          <div className="inline-flex rounded-xl border border-gray-200 bg-gray-50 p-1">
            <button
              type="button"
              onClick={() => setTab("my")}
              className={`rounded-lg px-4 py-2 text-sm font-medium ${tab === "my" ? "bg-white text-blue-700 shadow-sm" : "text-gray-600"}`}
            >
              我的
            </button>
            {canCoordination && (
              <button
                type="button"
                onClick={() => setTab("department")}
                className={`rounded-lg px-4 py-2 text-sm font-medium ${
                  tab === "department" ? "bg-white text-blue-700 shadow-sm" : "text-gray-600"
                }`}
              >
                部门协调
              </button>
            )}
          </div>
        </div>

        {tab === "department" && canCoordination && (
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            <select
              value={selectedDepartmentId}
              onChange={(event) => {
                setSelectedDepartmentId(event.target.value ? Number(event.target.value) : "");
                setSelectedUserId("");
              }}
              disabled={isLead}
              className="rounded-xl border border-gray-300 px-3 py-2.5"
            >
              {!isLead && <option value="">全部部门</option>}
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
            <select
              value={selectedUserId}
              onChange={(event) => setSelectedUserId(event.target.value ? Number(event.target.value) : "")}
              className="rounded-xl border border-gray-300 px-3 py-2.5"
            >
              <option value="">全部员工</option>
              {members.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.displayName}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <StatCard icon={<CalendarClock className="h-5 w-5 text-blue-600" />} label="面试总数" value={loading ? "--" : String(interviews.length)} />
        <StatCard icon={<ClipboardCheck className="h-5 w-5 text-amber-600" />} label="待评价" value={loading ? "--" : String(pendingCount)} />
        <StatCard icon={<Users className="h-5 w-5 text-emerald-600" />} label={roundStats[0]?.[0] ?? "轮次统计"} value={loading ? "--" : String(roundStats[0]?.[1] ?? 0)} />
        <StatCard icon={<Users className="h-5 w-5 text-violet-600" />} label={roundStats[1]?.[0] ?? "更多轮次"} value={loading ? "--" : String(roundStats[1]?.[1] ?? 0)} />
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">加载中...</div>
        ) : interviews.length === 0 ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">当前条件下暂无面试任务</div>
        ) : (
          <>
            {pendingInterviews.length > 0 && (
              <div className="space-y-4">
                {pendingInterviews.map((interview) => (
                  <InterviewCard key={interview.id} interview={interview} highlight />
                ))}
              </div>
            )}

            {completedInterviews.length > 0 && (
              <div className="rounded-2xl border border-gray-200 bg-white shadow-sm">
                <button
                  type="button"
                  onClick={() => setShowCompleted((current) => !current)}
                  className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left hover:bg-gray-50"
                >
                  <div>
                    <div className="text-sm font-semibold text-gray-900">已完成面试</div>
                    <div className="mt-1 text-sm text-gray-500">共 {completedInterviews.length} 场，点击展开查看</div>
                  </div>
                  {showCompleted ? (
                    <ChevronDown className="h-4 w-4 text-gray-400" />
                  ) : (
                    <ChevronRight className="h-4 w-4 text-gray-400" />
                  )}
                </button>
                {showCompleted && (
                  <div className="space-y-4 border-t border-gray-100 px-5 py-4">
                    {completedInterviews.map((interview) => (
                      <InterviewCard key={interview.id} interview={interview} />
                    ))}
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function InterviewCard({ interview, highlight = false }: { interview: InterviewPlan; highlight?: boolean }) {
  return (
    <div
      className={`rounded-2xl border bg-white p-5 shadow-sm ${
        highlight ? "border-amber-300 bg-amber-50/40 shadow-amber-100" : "border-gray-200"
      }`}
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className={`text-lg font-semibold ${highlight ? "text-amber-900" : "text-gray-900"}`}>
            {interview.candidateName ?? "候选人"}
          </div>
          <div className={`mt-1 text-sm ${highlight ? "font-semibold text-amber-800" : "text-gray-600"}`}>
            {interview.position ?? "-"} · {interview.interviewStageLabel ?? interview.roundLabel}
          </div>
          <div className={`mt-1 text-sm ${highlight ? "font-medium text-amber-700" : "text-gray-600"}`}>
            {new Date(interview.scheduledAt).toLocaleString("zh-CN")}
          </div>
          <div className={`mt-2 flex flex-wrap gap-3 text-xs ${highlight ? "font-semibold text-amber-700" : "text-gray-500"}`}>
            <span>面试官：{interview.interviewer}</span>
            {interview.departmentName && <span>部门：{interview.departmentName}</span>}
            <span>{interview.evaluationSubmitted ? "已提交评价" : "待提交评价"}</span>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {!interview.evaluationSubmitted && interview.meetingUrl && (
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
            to={`/interviews/${interview.id}`}
            className="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <FileSearch className="h-4 w-4" />
            面试详情
          </Link>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center gap-2">{icon}</div>
      <div className="text-sm text-gray-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-gray-900">{value}</div>
    </div>
  );
}
