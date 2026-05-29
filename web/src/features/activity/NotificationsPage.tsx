import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { axiosClient } from '../../shared/api/axiosClient';
import { useWebSocket } from '../../shared/context/WebSocketProvider';
import { formatDate } from '../../shared/utils/dateUtils';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { formatActivityMessage } from '../../shared/utils/activityFormat';
import Navbar from '../../shared/components/Navbar';

interface ActivityLog {
  id: number;
  actorName?: string;
  action: string;
  targetType: string;
  targetId: number;
  targetName: string;
  repositoryId?: number;
  repositoryName?: string;
  description?: string;
  createdAt: string;
}

const SEEN_KEY = 'rc_seen_activity_id';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [activities, setActivities] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const { lastMessage } = useWebSocket();

  const fetchActivities = useCallback(async (page: number, append: boolean = false) => {
    try {
      if (!append) setLoading(true);
      const res = await axiosClient.get(`/activities/notifications?page=${page}&size=10`);
      if (res.data.success) {
        const items: ActivityLog[] = res.data.data;
        if (append) {
          setActivities(prev => [...prev, ...items]);
        } else {
          setActivities(items);
          if (items.length > 0) {
            const maxId = Math.max(...items.map((a) => a.id));
            localStorage.setItem(SEEN_KEY, String(maxId));
          }
        }
        setHasMore(items.length === 10);
      }
    } catch {
      // fail silently
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchActivities(0, false);
  }, [fetchActivities]);

  useEffect(() => {
    if (!lastMessage || lastMessage.type === 'AUTH_SUCCESS') return;
    fetchActivities(0, false);
  }, [lastMessage, fetchActivities]);

  const handleItemClick = (activity: ActivityLog) => {
    if (activity.repositoryId) {
      let tab = 'activity';
      const type = activity.targetType || '';
      if (type.includes('MATERIAL')) tab = 'materials';
      else if (type.includes('REQUEST')) tab = 'requests';
      else if (type.includes('MEMBER') || type.includes('JOINED') || type.includes('LEFT') || type.includes('INVIT')) tab = 'members';
      
      navigate(`/repositories/${activity.repositoryId}`, { state: { activeTab: tab } });
    } else {
      navigate('/dashboard');
    }
  };

  if (loading && activities.length === 0) {
    return <LoadingScreen label="Loading notifications" fullScreen={false} />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="max-w-4xl mx-auto px-6 py-8">
        <div className="flex items-center gap-3 mb-6 border-b border-emerald-100 pb-4">
          <div className="p-3 bg-emerald-50 text-green-700 rounded-full border border-emerald-100">
            <Bell className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
        </div>

        <div className="flex flex-col gap-4">
          <div className="max-h-[calc(100vh-220px)] overflow-y-auto pr-2 space-y-4">
          {activities.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-lg border border-emerald-100">
              <Bell className="w-12 h-12 text-green-200 mx-auto mb-3" />
              <p className="text-gray-500">No recent notifications</p>
            </div>
          ) : (
            activities.map((log) => (
              <div key={log.id} onClick={() => handleItemClick(log)} className="p-4 bg-white rounded-lg shadow-sm border border-emerald-100 hover:border-green-300 hover:bg-green-50/30 transition-colors cursor-pointer">
                <div className="flex justify-between items-start">
                  <div className="space-y-1">
                    <p className="text-gray-900 font-medium">
                      {formatActivityMessage(log, { includeRepositoryName: false, selfLabel: 'System' })}
                    </p>
                    {log.repositoryName && (
                      <p className="text-sm text-gray-500">
                        in repository <span className="font-medium">{log.repositoryName}</span>
                      </p>
                    )}
                    {log.description && (
                      <p className="text-sm text-gray-600 mt-2 bg-emerald-50 p-2 rounded border border-emerald-100">{log.description}</p>
                    )}
                    <p className="text-xs text-gray-500 mt-2">{formatDate(log.createdAt)}</p>
                  </div>
                </div>
              </div>
            ))
          )}
          </div>

          {activities.length > 0 && (
            <div className="flex justify-center items-center gap-4 pt-4 mt-6 border-t border-gray-100">
              <button
                onClick={() => {
                  const prev = Math.max(0, currentPage - 1);
                  setCurrentPage(prev);
                  fetchActivities(prev, false);
                }}
                disabled={currentPage === 0 || loading}
                className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
                Previous
              </button>
              <span className="text-sm text-gray-600">Page {currentPage + 1}</span>
              <button
                onClick={() => {
                  if (hasMore) {
                    const next = currentPage + 1;
                    setCurrentPage(next);
                    fetchActivities(next, false);
                  }
                }}
                disabled={!hasMore || loading}
                className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
              >
                Next
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
              </button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
