import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { BookOpen, Trash2 } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';

const PAGE_SIZE = 10;

export default function AdminRepositoriesPage() {
  const [repositories, setRepositories] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [repoToDelete, setRepoToDelete] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [visibilityFilter, setVisibilityFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    fetchRepositories();
  }, []);

  const fetchRepositories = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getRepositories();
      if (res.success) {
        setRepositories(res.data);
      }
    } catch (err) {
      setError('Failed to load repositories');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = (id: number) => {
    setRepoToDelete(id);
    setShowConfirm(true);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [search, visibilityFilter]);

  const filteredRepositories = useMemo(() => {
    const query = search.trim().toLowerCase();
    return repositories.filter((r) => {
      const searchable = `${r.id} ${r.name || ''} ${r.description || ''} ${r.owner || ''}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesVisibility =
        visibilityFilter === 'ALL' ||
        (visibilityFilter === 'PUBLIC' && r.isPublic) ||
        (visibilityFilter === 'PRIVATE' && !r.isPublic);
      return matchesSearch && matchesVisibility;
    });
  }, [repositories, search, visibilityFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRepositories.length / PAGE_SIZE));
  const paginatedRepositories = filteredRepositories.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const confirmDelete = async () => {
    if (!repoToDelete) return;
    try {
      await adminApi.deleteRepository(repoToDelete);
      setRepositories(repositories.filter((r) => r.id !== repoToDelete));
    } catch (err) {
      setError('Failed to delete repository');
    } finally {
      setShowConfirm(false);
      setRepoToDelete(null);
    }
  };

  if (loading) {
    return <LoadingScreen label="Loading repositories" fullScreen={false} />;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>;
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-green-100 text-green-600 rounded-lg">
            <BookOpen className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Repositories</h1>
        </div>
        <div className="text-sm text-gray-500">
          Total Repositories: {repositories.length}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <AdminTableControls
          search={search}
          onSearchChange={setSearch}
          filterLabel="Filter repositories"
          filterValue={visibilityFilter}
          onFilterChange={setVisibilityFilter}
          filterOptions={[
            { label: 'All repositories', value: 'ALL' },
            { label: 'Public', value: 'PUBLIC' },
            { label: 'Private', value: 'PRIVATE' },
          ]}
          resultCount={filteredRepositories.length}
          totalCount={repositories.length}
          currentPage={Math.min(currentPage, totalPages)}
          totalPages={totalPages}
          onPreviousPage={() => setCurrentPage((page) => Math.max(1, page - 1))}
          onNextPage={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
        />
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 border-b border-gray-200 text-gray-700 uppercase font-medium">
              <tr>
                <th className="px-6 py-4">ID</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Description</th>
                <th className="px-6 py-4">Visibility</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedRepositories.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 font-medium text-gray-900">{r.id}</td>
                  <td className="px-6 py-4 font-medium text-gray-900">{r.name}</td>
                  <td className="px-6 py-4 max-w-xs truncate">{r.description}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${r.isPublic ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'}`}>
                      {r.isPublic ? 'Public' : 'Private'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleDeleteClick(r.id)}
                      className="text-red-600 hover:text-red-900 transition-colors p-2 rounded-full hover:bg-red-50"
                      title="Delete Repository"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredRepositories.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                    No repositories found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        title="Delete Repository"
        message="Are you sure you want to delete this repository? All materials inside it will also be deleted."
        confirmText="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
}
