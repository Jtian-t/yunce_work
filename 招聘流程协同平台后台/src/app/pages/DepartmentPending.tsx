import { Link } from "react-router";
import { Clock, FileText, User } from "lucide-react";
import { useData } from "../context/DataContext";

export function DepartmentPending() {
  const { departmentTasks, candidates, loading, error } = useData();

  const pendingCandidates = departmentTasks.map((task) => ({
    task,
    candidate: candidates.find((candidate) => candidate.id === task.candidateId),
  }));

  return (
    <div className="p-6 space-y-6">
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          联调失败：{error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <Clock className="w-8 h-8 text-purple-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">待处理简历</p>
          <p className="text-3xl font-bold text-gray-900">{loading ? "--" : pendingCandidates.length}</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <FileText className="w-8 h-8 text-green-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">实时任务来源</p>
          <p className="text-3xl font-bold text-gray-900">API</p>
        </div>
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <User className="w-8 h-8 text-blue-600" />
          </div>
          <p className="text-sm text-gray-600 mb-1">待反馈部门数</p>
          <p className="text-3xl font-bold text-gray-900">
            {new Set(pendingCandidates.map((item) => item.task.department)).size}
          </p>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">待处理简历列表</h3>
          <p className="text-sm text-gray-600 mt-1">
            共 {pendingCandidates.length} 份简历等待提交反馈
          </p>
        </div>

        <div className="divide-y divide-gray-200">
          {!loading && pendingCandidates.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <FileText className="w-12 h-12 text-gray-400 mx-auto mb-3" />
              <p className="text-gray-600">暂无待处理简历</p>
            </div>
          ) : (
            pendingCandidates.map(({ task, candidate }) => (
              <div key={task.id} className="px-6 py-4 hover:bg-gray-50 transition-colors">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-4 flex-1">
                    <div className="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0">
                      <span className="text-purple-600 font-semibold">
                        {(candidate?.name ?? task.candidateName)[0]}
                      </span>
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h4 className="font-semibold text-gray-900 text-lg">
                          {candidate?.name ?? task.candidateName}
                        </h4>
                        <span className="px-2 py-1 bg-yellow-100 text-yellow-700 text-xs font-medium rounded-full">
                          {candidate?.status ?? task.status}
                        </span>
                      </div>
                      <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-gray-600 mb-2">
                        <span>应聘岗位：{candidate?.position ?? "-"}</span>
                        <span>来源：{candidate?.source ?? "-"}</span>
                        <span>截止时间：{new Date(task.dueAt).toLocaleString("zh-CN")}</span>
                      </div>
                      <div className="flex items-center gap-2 text-sm text-gray-500">
                        <Clock className="w-4 h-4" />
                        <span>部门：{task.department} · 负责人：{task.reviewer}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 ml-4">
                    <Link
                      to={`/feedback/${task.candidateId}`}
                      className="px-6 py-2.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-center font-medium whitespace-nowrap"
                    >
                      开始筛选
                    </Link>
                    <Link
                      to={`/candidates/${task.candidateId}`}
                      className="px-6 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium whitespace-nowrap text-center"
                    >
                      查看候选人
                    </Link>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
