import { Outlet, Link, useLocation } from "react-router";
import { LayoutDashboard, Users, FileText, TrendingUp, Menu } from "lucide-react";
import { useState } from "react";
import { useData } from "../../context/DataContext";

export function MainLayout() {
  const location = useLocation();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const { currentUser } = useData();

  const navItems = [
    { path: "/", label: "招聘总览", icon: LayoutDashboard },
    { path: "/candidates", label: "候选人列表", icon: Users },
    { path: "/report", label: "每日报告", icon: FileText },
  ];

  const isActive = (path: string) => {
    if (path === "/") return location.pathname === "/";
    return location.pathname.startsWith(path);
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className={`bg-white border-r border-gray-200 flex flex-col transition-all duration-300 ${sidebarCollapsed ? "w-16" : "w-64"}`}>
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          {!sidebarCollapsed ? (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center">
                <TrendingUp className="w-5 h-5 text-white" />
              </div>
              <span className="font-semibold text-gray-900">招聘协同平台</span>
            </div>
          ) : (
            <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center mx-auto">
              <TrendingUp className="w-5 h-5 text-white" />
            </div>
          )}
        </div>

        <nav className="flex-1 px-3 py-4">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg mb-1 transition-colors ${
                  active ? "bg-blue-50 text-blue-600" : "text-gray-700 hover:bg-gray-50"
                }`}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                {!sidebarCollapsed && <span className="font-medium">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        <div className="p-3 border-t border-gray-200">
          <button
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="w-full flex items-center justify-center gap-3 px-3 py-2.5 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            <Menu className="w-5 h-5" />
            {!sidebarCollapsed && <span className="font-medium">收起菜单</span>}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-gray-900">
              {navItems.find((item) => isActive(item.path))?.label || "招聘协同平台"}
            </h1>
          </div>
          <div className="flex items-center gap-4">
            <Link
              to="/dept"
              className="px-4 py-2 border border-purple-300 text-purple-600 rounded-lg hover:bg-purple-50 transition-colors text-sm font-medium"
            >
              切换到部门视角
            </Link>
            <div className="flex items-center gap-3 px-4 py-2 rounded-lg bg-gray-50">
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                <span className="text-white text-sm font-medium">
                  {(currentUser?.displayName ?? "HR").slice(0, 2).toUpperCase()}
                </span>
              </div>
              <div className="text-sm">
                <div className="font-medium text-gray-900">{currentUser?.displayName ?? "HR 管理员"}</div>
                <div className="text-gray-500">{currentUser?.email ?? "admin@company.com"}</div>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
