import { CheckCircle, XCircle, Clock } from "lucide-react";

export function DepartmentCompleted() {
  const completedRecords = [
    {
      id: 1,
      name: "王芳",
      position: "产品经理",
      result: "通过",
      date: "2026-04-19 14:20",
      feedback: "产品思维清晰，有丰富的B端产品经验，建议安排一面",
    },
    {
      id: 2,
      name: "周杰",
      position: "产品运营",
      result: "通过",
      date: "2026-04-18 16:30",
      feedback: "运营经验丰富，数据分析能力强，可以进入面试环节",
    },
    {
      id: 3,
      name: "陈静",
      position: "产品助理",
      result: "未通过",
      date: "2026-04-17 10:15",
      feedback: "工作经验不足，不符合岗位要求",
    },
    {
      id: 4,
      name: "赵六",
      position: "产品经理",
      result: "通过",
      date: "2026-04-16 11:45",
      feedback: "有成功的产品案例，沟通能力好，推荐面试",
    },
    {
      id: 5,
      name: "孙丽",
      position: "产品运营",
      result: "未通过",
      date: "2026-04-15 09:20",
      feedback: "缺乏相关行业经验",
    },
  ];

  return (
    <div className="p-6 space-y-6">
      {/* Summary */}
      <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">筛选统计</h3>
        <div className="grid grid-cols-3 gap-6">
          <div className="text-center">
            <p className="text-3xl font-bold text-gray-900 mb-1">
              {completedRecords.length}
            </p>
            <p className="text-sm text-gray-600">累计已处理</p>
          </div>
          <div className="text-center">
            <p className="text-3xl font-bold text-green-600 mb-1">
              {completedRecords.filter((r) => r.result === "通过").length}
            </p>
            <p className="text-sm text-gray-600">通过人数</p>
          </div>
          <div className="text-center">
            <p className="text-3xl font-bold text-red-600 mb-1">
              {completedRecords.filter((r) => r.result === "未通过").length}
            </p>
            <p className="text-sm text-gray-600">淘汰人数</p>
          </div>
        </div>
      </div>

      {/* Records List */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">处理记录</h3>
        </div>

        <div className="divide-y divide-gray-200">
          {completedRecords.map((record) => (
            <div key={record.id} className="px-6 py-4">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center">
                    <span className="text-purple-600 font-medium">
                      {record.name[0]}
                    </span>
                  </div>
                  <div>
                    <div className="flex items-center gap-3 mb-1">
                      <h4 className="font-semibold text-gray-900">{record.name}</h4>
                      <span className="text-sm text-gray-600">· {record.position}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-gray-500">
                      <Clock className="w-4 h-4" />
                      <span>{record.date}</span>
                    </div>
                  </div>
                </div>
                <span
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium ${
                    record.result === "通过"
                      ? "bg-green-100 text-green-700"
                      : "bg-red-100 text-red-700"
                  }`}
                >
                  {record.result === "通过" ? (
                    <CheckCircle className="w-4 h-4" />
                  ) : (
                    <XCircle className="w-4 h-4" />
                  )}
                  {record.result}
                </span>
              </div>
              <div className="ml-13 pl-3 border-l-2 border-gray-200">
                <p className="text-sm text-gray-700">{record.feedback}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
