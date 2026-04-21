import { Calendar, Download, Share2, TrendingUp, Users, CheckCircle, Clock, AlertTriangle, Printer } from "lucide-react";

export function DailyReport() {
  const today = "2026年04月20日";

  const summary = {
    newCandidates: 24,
    feedbackCompleted: 18,
    pendingReview: 38,
    interviews: 12,
    offers: 5,
    timeout: 7,
  };

  const statusBreakdown = [
    { status: "待筛选", count: 45, color: "blue" },
    { status: "待面试", count: 38, color: "yellow" },
    { status: "一面中", count: 28, color: "green" },
    { status: "二面中", count: 18, color: "purple" },
    { status: "待发Offer", count: 8, color: "indigo" },
    { status: "已录用", count: 12, color: "green" },
  ];

  const timeoutCandidates = [
    { id: 1, name: "张三", position: "前端工程师", dept: "技术部", days: 3, owner: "王总" },
    { id: 2, name: "李四", position: "产品经理", dept: "产品部", days: 2, owner: "张经理" },
    { id: 3, name: "王五", position: "UI设计师", dept: "设计部", days: 4, owner: "刘总监" },
    { id: 4, name: "赵六", position: "Java工程师", dept: "技术部", days: 2, owner: "王总" },
  ];

  const todayProgress = [
    {
      id: 1,
      name: "张伟",
      position: "前端工程师",
      dept: "技术部",
      status: "已完成部门筛选",
      time: "10:30",
      result: "通过",
    },
    {
      id: 2,
      name: "王芳",
      position: "产品经理",
      dept: "产品部",
      status: "已完成一面",
      time: "14:20",
      result: "通过",
    },
    {
      id: 3,
      name: "李娜",
      position: "UI设计师",
      dept: "设计部",
      status: "已完成二面",
      time: "16:00",
      result: "通过",
    },
    {
      id: 4,
      name: "赵强",
      position: "Java工程师",
      dept: "技术部",
      status: "已发放Offer",
      time: "17:30",
      result: "已接受",
    },
    {
      id: 5,
      name: "陈静",
      position: "运营专员",
      dept: "运营部",
      status: "已完成部门筛选",
      time: "11:15",
      result: "未通过",
    },
  ];

  const departmentStats = [
    { dept: "技术部", pending: 12, completed: 8, offered: 3 },
    { dept: "产品部", pending: 8, completed: 5, offered: 1 },
    { dept: "设计部", pending: 6, completed: 3, offered: 1 },
    { dept: "运营部", pending: 7, completed: 2, offered: 0 },
    { dept: "市场部", pending: 5, completed: 0, offered: 0 },
  ];

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-xl p-8 text-white shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <Calendar className="w-6 h-6" />
              <h1 className="text-3xl font-bold">{today} 招聘日报</h1>
            </div>
            <p className="text-blue-100">今日招聘进展汇总报告</p>
          </div>
          <div className="flex gap-3">
            <button className="flex items-center gap-2 px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors backdrop-blur-sm">
              <Share2 className="w-5 h-5" />
              <span className="font-medium">分享</span>
            </button>
            <button className="flex items-center gap-2 px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors backdrop-blur-sm">
              <Printer className="w-5 h-5" />
              <span className="font-medium">打印</span>
            </button>
            <button className="flex items-center gap-2 px-4 py-2 bg-white text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
              <Download className="w-5 h-5" />
              <span className="font-medium">导出报告</span>
            </button>
          </div>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <Users className="w-8 h-8 text-blue-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">今日新增</p>
          <p className="text-3xl font-bold text-gray-900">{summary.newCandidates}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">完成反馈</p>
          <p className="text-3xl font-bold text-gray-900">{summary.feedbackCompleted}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <Clock className="w-8 h-8 text-yellow-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">待反馈</p>
          <p className="text-3xl font-bold text-gray-900">{summary.pendingReview}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <Users className="w-8 h-8 text-purple-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">今日面试</p>
          <p className="text-3xl font-bold text-gray-900">{summary.interviews}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <TrendingUp className="w-8 h-8 text-green-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">发放Offer</p>
          <p className="text-3xl font-bold text-gray-900">{summary.offers}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <AlertTriangle className="w-8 h-8 text-red-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">超时未处理</p>
          <p className="text-3xl font-bold text-gray-900">{summary.timeout}</p>
        </div>
      </div>

      {/* Status Breakdown */}
      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">当前各状态人数</h3>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {statusBreakdown.map((item, index) => (
            <div key={index} className="text-center p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-2">{item.status}</p>
              <p className="text-2xl font-bold text-gray-900">{item.count}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Timeout Alerts */}
      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-red-600" />
            <h3 className="text-lg font-semibold text-gray-900">超时未处理名单</h3>
            <span className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
              {timeoutCandidates.length} 人
            </span>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-red-50 border-b border-red-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">姓名</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">岗位</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">部门</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">负责人</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">超时天数</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-gray-700 uppercase">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {timeoutCandidates.map((candidate) => (
                <tr key={candidate.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{candidate.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{candidate.position}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{candidate.dept}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{candidate.owner}</td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
                      {candidate.days} 天
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button className="text-sm text-red-600 hover:text-red-700 font-medium">催办</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Department Statistics */}
      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">各部门招聘进展</h3>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">部门</th>
                <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase">待处理</th>
                <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase">已完成反馈</th>
                <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase">已录用</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-gray-700 uppercase">完成率</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {departmentStats.map((dept, index) => {
                const total = dept.pending + dept.completed;
                const rate = total > 0 ? ((dept.completed / total) * 100).toFixed(0) : "0";
                return (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{dept.dept}</td>
                    <td className="px-4 py-3 text-sm text-center">
                      <span className="px-2 py-1 bg-yellow-100 text-yellow-700 rounded-full font-medium">
                        {dept.pending}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-center">
                      <span className="px-2 py-1 bg-green-100 text-green-700 rounded-full font-medium">
                        {dept.completed}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-center">
                      <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded-full font-medium">
                        {dept.offered}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-right font-medium text-gray-900">{rate}%</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Today's Progress */}
      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">候选人进展明细</h3>
        <div className="space-y-3">
          {todayProgress.map((item) => (
            <div
              key={item.id}
              className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <div className="flex items-center gap-4 flex-1">
                <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <span className="text-blue-600 font-medium">{item.name[0]}</span>
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <span className="font-medium text-gray-900">{item.name}</span>
                    <span className="text-sm text-gray-600">· {item.position}</span>
                    <span className="text-sm text-gray-600">· {item.dept}</span>
                  </div>
                  <p className="text-sm text-gray-600">{item.status}</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-500">{item.time}</span>
                <span
                  className={`px-3 py-1 rounded-full text-xs font-medium ${
                    item.result.includes("通过") || item.result === "已接受"
                      ? "bg-green-100 text-green-700"
                      : "bg-red-100 text-red-700"
                  }`}
                >
                  {item.result}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Footer Note */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
        <p className="text-sm text-blue-900">
          报告生成时间：{today} 18:00 · 数据来源：招聘协同平台
        </p>
      </div>
    </div>
  );
}
