import Navbar from '../../shared/components/Navbar';
import { ActivityFeed } from './ActivityFeed';

export default function ActivitiesPage() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <main className="max-w-4xl mx-auto px-6 py-8 w-full flex-1 flex flex-col">
        <div className="h-full min-h-[500px]">
          <ActivityFeed fullPage={true} />
        </div>
      </main>
    </div>
  );
}