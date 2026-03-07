import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import RepositoryCard from '../components/RepositoryCard';

interface Repo {
  id: number;
  name: string;
  description: string;
  memberCount: number;
  materialCount: number;
  lastActivity: string;
}

// Placeholder data until backend repositories API is built
const sampleOwned: Repo[] = [
  { id: 1, name: 'Machine Learning Research', description: 'A comprehensive collection of ML papers and resources', memberCount: 5, materialCount: 24, lastActivity: '2 days ago' },
  { id: 2, name: 'Quantum Computing', description: 'Quantum algorithms and implementations', memberCount: 3, materialCount: 12, lastActivity: '1 week ago' },
];

const sampleMember: Repo[] = [
  { id: 3, name: 'Blockchain Research', description: 'Distributed systems and cryptocurrency studies', memberCount: 8, materialCount: 31, lastActivity: '3 days ago' },
  { id: 4, name: 'Climate Science Data', description: 'Environmental and climate research materials', memberCount: 12, materialCount: 47, lastActivity: 'Today' },
];

const DashboardPage = () => {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [ownedRepos] = useState<Repo[]>(sampleOwned);
  const [memberRepos] = useState<Repo[]>(sampleMember);

  useEffect(() => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      setFirstName(user.firstname || 'User');
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-[#F5F5F5]">
      {/* Navbar */}
      <nav className="bg-white shadow-sm h-14 flex items-center px-6 gap-4">
        <span className="text-lg font-semibold text-[#2E7D32] mr-auto">
          ResearchCenter
        </span>
        <input
          type="text"
          placeholder="Search repositories..."
          className="px-3 py-2 rounded-md border border-[#E0E0E0] text-sm w-64 focus:outline-none focus:ring-2 focus:ring-[#2E7D32] placeholder-[#9E9E9E]"
        />
        <button aria-label="Notifications" className="text-[#757575] hover:text-[#212121] p-1">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
        </button>
        <button aria-label="Profile" className="text-[#757575] hover:text-[#212121] p-1">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/></svg>
        </button>
        <button
          onClick={handleLogout}
          className="text-sm text-[#D32F2F] hover:underline font-medium ml-2"
        >
          Logout
        </button>
      </nav>

      <main className="max-w-6xl mx-auto px-6 py-8">
        {/* Welcome */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#212121]">
            Welcome back, {firstName}!
          </h1>
          <p className="text-sm text-[#757575] mt-1">
            Manage your research repositories and collaborate with your team
          </p>
        </div>

        {/* Create Button */}
        <button className="bg-[#2E7D32] hover:bg-[#1B5E20] text-white px-4 py-2 rounded-md text-sm font-semibold mb-8 focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:ring-offset-2 transition-colors duration-150">
          + Create New Repository
        </button>

        {/* Your Repositories */}
        <section className="mb-8">
          <h2 className="text-lg font-semibold text-[#212121] mb-4">
            Your Repositories
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {ownedRepos.map((repo) => (
              <RepositoryCard key={repo.id} repo={repo} isOwner={true} />
            ))}
          </div>
        </section>

        {/* Repos You're In */}
        <section>
          <h2 className="text-lg font-semibold text-[#212121] mb-4">
            Repositories You're In
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {memberRepos.map((repo) => (
              <RepositoryCard key={repo.id} repo={repo} isOwner={false} />
            ))}
          </div>
        </section>
      </main>
    </div>
  );
};

export default DashboardPage;
