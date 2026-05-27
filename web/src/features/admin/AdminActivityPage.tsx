import { ActivityFeed } from './../activity/ActivityFeed';
import { Activity } from 'lucide-react';

export default function AdminActivityPage() {
  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <div className="flex items-start gap-3">
        <div className="p-2 bg-green-100 text-green-600 rounded-lg">
          <Activity className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Global Activity</h1>
          <p className="mt-1 text-sm text-gray-500">Monitor all activities across the platform.</p>
        </div>
      </div>
      <ActivityFeed repositoryId="all" hideHeader />
    </div>
  );
}
