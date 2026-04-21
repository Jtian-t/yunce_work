import { useEffect, useState } from "react";
import { useData, type NotificationItem } from "../context/DataContext";

export function NotificationsPage() {
  const { loadNotifications, markNotificationRead } = useData();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    loadNotifications()
      .then(setItems)
      .catch((requestError) => {
        setError(requestError instanceof Error ? requestError.message : "加载通知失败");
      })
      .finally(() => setLoading(false));
  }, [loadNotifications]);

  async function handleRead(id: number) {
    await markNotificationRead(id);
    const latest = await loadNotifications();
    setItems(latest);
  }

  return (
    <div className="p-6 space-y-6">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-gray-900">我的通知</h2>
        <p className="mt-1 text-sm text-gray-500">站内通知，包括面试分配、任务催办和流程提醒。</p>
      </div>
      <div className="space-y-3">
        {loading ? (
          <div className="text-sm text-gray-500">加载中...</div>
        ) : items.length === 0 ? (
          <div className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">暂无通知</div>
        ) : (
          items.map((item) => (
            <div key={item.id} className={`rounded-xl border p-4 shadow-sm ${item.read ? "border-gray-200 bg-white" : "border-blue-200 bg-blue-50"}`}>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="font-medium text-gray-900">{item.title}</div>
                  <div className="mt-1 text-sm text-gray-700">{item.content}</div>
                  <div className="mt-1 text-xs text-gray-500">{new Date(item.createdAt).toLocaleString("zh-CN")} · {item.type}</div>
                </div>
                {!item.read && (
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
