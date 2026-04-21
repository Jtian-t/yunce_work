import { Eye, FileSearch, Plus, Search } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router";
import { useEffect, useMemo, useState } from "react";
import { useData } from "../context/DataContext";

const statusOptions = [
  { value: "ALL", label: "全部状态" },
  { value: "POOL", label: "简历池" },
  { value: "NEW", label: "新建" },
  { value: "IN_DEPT_REVIEW", label: "部门处理中" },
  { value: "PENDING_INTERVIEW", label: "待安排面试" },
  { value: "INTERVIEWING", label: "面试中" },
  { value: "INTERVIEW_PASSED", label: "面试通过" },
  { value: "OFFER_PENDING", label: "待发 Offer" },
  { value: "OFFER_SENT", label: "已发 Offer" },
  { value: "HIRED", label: "已录用" },
  { value: "REJECTED", label: "已淘汰" },
  { value: "TIMEOUT", label: "超时" },
];

export function CandidateList() {
  const location = useLocation();
  const navigate = useNavigate();
  const { candidates, previewResume } = useData();
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("ALL");
  const [selectedDept, setSelectedDept] = useState("ALL");
  const [pageMessage, setPageMessage] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);

  useEffect(() => {
    const message = (location.state as { successMessage?: string } | null)?.successMessage;
    if (message) {
      setPageMessage(message);
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate]);

  const departmentOptions = useMemo(
    () => ["ALL", ...new Set(candidates.map((candidate) => candidate.department).filter(Boolean) as string[])],
    [candidates]
  );

  const filteredCandidates = useMemo(() => {
    return candidates.filter((candidate) => {
      const query = searchTerm.trim().toLowerCase();
      const matchesQuery =
        !query ||
        candidate.name.toLowerCase().includes(query) ||
        candidate.position.toLowerCase().includes(query) ||
        (candidate.department ?? "").toLowerCase().includes(query);

      const matchesDepartment =
        selectedDept === "ALL" ? true : (candidate.department ?? "简历池") === selectedDept;

      const matchesStatus =
        selectedStatus === "ALL"
          ? true
          : selectedStatus === "POOL"
          ? candidate.department == null
          : candidate.statusCode === selectedStatus;

      return matchesQuery && matchesDepartment && matchesStatus;
    });
  }, [candidates, searchTerm, selectedDept, selectedStatus]);

  function getStatusColor(statusCode: string) {
    switch (statusCode) {
      case "NEW":
        return "bg-slate-100 text-slate-700";
      case "IN_DEPT_REVIEW":
        return "bg-amber-100 text-amber-700";
      case "PENDING_INTERVIEW":
        return "bg-sky-100 text-sky-700";
      case "INTERVIEWING":
        return "bg-emerald-100 text-emerald-700";
      case "INTERVIEW_PASSED":
        return "bg-green-100 text-green-700";
      case "OFFER_PENDING":
      case "OFFER_SENT":
        return "bg-violet-100 text-violet-700";
      case "HIRED":
        return "bg-indigo-100 text-indigo-700";
      case "REJECTED":
        return "bg-gray-100 text-gray-700";
      case "TIMEOUT":
        return "bg-red-100 text-red-700";
      default:
        return "bg-gray-100 text-gray-700";
    }
  }

  return (
    <div className="p-6 space-y-6">
      {pageMessage && (
        <div className="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
          {pageMessage}
        </div>
      )}

      {pageError && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {pageError}
        </div>
      )}

      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="搜索候选人姓名、岗位或部门"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              className="w-full rounded-lg border border-gray-300 py-2.5 pl-10 pr-4"
            />
          </div>

          <div className="flex flex-wrap gap-3">
            <select
              value={selectedDept}
              onChange={(event) => setSelectedDept(event.target.value)}
              className="rounded-lg border border-gray-300 px-4 py-2.5"
            >
              {departmentOptions.map((department) => (
                <option key={department} value={department}>
                  {department === "ALL" ? "全部部门" : department}
                </option>
              ))}
            </select>

            <select
              value={selectedStatus}
              onChange={(event) => setSelectedStatus(event.target.value)}
              className="rounded-lg border border-gray-300 px-4 py-2.5"
            >
              {statusOptions.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>

            <Link
              to="/candidates/new"
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              新增候选人
            </Link>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-600">
          当前共 <span className="font-semibold text-gray-900">{filteredCandidates.length}</span> 位候选人
        </p>
      </div>

      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">候选人</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">岗位</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">部门</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">状态</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">负责人</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">推荐日期</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">下一步</th>
                <th className="px-6 py-3 text-right text-xs font-semibold text-gray-700 uppercase">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredCandidates.map((candidate) => (
                <tr key={candidate.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-sm font-semibold text-blue-700">
                        {candidate.name[0]}
                      </div>
                      <div>
                        <div className="font-medium text-gray-900">{candidate.name}</div>
                        <div className="text-sm text-gray-500">{candidate.source}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900">{candidate.position}</td>
                  <td className="px-6 py-4 text-sm text-gray-900">{candidate.department ?? "简历池"}</td>
                  <td className="px-6 py-4">
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${getStatusColor(candidate.statusCode)}`}>
                      {candidate.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900">{candidate.owner ?? "HR 待处理"}</td>
                  <td className="px-6 py-4 text-sm text-gray-900">{candidate.submittedDate}</td>
                  <td className="px-6 py-4 text-sm text-gray-700">{candidate.nextAction}</td>
                  <td className="px-6 py-4">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() =>
                          void previewResume(candidate.id).catch((requestError) => {
                            setPageError(requestError instanceof Error ? requestError.message : "简历预览失败");
                          })
                        }
                        className="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
                      >
                        <FileSearch className="h-4 w-4" />
                        简历
                      </button>
                      <Link
                        to={`/candidates/${candidate.id}`}
                        className="inline-flex items-center gap-1 rounded-lg bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-100"
                      >
                        <Eye className="h-4 w-4" />
                        详情
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
