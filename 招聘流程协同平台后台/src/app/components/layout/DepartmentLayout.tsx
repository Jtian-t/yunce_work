import { Outlet, Link, useLocation } from "react-router";
import { ClipboardList, Users, LogOut, Bell } from "lucide-react";
import { useData } from "../../context/DataContext";

export function DepartmentLayout() {
  const location = useLocation();
  const { notifications } = useData();
  const unread = notifications.filter((item) => !item.read).length;

  const navItems = [
    { path: "/dept", label: "待处理简历", icon: ClipboardList },
    { path: "/dept/completed", label: "已处理记录", icon: Users },
  ];

  const isActive = (path: string) => {
    if (path === "/dept") return location.pathname === "/dept";
    return location.pathname.startsWith(path);
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="bg-white border-r border-gray-200 flex flex-col w-64">
        {/* Logo */}
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-purple-600 flex items-center justify-center">
              <ClipboardList className="w-5 h-5 text-white" />
            </div>
            <span className="font-semibold text-gray-900">部门简历筛选</span>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg mb-1 transition-colors ${
                  active
                    ? "bg-purple-50 text-purple-600"
                    : "text-gray-700 hover:bg-gray-50"
                }`}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                <span className="font-medium">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* User Info */}
        <div className="p-4 border-t border-gray-200">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 rounded-full bg-purple-600 flex items-center justify-center">
              <span className="text-white text-sm font-medium">张</span>
            </div>
            <div className="flex-1">
              <div className="font-medium text-gray-900">张三</div>
              <div className="text-xs text-gray-500">产品部 · 产品总监</div>
            </div>
          </div>
          <Link
            to="/"
            className="w-full flex items-center justify-center gap-2 px-3 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors text-sm"
          >
            <LogOut className="w-4 h-4" />
            <span>切换到HR视角</span>
          </Link>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Bar */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-gray-900">
              {navItems.find((item) => isActive(item.path))?.label || "部门简历筛选"}
            </h1>
          </div>
          <div className="flex items-center gap-4">
            <button className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors">
              <Bell className="w-5 h-5 text-gray-600" />
              {unread > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center">
                  {unread}
                </span>
              )}
            </button>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
