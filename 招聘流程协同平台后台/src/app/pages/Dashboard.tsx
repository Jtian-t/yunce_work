import {
  AlertTriangle,
  ArrowUpRight,
  CheckCircle,
  Clock,
  Users,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useData } from "../context/DataContext";

export function Dashboard() {
  const {
    dashboardOverview,
    funnelMetrics,
    statusDistribution,
    departmentEfficiency,
    alerts,
    loading,
    error,
  } = useData();

  const stats = [
    {
      label: "今日新增候选人",
      value: dashboardOverview?.newCandidatesToday ?? 0,
      icon: Users,
      color: "blue",
    },
    {
      label: "待反馈人数",
      value: dashboardOverview?.pendingFeedbackCount ?? 0,
      icon: Clock,
      color: "yellow",
    },
    {
      label: "超时未处理人数",
      value: dashboardOverview?.timeoutCount ?? 0,
      icon: AlertTriangle,
      color: "red",
    },
    {
      label: "已录用人数",
      value: dashboardOverview?.hiredCount ?? 0,
      icon: CheckCircle,
      color: "green",
    },
  ];

  const pieData = statusDistribution.map((item, index) => ({
    ...item,
    color: ["#3B82F6", "#F59E0B", "#10B981", "#8B5CF6", "#6B7280", "#EF4444"][index % 6],
  }));

  return (
    <div className="p-6 space-y-6">
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          联调失败：{error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat) => {
          const Icon = stat.icon;
          const colorClasses = {
            blue: "bg-blue-50 text-blue-600",
            yellow: "bg-yellow-50 text-yellow-600",
            red: "bg-red-50 text-red-600",
            green: "bg-green-50 text-green-600",
          }[stat.color];

          return (
            <div key={stat.label} className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <p className="text-sm text-gray-600 mb-1">{stat.label}</p>
                  <p className="text-3xl font-bold text-gray-900 mb-2">
                    {loading ? "--" : stat.value}
                  </p>
                  <div className="flex items-center gap-1 text-green-600 text-sm font-medium">
                    <ArrowUpRight className="w-4 h-4" />
                    <span>实时数据</span>
                  </div>
                </div>
                <div className={`w-12 h-12 rounded-lg ${colorClasses} flex items-center justify-center`}>
                  <Icon className="w-6 h-6" />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">招聘漏斗</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={funnelMetrics} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis type="number" stroke="#6B7280" />
              <YAxis dataKey="label" type="category" width={90} stroke="#6B7280" />
              <Tooltip />
              <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                {funnelMetrics.map((entry, index) => (
                  <Cell
                    key={`${entry.label}-${index}`}
                    fill={["#2563EB", "#60A5FA", "#93C5FD", "#A78BFA", "#C4B5FD", "#D1FAE5"][index % 6]}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">候选人状态分布</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ label, percent }) => `${label} ${((percent ?? 0) * 100).toFixed(0)}%`}
                outerRadius={100}
                dataKey="value"
                nameKey="label"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`${entry.label}-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">各部门反馈时效</h3>
        <div className="space-y-3">
          {departmentEfficiency.map((dept, index) => (
            <div key={dept.department} className="flex items-center gap-4">
              <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center font-semibold text-gray-700">
                {index + 1}
              </div>
              <div className="flex-1">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium text-gray-900">{dept.department}</span>
                  <span className="text-sm text-gray-600">平均 {dept.averageDays} 天</span>
                </div>
                <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${
                      dept.averageDays < 2 ? "bg-green-500" : dept.averageDays < 3 ? "bg-yellow-500" : "bg-red-500"
                    }`}
                    style={{ width: `${Math.min(((5 - dept.averageDays) / 5) * 100, 100)}%` }}
                  />
                </div>
              </div>
              <span className="text-sm text-gray-500">{dept.completedCount} 次</span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <AlertTriangle className="w-5 h-5 text-red-600" />
          <h3 className="text-lg font-semibold text-gray-900">异常提醒</h3>
          <span className="ml-auto px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
            {alerts.length} 条
          </span>
        </div>
        <div className="space-y-3">
          {alerts.length === 0 ? (
            <div className="text-sm text-gray-500">当前没有超时异常。</div>
          ) : (
            alerts.map((alert) => (
              <div key={alert.assignmentId} className="flex items-center gap-3 p-3 rounded-lg border border-red-200 bg-red-50">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-medium text-gray-900">{alert.candidateName}</span>
                    <span className="text-sm text-gray-600">· {alert.position}</span>
                  </div>
                  <p className="text-sm text-gray-600">
                    {alert.department} · 已超时约 {Math.ceil(alert.overdueHours / 24)} 天
                  </p>
                </div>
                <button className="px-3 py-1.5 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition-colors">
                  催办
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
