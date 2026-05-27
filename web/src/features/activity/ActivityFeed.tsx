import { useState, useEffect, useCallback, useRef } from 'react';
import { axiosClient } from '../../shared/api/axiosClient';
import { formatDate } from '../../shared/utils/dateUtils';
import { Activity, BookOpen, UserPlus, FolderPlus, CheckCircle, XCircle, Trash2, LogOut } from 'lucide-react';
import { useWebSocket } from '../../shared/context/WebSocketProvider';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { formatActivityMessage, getActivityActionKey } from '../../shared/utils/activityFormat';

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

type ActivityFeedProps = {
  repositoryId?: string | number;
  fullPage?: boolean;
  limit?: number;
  hideHeader?: boolean;
};

export function ActivityFeed({ repositoryId, fullPage = false, limit = 10, hideHeader = false }: ActivityFeedProps) {
  const [activities, setActivities] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const endpointUnavailableRef = useRef(false);
  const hasFetchedRef = useRef(false);
  const { lastMessage } = useWebSocket();
  const pageSize = limit;

  const fetchActivities = useCallback(async () => {
    if (endpointUnavailableRef.current) return;
    try {
      if (!hasFetchedRef.current) setLoading(true);
      setLoadError('');
      const url = repositoryId === 'all'
        ? '/activities/all?page=0&size=50'
        : repositoryId
          ? `/activities?repositoryId=${repositoryId}&page=0&size=100`
          : fullPage
            ? '/activities?page=0&size=50'
            : '/activities?page=0&size=10';
      const res = await axiosClient.get(url);
      if (res.data.success) {
        setActivities(res.data.data);
        setCurrentPage(1);
      }
    } catch (err) {
      const status = (err as any)?.response?.status;
      const message = (err as any)?.response?.data?.error?.message || (err as any)?.message || '';
      if (status === 401 || status === 403) {
        return;
      }
      if (String(message).includes('No static resource')) {
        endpointUnavailableRef.current = true;
        setLoadError('Activity is unavailable until the backend is restarted with the latest routes.');
      } else {
        setLoadError('Failed to load activity.');
        console.error('Failed to load activities', err);
      }
    } finally {
      hasFetchedRef.current = true;
      setLoading(false);
    }
  }, [repositoryId, fullPage]);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  useEffect(() => {
    const interval = setInterval(() => {
      if (!endpointUnavailableRef.current) fetchActivities();
    }, 15000);
    return () => clearInterval(interval);
  }, [fetchActivities]);

  useEffect(() => {
    if (!lastMessage || lastMessage.type === 'AUTH_SUCCESS') return;
    if (repositoryId && String(lastMessage.repositoryId) !== String(repositoryId)) return;
    fetchActivities();
  }, [lastMessage, repositoryId, fetchActivities]);

  const getActionIcon = (action: string) => {
    switch (getActivityActionKey(action)) {
      case 'UPLOADED_MATERIAL': return <BookOpen className="w-4 h-4 text-blue-500" />;
      case 'CREATED_REPOSITORY': return <FolderPlus className="w-4 h-4 text-green-500" />;
      case 'UPDATED_REPOSITORY': return <FolderPlus className="w-4 h-4 text-green-600" />;
      case 'JOINED_REPOSITORY': return <UserPlus className="w-4 h-4 text-indigo-500" />;
      case 'INVITED_MEMBER': return <UserPlus className="w-4 h-4 text-purple-500" />;
      case 'LEFT_REPOSITORY': return <LogOut className="w-4 h-4 text-red-500" />;
      case 'CREATED_REQUEST': return <Activity className="w-4 h-4 text-yellow-500" />;
      case 'FULFILLED_REQUEST': return <CheckCircle className="w-4 h-4 text-teal-500" />;
      case 'CLOSED_REQUEST': return <XCircle className="w-4 h-4 text-gray-500" />;
      case 'DELETED_MATERIAL': return <Trash2 className="w-4 h-4 text-red-500" />;
      default: return <Activity className="w-4 h-4 text-gray-500" />;
    }
  };

  const totalPages = Math.max(1, Math.ceil(activities.length / pageSize));
  const visibleActivities = (!fullPage && !repositoryId) ? activities.slice(0, limit) : activities.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const Pagination = () => {
    if (!fullPage && !repositoryId) {
      return (
        <div className="flex justify-center items-center mt-4 pt-4 border-t border-gray-100">
          <a
            href="/activities"
            className="text-sm font-medium text-green-600 hover:text-green-700 cursor-pointer"
          >
            View More Activity
          </a>
        </div>
      );
    }
    
    return totalPages > 1 ? (
      <div className="flex justify-center items-center gap-4 mt-6 pt-4 border-t border-gray-100">
        <button
          type="button"
          onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
          disabled={currentPage === 1}
          className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
          Previous
        </button>
        <span className="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
        <button
          type="button"
          onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
          disabled={currentPage === totalPages}
          className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
        >
          Next
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
        </button>
      </div>
    ) : null;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[200px]">
        <LoadingScreen label="Loading activity" fullScreen={false} />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="bg-white rounded-lg border border-gray-200 p-6 text-center">
        <Activity className="mx-auto h-10 w-10 text-gray-300" />
        <h3 className="mt-2 text-sm font-medium text-gray-900">Activity unavailable</h3>
        <p className="mt-1 text-sm text-gray-500">{loadError}</p>
      </div>
    );
  }

  if (activities.length === 0) {
    return (
      <div className="bg-white rounded-lg border border-gray-200 p-6 text-center">
        <Activity className="mx-auto h-12 w-12 text-gray-300" />
        <h3 className="mt-2 text-sm font-medium text-gray-900">No activity yet</h3>
        <p className="mt-1 text-sm text-gray-500">{repositoryId ? 'Repository actions will appear here.' : 'Your actions will appear here.'}</p>
      </div>
    );
  }

  return (
    <section>
      {!hideHeader && (repositoryId ? (
        repositoryId === 'all' && !loading && (
          <div className="mb-4">
            <h2 className="text-2xl font-bold text-gray-900 leading-tight">Global Activity</h2>
            <p className="mt-1 text-sm text-gray-500">Monitor all activities across the platform.</p>
          </div>
        )
      ) : (
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center">
            <Activity className="w-5 h-5 mr-2 text-indigo-600" />
            Your Activity
          </h3>
        </div>
      ))}
      <div className="bg-white rounded-lg border border-gray-200 p-5">
        <div className={`overflow-y-auto pr-2 ${fullPage || repositoryId === 'all' ? 'max-h-[calc(100vh-250px)]' : repositoryId ? 'max-h-[calc(100vh-310px)]' : 'max-h-[480px]'}`}>
          <ul>
            {visibleActivities.map((activity, activityIdx) => (
              <li key={activity.id}>
                <div className={`relative ${activityIdx !== visibleActivities.length - 1 ? 'pb-8' : ''}`}>
                  {activityIdx !== visibleActivities.length - 1 ? (
                    <span
                      className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200"
                      aria-hidden="true"
                    />
                  ) : null}
                  <div className="relative flex space-x-3">
                    <div>
                      <span className="h-8 w-8 rounded-full bg-gray-100 flex items-center justify-center ring-8 ring-white">
                        {getActionIcon(activity.action)}
                      </span>
                    </div>
                    <div className="flex min-w-0 flex-1 justify-between space-x-4 pt-1.5">
                      <div className="flex-1 min-w-0 pr-4">
                        <p className="text-sm text-gray-700 break-words whitespace-normal line-clamp-3">
                          {formatActivityMessage(activity, {
                            includeActor: true,
                            selfLabel: repositoryId ? 'Someone' : 'You',
                          })}
                        </p>
                      </div>
                      <div className="whitespace-nowrap text-right text-xs text-gray-500 font-medium">
                        {formatDate(activity.createdAt)}
                      </div>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
          <Pagination />
        </div>
      </div>
    </section>
  );
}
