import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { axiosClient } from '../api/axiosClient';
import { useWebSocket } from '../context/WebSocketProvider';
import { formatDate } from '../utils/dateUtils';
import { formatActivityMessage } from '../utils/activityFormat';

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

const NotificationDropdown = () => {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [activities, setActivities] = useState<ActivityLog[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { lastMessage } = useWebSocket();

  const getLastSeenId = () => {
    const val = localStorage.getItem(SEEN_KEY);
    return val ? parseInt(val, 10) : 0;
  };

  const fetchActivities = useCallback(async () => {
    try {
      setLoading(true);
      const res = await axiosClient.get('/activities/notifications?page=0&size=10');
      if (res.data.success) {
        const items: ActivityLog[] = res.data.data;
        setActivities(items);
        const lastSeen = getLastSeenId();
        const unseen = items.filter((a) => a.id > lastSeen).length;
        setUnreadCount(unseen);
      }
    } catch {
      // silently fail
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  useEffect(() => {
    if (!lastMessage || lastMessage.type === 'AUTH_SUCCESS') return;
    fetchActivities();
  }, [lastMessage, fetchActivities]);

  useEffect(() => {
    const interval = setInterval(fetchActivities, 30000);
    return () => clearInterval(interval);
  }, [fetchActivities]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggle = () => {
    setOpen((prev) => !prev);
    if (!open && activities.length > 0) {
      const maxId = Math.max(...activities.map((a) => a.id));
      localStorage.setItem(SEEN_KEY, String(maxId));
      setUnreadCount(0);
    }
  };

  const handleItemClick = (activity: ActivityLog) => {
    setOpen(false);
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

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={handleToggle}
        className="relative p-1.5 rounded-full text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors cursor-pointer inline-flex items-center justify-center"
        title="Notifications"
        id="notification-bell"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 rounded-xl bg-white shadow-lg border border-gray-200 z-50 overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between shrink-0">
            <h3 className="text-sm font-semibold text-gray-900">Notifications</h3>
          </div>
          <div className="max-h-[300px] overflow-y-auto">
            {loading && activities.length === 0 ? (
              <div className="px-4 py-6 text-center text-sm text-gray-500">Loading...</div>
            ) : activities.length === 0 ? (
              <div className="px-4 py-6 text-center text-sm text-gray-500">No notifications yet</div>
            ) : (
              activities.slice(0, 10).map((activity) => (
                <button
                  key={activity.id}
                  type="button"
                  onClick={() => handleItemClick(activity)}
                  className="w-full text-left px-4 py-3 hover:bg-gray-50 border-b border-gray-50 last:border-b-0 transition-colors cursor-pointer"
                >
                  <p className="text-sm text-gray-700 leading-snug line-clamp-2">
                    {formatActivityMessage(activity, { includeRepositoryName: false })}
                  </p>
                  <div className="flex items-center gap-2 mt-1">
                    {activity.repositoryName && (
                      <span className="text-xs text-green-600 font-medium truncate max-w-[140px]">
                        {activity.repositoryName}
                      </span>
                    )}
                    <span className="text-xs text-gray-400">{formatDate(activity.createdAt)}</span>
                  </div>
                </button>
              ))
            )}
          </div>
          {activities.length > 0 && (
            <div className="px-4 py-2 border-t border-gray-100 shrink-0 text-center">
              <Link
                to="/notifications"
                onClick={() => setOpen(false)}
                className="text-xs font-medium text-green-600 hover:text-green-700 block py-1"
              >
                View all notifications
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationDropdown;
