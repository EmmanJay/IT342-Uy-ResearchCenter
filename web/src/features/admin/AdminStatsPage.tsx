import { useState, useEffect } from 'react';
import { adminApi } from './api/adminApi';
import type { AdminStats } from './types';
import { Users, BookOpen, FileText, BarChart3, ClipboardList } from 'lucide-react';
import LoadingScreen from '../../shared/components/LoadingScreen';

export default function AdminStatsPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getStats();
      if (res.success) {
        setStats(res.data);
      }
    } catch (err) {
      setError('Failed to fetch statistics');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingScreen label="Loading statistics" fullScreen={false} />;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>;
  }

  if (!stats) {
    return <div className="p-8 text-center text-gray-500">No statistics available</div>;
  }

  const statCards = [
    {
      title: 'Total Users',
      value: stats.totalUsers,
      icon: Users,
      color: 'bg-blue-100 text-blue-600',
      bgColor: 'bg-blue-50'
    },
    {
      title: 'Total Repositories',
      value: stats.totalRepositories,
      icon: BookOpen,
      color: 'bg-green-100 text-green-600',
      bgColor: 'bg-green-50'
    },
    {
      title: 'Total Materials',
      value: stats.totalMaterials,
      icon: FileText,
      color: 'bg-purple-100 text-purple-600',
      bgColor: 'bg-purple-50'
    },
    {
      title: 'Total Requests',
      value: stats.totalRequests,
      icon: ClipboardList,
      color: 'bg-orange-100 text-orange-600',
      bgColor: 'bg-orange-50'
    }
  ];

  return (
    <div className="p-6">
      <div className="mb-8 flex items-center">
        <BarChart3 className="h-8 w-8 text-gray-900 mr-3" />
        <h1 className="text-2xl font-bold text-gray-900">System Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              className={`${card.bgColor} rounded-lg p-4 border border-gray-200 shadow-sm hover:shadow-md transition-shadow min-h-[160px] flex flex-col`}
            >
              <div className="flex items-end justify-between mb-2">
                <h3 className="text-gray-600 font-medium text-base">{card.title}</h3>
                <div className={`${card.color} p-2 rounded-lg`}>
                  <Icon className="h-5 w-5" />
                </div>
              </div>
              <div className="text-7xl font-black leading-none tracking-tight text-gray-900">
                {card.value}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-8 bg-gray-50 rounded-lg p-6 border border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Overview</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
          <div>
            <p className="font-medium text-gray-700 mb-2">Research Community</p>
            <ul className="list-disc list-inside space-y-1 text-gray-600">
              <li>{stats.totalUsers} registered users</li>
              <li>{stats.totalRepositories} active repositories</li>
              <li>{stats.totalMaterials} research materials shared</li>
              <li>{stats.totalRequests} materials requested</li>
            </ul>
          </div>
          <div>
            <p className="font-medium text-gray-700 mb-2">Quick Facts</p>
            <ul className="list-disc list-inside space-y-1 text-gray-600">
              <li>Average materials per repository: {(stats.totalMaterials / Math.max(stats.totalRepositories, 1)).toFixed(1)}</li>
              <li>Active platform with growing community</li>
              <li>Monitor activity regularly for moderation</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
