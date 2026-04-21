import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Bell, MailCheck, Users } from "lucide-react";
import { useData, type DepartmentMember, type LookupDepartment, type NotificationItem } from "../context/DataContext";

type TabKey = "my" | "department";

export function NotificationsPage() {
  const { currentUser, loadDepartments, loadDepartmentMembers, loadNotifications, markNotificationRead } = useData();
  const [tab, setTab] = useState<TabKey>("my");
  const [departments, setDepartments] = useState<LookupDepartment[]>([]);
  const [members, setMembers] = useState<DepartmentMember[]>([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | "">("");
  const [selectedUserId, setSelectedUserId] = useState<number | "">("");
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const roles = currentUser?.roles ?? [];
  const canCoordination = roles.includes("HR") || roles.includes("DEPARTMENT_LEAD");
  const isLead = roles.includes("DEPARTMENT_LEAD") && !roles.includes("HR");

  useEffect(() => {
    let active = true;
    setError(null);

    Promise.all([
      loadNotifications({ scope: "my" }),
      canCoordination ? loadDepartments() : Promise.resolve([] as LookupDepartment[]),
    ])
      .then(([notificationItems, departmentOptions]) => {
        if (!active) {
          return;
        }
        setItems(notificationItems);
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
          setError(requestError instanceof Error ? requestError.message : "加载通知工作台失败");
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
  }, [canCoordination, currentUser?.department, isLead, loadDepartments, loadNotifications]);

  useEffect(() => {
    if (!canCoordination || !selectedDepartmentId) {
      setMembers([]);
      return;
    }

    let active = true;
    loadDepartmentMembers(Number(selectedDepartmentId))
      .then((departmentMembers) => {
        if (active) {
          setMembers(departmentMembers);
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

    loadNotifications(query)
      .then((notificationItems) => {
        if (active) {
          setItems(notificationItems);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载通知工作台失败");
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
  }, [loadNotifications, selectedDepartmentId, selectedUserId, tab]);

  async function handleRead(id: number) {
    await markNotificationRead(id);
    const latest = await loadNotifications({ scope: "my" });
    setItems(latest);
  }

  const unreadCount = items.filter((item) => !item.read).length;
  const assignedCount = items.filter((item) => item.type === "INTERVIEW_ASSIGNED").length;
  const selectedDepartmentName = departments.find((item) => item.id === Number(selectedDepartmentId))?.name ?? "全部部门";

  const groupedPreview = useMemo(() => {
    const groups = new Map<string, number>();
    items.forEach((item) => {
      const interviewerName = String(item.payload?.interviewerName ?? "未分组");
      groups.set(interviewerName, (groups.get(interviewerName) ?? 0) + 1);
    });
    return Array.from(groups.entries()).slice(0, 3);
  }, [items]);

  return (
    <div className="space-y-6 p-6">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-semibold text-gray-900">我的通知</h2>
            <p className="mt-1 text-sm text-gray-500">
              {tab === "my" ? "查看当前账号收到的站内通知。" : `查看 ${selectedDepartmentName} 的通知协调情况。`}
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
        <StatCard icon={<Bell className="h-5 w-5 text-blue-600" />} label="通知总数" value={loading ? "--" : String(items.length)} />
        <StatCard icon={<MailCheck className="h-5 w-5 text-amber-600" />} label="未读数量" value={loading ? "--" : String(unreadCount)} />
        <StatCard icon={<Users className="h-5 w-5 text-emerald-600" />} label="面试分配" value={loading ? "--" : String(assignedCount)} />
        <StatCard icon={<Users className="h-5 w-5 text-violet-600" />} label={groupedPreview[0]?.[0] ?? "员工分布"} value={loading ? "--" : String(groupedPreview[0]?.[1] ?? 0)} />
      </div>

      <div className="space-y-3">
        {loading ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">加载中...</div>
        ) : items.length === 0 ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">当前条件下暂无通知</div>
        ) : (
          items.map((item) => (
            <div key={item.id} className={`rounded-2xl border p-4 shadow-sm ${item.read ? "border-gray-200 bg-white" : "border-blue-200 bg-blue-50"}`}>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="font-medium text-gray-900">{item.title}</div>
                  <div className="mt-1 text-sm text-gray-700">{item.content}</div>
                  <div className="mt-2 flex flex-wrap gap-3 text-xs text-gray-500">
                    <span>{new Date(item.createdAt).toLocaleString("zh-CN")}</span>
                    <span>{item.type}</span>
                    {item.payload?.departmentName && <span>部门：{String(item.payload.departmentName)}</span>}
                    {item.payload?.interviewerName && <span>员工：{String(item.payload.interviewerName)}</span>}
                  </div>
                </div>
                {!item.read && tab === "my" && (
                  <button
                    type="button"
                    onClick={() => void handleRead(item.id)}
                    className="rounded-lg border border-blue-300 bg-white px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-50"
                  >
                    标记已读
                  </button>
                )}
              </div>
            </div>
          ))
        )}
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
