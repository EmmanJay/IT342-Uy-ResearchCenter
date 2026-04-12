import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SessionManager } from '../auth/sessionManager';
import { repositoryApi } from '../api/repositoryApi';
import type { Repository } from '../types';
import Navbar from '../components/Navbar';

const DashboardPage = () => {
  const navigate = useNavigate();
  const user = SessionManager.getUser();
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newRepoName, setNewRepoName] = useState('');
  const [newRepoDesc, setNewRepoDesc] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    fetchRepositories();
  }, []);

  const fetchRepositories = async () => {
    try {
      setLoading(true);
      const repos = await repositoryApi.getAll();
      setRepositories(repos);
    } catch (err: any) {
      setError('Failed to load repositories. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRepository = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRepoName.trim()) return;

    try {
      setCreating(true);
      await repositoryApi.create({ name: newRepoName, description: newRepoDesc });
      setNewRepoName('');
      setNewRepoDesc('');
      setShowCreateModal(false);
      await fetchRepositories();
    } catch (err: any) {
      setError('Failed to create repository. Please try again.');
      console.error(err);
    } finally {
      setCreating(false);
    }
  };

  /*
  const handleLogout = () => {
    SessionManager.clear();
    navigate('/login');
  };
  */

  const ownedRepos = repositories.filter((r) => r.ownerId === user?.id);
  const memberRepos = repositories.filter((r) => r.ownerId !== user?.id);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <main className="max-w-6xl mx-auto px-6 py-8">
        {/* Welcome */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Welcome back, {user?.firstname}!</h1>
          <p className="text-sm text-gray-600 mt-1">Manage your research repositories and collaborate with your team</p>
        </div>

        {/* Create Button */}
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-md text-sm font-semibold mb-8 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2 transition-colors cursor-pointer"
        >
          + Create New Research Repository
        </button>

        {error && <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{error}</div>}

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <p className="text-gray-500">Loading repositories...</p>
          </div>
        ) : (
          <>
            {/* Your Repositories */}
            {ownedRepos.length > 0 && (
              <section className="mb-8">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Your Research Repositories ({ownedRepos.length})</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {ownedRepos.map((repo) => (
                    <div
                      key={repo.id}
                      onClick={() => navigate(`/repositories/${repo.id}`)}
                      className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="text-sm font-semibold text-gray-900">{repo.name}</h3>
                        <span className="bg-green-50 text-green-700 text-xs font-medium px-2 py-0.5 rounded">Owner</span>
                      </div>
                      <p className="text-xs text-gray-600 mb-4 line-clamp-2">{repo.description}</p>
                      <div className="border-t border-gray-200 pt-3 text-xs text-gray-600">
                        <p>Created {new Date(repo.createdAt).toLocaleDateString()}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {/* Repos You're In */}
            {memberRepos.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Invited Repositories ({memberRepos.length})</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {memberRepos.map((repo) => (
                    <div
                      key={repo.id}
                      onClick={() => navigate(`/repositories/${repo.id}`)}
                      className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="text-sm font-semibold text-gray-900">{repo.name}</h3>
                        <span className="bg-gray-100 text-gray-700 text-xs font-medium px-2 py-0.5 rounded border border-gray-200">Member</span>
                      </div>
                      <p className="text-xs text-gray-600 mb-4 line-clamp-2">{repo.description}</p>
                      <div className="border-t border-gray-200 pt-3 text-xs text-gray-600 flex justify-between items-center">
                        <p>Created {new Date(repo.createdAt).toLocaleDateString()}</p>
                        <p className="text-gray-400">By {repo.ownerName || 'Unknown'}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {repositories.length === 0 && (
              <div className="text-center py-12">
                <p className="text-gray-500 mb-4">No research repositories yet. Create one to get started!</p>
              </div>
            )}
          </>
        )}
      </main>

      {/* Create Repository Modal */}
      {showCreateModal && (
        <div 
          className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50 cursor-pointer"
          onClick={() => setShowCreateModal(false)}
        >
          <div 
            className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md cursor-pointer"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-lg font-bold text-gray-900 mb-4">Create New Research Repository</h2>
            <form onSubmit={handleCreateRepository} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-1">Name</label>
                <input
                  type="text"
                  value={newRepoName}
                  onChange={(e) => setNewRepoName(e.target.value)}
                  placeholder="e.g., ML Research"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-1">Description</label>
                <textarea
                  value={newRepoDesc}
                  onChange={(e) => setNewRepoDesc(e.target.value)}
                  placeholder="Describe your repository..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600 h-24 resize-none"
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-60 cursor-pointer"
                >
                  {creating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
