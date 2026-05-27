import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SessionManager } from '../../shared/auth/sessionManager';
import { repositoryApi } from '../repository/api/repositoryApi';
import type { Repository } from '../../shared/types';
import Navbar from '../../shared/components/Navbar';
import { ActivityFeed } from '../activity/ActivityFeed';
import RepositoryCard from './components/RepositoryCard';
import ConfirmModal from '../../shared/components/ConfirmModal';
import { useWebSocket } from '../../shared/context/WebSocketProvider';
import LoadingScreen from '../../shared/components/LoadingScreen';

let dashboardCache: Repository[] | null = null;

const getResponseStatus = (error: unknown) => {
  return (error as { response?: { status?: number } }).response?.status;
};

const DashboardPage = () => {
  const navigate = useNavigate();
  const user = SessionManager.getUser();
  const { subscribe, unsubscribe, lastMessage } = useWebSocket();
  const [repositories, setRepositories] = useState<Repository[]>(dashboardCache || []);
  const [loading, setLoading] = useState(!dashboardCache);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newRepoName, setNewRepoName] = useState('');
  const [newRepoDesc, setNewRepoDesc] = useState('');
  const [creating, setCreating] = useState(false);
  const [editingRepo, setEditingRepo] = useState<Repository | null>(null);
  const [editRepoName, setEditRepoName] = useState('');
  const [editRepoDesc, setEditRepoDesc] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);
  const [deleteRepo, setDeleteRepo] = useState<Repository | null>(null);

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      navigate('/admin/stats', { replace: true });
      return;
    }
    fetchRepositories();
  }, [user?.id, user?.role, navigate]);

  useEffect(() => {
    const repositoryIds = repositories.map((repo) => Number(repo.id)).filter((repoId) => !Number.isNaN(repoId));
    repositoryIds.forEach(id => subscribe(String(id), () => {}));
    return () => {
      repositoryIds.forEach(id => unsubscribe(String(id), () => {}));
    };
  }, [repositories, subscribe, unsubscribe]);

  useEffect(() => {
    if (!lastMessage || lastMessage.type === 'AUTH_SUCCESS') return;
    fetchRepositories();
  }, [lastMessage]);

  const fetchRepositories = async () => {
    try {
      if (!dashboardCache) setLoading(true);
      const repos = await repositoryApi.getAll();
      dashboardCache = repos;
      setRepositories(repos);
    } catch (error: unknown) {
      const status = getResponseStatus(error);
      if (status === 401 || status === 403) {
        return;
      }
      setError('Failed to load repositories. Please try again.');
      console.error(error);
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
    } catch (error: unknown) {
      const status = getResponseStatus(error);
      if (status === 401 || status === 403) {
        return;
      }
      setError('Failed to create repository. Please try again.');
      console.error(error);
    } finally {
      setCreating(false);
    }
  };

  const handleEditRepository = (repo: Repository) => {
    setEditingRepo(repo);
    setEditRepoName(repo.name);
    setEditRepoDesc(repo.description || '');
  };

  const handleSaveRepository = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingRepo) return;
    try {
      setSavingEdit(true);
      const updated = await repositoryApi.update(String(editingRepo.id), {
        name: editRepoName,
        description: editRepoDesc,
      });
      setRepositories(prev => prev.map(repo => String(repo.id) === String(editingRepo.id) ? { ...repo, ...updated } : repo));
      setEditingRepo(null);
    } catch (error: unknown) {
      const status = getResponseStatus(error);
      if (status === 401 || status === 403) return;
      setError('Failed to update repository. Please try again.');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDeleteRepository = async () => {
    if (!deleteRepo) return;
    try {
      await repositoryApi.delete(String(deleteRepo.id));
      setRepositories(prev => prev.filter(repo => String(repo.id) !== String(deleteRepo.id)));
      setDeleteRepo(null);
    } catch {
      setError('Failed to delete repository. Please try again.');
    }
  };

  const handleToggleFavorite = async (repoId: string | number) => {
    const previousRepos = [...repositories];
    const targetRepo = previousRepos.find(r => String(r.id) === String(repoId));
    if (!targetRepo) return;

    // Optimistic UI update
    setRepositories(prev => prev.map(repo => String(repo.id) === String(repoId) ? { ...repo, bookmarked: !repo.bookmarked, favorited: !repo.bookmarked } : repo));

    try {
      const bookmarked = await repositoryApi.toggleBookmark(String(repoId));
      // Re-sync with actual server state silently
      setRepositories(prev => prev.map(repo => String(repo.id) === String(repoId) ? { ...repo, bookmarked, favorited: bookmarked } : repo));
    } catch {
      // Revert on error
      setRepositories(previousRepos);
      setError('Failed to toggle favorite. Please try again.');
    }
  };

  /*
  const handleLogout = () => {
    SessionManager.clear();
    navigate('/login');
  };
  */

  const sortFavoritedFirst = (items: Repository[]) => [...items].sort((a, b) => {
    const aFavorited = Boolean(a.bookmarked || a.favorited);
    const bFavorited = Boolean(b.bookmarked || b.favorited);
    if (aFavorited !== bFavorited) return aFavorited ? -1 : 1;
    return new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime();
  });

  const ownedRepos = sortFavoritedFirst(repositories.filter((r) => r.ownerId === user?.id));
  const memberRepos = sortFavoritedFirst(repositories.filter((r) => r.ownerId !== user?.id));

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <main className="max-w-6xl mx-auto px-6 py-8">
        {/* Welcome */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Welcome back, {user?.firstname}!</h1>
          <p className="text-sm text-gray-600 mt-1">Manage your research repositories and collaborate with your team</p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-md text-sm font-semibold mb-8 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2 transition-colors cursor-pointer"
        >
          + Create New Research Repository
        </button>

        {error && <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{error}</div>}

        {loading && repositories.length === 0 ? (
          <div className="flex justify-center items-center h-[60vh] w-full">
            <LoadingScreen label="Loading dashboard" fullScreen={false} />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2">
              {/* Your Repositories */}
            {ownedRepos.length > 0 && (
              <section className="mb-8">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Your Research Repositories ({ownedRepos.length})</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {ownedRepos.map((repo) => (
                    <RepositoryCard
                      key={repo.id}
                      repo={{
                        id: repo.id,
                        name: repo.name,
                        description: repo.description,
                        memberCount: repo.memberCount || 0,
                        materialCount: repo.materialCount || 0,
                        favorited: repo.bookmarked,
                        lastActivity: repo.recentActivity || (repo.updatedAt ? `Updated ${new Date(repo.updatedAt).toLocaleDateString()}` : `Created ${new Date(repo.createdAt).toLocaleDateString()}`),
                      }}
                      isOwner={true}
                      onOpen={() => navigate(`/repositories/${repo.id}`)}
                      onEdit={() => handleEditRepository(repo)}
                      onDelete={() => setDeleteRepo(repo)}
                      onToggleFavorite={() => handleToggleFavorite(repo.id)}
                    />
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
                    <RepositoryCard
                      key={repo.id}
                      repo={{
                        id: repo.id,
                        name: repo.name,
                        description: repo.description,
                        memberCount: repo.memberCount || 0,
                        materialCount: repo.materialCount || 0,
                        favorited: repo.bookmarked,
                        lastActivity: repo.recentActivity || (repo.updatedAt ? `Updated ${new Date(repo.updatedAt).toLocaleDateString()}` : `Created ${new Date(repo.createdAt).toLocaleDateString()}`),
                      }}
                      isOwner={false}
                      onOpen={() => navigate(`/repositories/${repo.id}`)}
                      onEdit={() => handleEditRepository(repo)}
                      onDelete={() => setDeleteRepo(repo)}
                      onToggleFavorite={() => handleToggleFavorite(repo.id)}
                    />
                  ))}
                </div>
              </section>
            )}

            {repositories.length === 0 && !loading && (
              <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
                <p className="text-gray-500 mb-4">No research repositories yet. Create one to get started!</p>
              </div>
            )}
            </div>

            <div className="lg:col-span-1">
              <ActivityFeed />
            </div>
          </div>
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

      {editingRepo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 px-4" onClick={() => setEditingRepo(null)}>
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-lg font-bold text-gray-900 mb-4">Edit Repository</h2>
            <form className="space-y-4" onSubmit={handleSaveRepository}>
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-1">Name</label>
                <input
                  type="text"
                  value={editRepoName}
                  onChange={(e) => setEditRepoName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-1">Description</label>
                <textarea
                  value={editRepoDesc}
                  onChange={(e) => setEditRepoDesc(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600 h-24 resize-none"
                />
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setEditingRepo(null)} className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit" disabled={savingEdit} className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-60">{savingEdit ? 'Saving...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={Boolean(deleteRepo)}
        title="Delete Repository"
        message={`Delete ${deleteRepo?.name}? This removes the repository for everyone.`}
        confirmText="Delete"
        confirmColor="danger"
        onConfirm={handleDeleteRepository}
        onCancel={() => setDeleteRepo(null)}
      />
    </div>
  );
};

export default DashboardPage;
