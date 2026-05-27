import { Outlet, Link, useLocation } from 'react-router-dom';
import { SessionManager } from '../../shared/auth/sessionManager';
import Navbar from '../../shared/components/Navbar';
import { Users, FolderGit2, ShieldAlert, FileText, BarChart3, ClipboardList, Activity } from 'lucide-react';

export function AdminLayout() {
  const user = SessionManager.getUser();
  const location = useLocation();

  if (!user || user.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 px-4">
        <ShieldAlert className="w-16 h-16 text-red-500 mb-4" />
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Access Denied</h1>
        <p className="text-gray-600 mb-6 text-center max-w-md">
          You do not have the required administrative privileges to view this section.
        </p>
        <Link to="/dashboard" className="bg-indigo-600 text-white px-6 py-2 rounded-md hover:bg-indigo-700 transition cursor-pointer">
          Return to Dashboard
        </Link>
      </div>
    );
  }

  const navigation = [
    { name: 'Dashboard', href: '/admin/stats', icon: BarChart3 },
    { name: 'Users Management', href: '/admin/users', icon: Users },
    { name: 'Repositories', href: '/admin/repositories', icon: FolderGit2 },
    { name: 'Requests', href: '/admin/requests', icon: ClipboardList },
    { name: 'Content Moderation', href: '/admin/materials', icon: FileText },
    { name: 'Activities', href: '/admin/activity', icon: Activity },
  ];

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      
      <div className="flex-1 flex flex-col lg:flex-row w-full px-4 sm:px-6 lg:px-8 py-4 gap-6">
        {/* Sidebar Navigation */}
        <div className="w-full lg:w-64 lg:flex-shrink-0">
          <nav className="space-y-1">
            <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4 px-3">
              Admin Panel
            </h2>
            {navigation.map((item) => {
              const isActive = location.pathname.startsWith(item.href);
              const Icon = item.icon;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`
                    group flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors cursor-pointer
                    ${isActive 
                      ? 'bg-green-50 text-green-700' 
                      : 'text-gray-700 hover:text-green-700 hover:bg-green-50'
                    }
                  `}
                >
                  <Icon
                    className={`flex-shrink-0 -ml-1 mr-3 h-5 w-5 ${
                      isActive ? 'text-green-700' : 'text-gray-400 group-hover:text-green-700'
                    }`}
                  />
                  <span className="truncate">{item.name}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* Main Content Area */}
        <div className="flex-1 min-w-0">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
