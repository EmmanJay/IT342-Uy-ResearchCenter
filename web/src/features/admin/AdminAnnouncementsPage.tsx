import { useState, useEffect } from 'react';
import { adminApi } from './api/adminApi';
import { repositoryExtraApi } from '../repository/api/repositoryExtraApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { Trash2, Megaphone, MessageSquare, X } from 'lucide-react';
import AdminTableControls from './components/AdminTableControls';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function AdminAnnouncementsPage() {
  const [repositories, setRepositories] = useState<any[]>([]);
  const [selectedRepoId, setSelectedRepoId] = useState<string>('');
  const [search, setSearch] = useState('');
  const [updates, setUpdates] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('NEWEST');
  const [updatesLoading, setUpdatesLoading] = useState(false);
  const [expandedUpdate, setExpandedUpdate] = useState<any | null>(null);

  // Pagination for Updates
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 10;

  useEffect(() => {
    fetchRepositories();
  }, []);

  const fetchRepositories = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getRepositories();
      if (res.success) {
        setRepositories(res.data.sort((a: any, b: any) => b.id - a.id));
      }
    } catch (err) {
      console.error('Failed to load repositories');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedRepoId) {
      fetchUpdates(selectedRepoId);
    } else {
      setUpdates([]);
    }
    setCurrentPage(1);
  }, [selectedRepoId]);

  const fetchUpdates = async (repoId: string) => {
    try {
      setUpdatesLoading(true);
      const res = await repositoryExtraApi.getUpdates(repoId, 0, 100);
      if (res.success) {
        setUpdates(res.data);
      }
    } catch (err) {
      console.error('Failed to load updates', err);
    } finally {
      setUpdatesLoading(false);
    }
  };

  const handleDeleteUpdate = async (updateId: string) => {
    if (!selectedRepoId) return;
    if (!window.confirm('Are you sure you want to delete this announcement?')) return;
    
    try {
      const res = await repositoryExtraApi.deleteUpdate(selectedRepoId, updateId);
      if (res.success) {
        setUpdates(updates.filter(u => String(u.id) !== String(updateId)));
        if (expandedUpdate?.id === updateId) {
          setExpandedUpdate(null);
        }
      }
    } catch (err) {
      console.error('Failed to delete announcement', err);
      alert('Failed to delete announcement. You may not have sufficient permissions.');
    }
  };

  const filteredUpdates = updates.filter(u => 
    !search || 
    u.content?.toLowerCase().includes(search.toLowerCase()) ||
    u.authorName?.toLowerCase().includes(search.toLowerCase())
  );

  const sortedUpdates = [...filteredUpdates].sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    return sortOrder === 'NEWEST' ? dateB - dateA : dateA - dateB;
  });
  const totalPages = Math.max(1, Math.ceil(sortedUpdates.length / PAGE_SIZE));
  const paginatedUpdates = sortedUpdates.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  if (loading) {
    return <LoadingScreen label="Loading repositories..." fullScreen={false} />;
  }

  const truncateMessage = (msg: string, limit = 60) => {
    if (!msg) return '';
    return msg.length > limit ? msg.substring(0, limit) + '...' : msg;
  };

  return (
    <div className="mx-auto max-w-7xl">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-green-100 text-green-700 rounded-lg">
            <MessageSquare className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Messages</h1>
        </div>
        <div className="text-sm font-medium text-gray-500">
          Total Messages: {filteredUpdates.length}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-6">
        <AdminTableControls
          search={search}
          onSearchChange={setSearch}
          filterLabel="Repository"
          filterValue={selectedRepoId}
          onFilterChange={setSelectedRepoId}
          filterOptions={[
            { label: '-- Select a repository --', value: '' },
            ...repositories.map((repo) => ({
              label: `${repo.id} - ${repo.name} (${repo.owner})`,
              value: String(repo.id),
            })),
          ]}
          sortValue={sortOrder}
          onSortChange={setSortOrder}
          sortOptions={[
            { label: 'Newest First', value: 'NEWEST' },
            { label: 'Oldest First', value: 'OLDEST' },
          ]}
          resultCount={paginatedUpdates.length}
          totalCount={sortedUpdates.length}
        />

        <div className="min-h-[400px]">
          {!selectedRepoId ? (
            <div className="flex justify-center items-center py-12 text-gray-500">
              Please select a repository to view its messages.
            </div>
          ) : updatesLoading ? (
            <div className="flex justify-center items-center py-12">
              <LoadingScreen label="Loading messages..." fullScreen={false} />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Author
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Message
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Date Posted
                    </th>
                    <th scope="col" className="relative px-6 py-3">
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {paginatedUpdates.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-8 text-center text-sm text-gray-500">
                        No messages found in this repository.
                      </td>
                    </tr>
                  ) : (
                    paginatedUpdates.map((update: any) => (
                      <tr key={update.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-gray-900">{update.authorName}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm text-gray-600 max-w-md">
                            {truncateMessage(update.content)}
                            {update.content && update.content.length > 60 && (
                              <button
                                onClick={() => setExpandedUpdate(update)}
                                className="ml-2 text-green-600 hover:text-green-800 font-medium text-xs cursor-pointer"
                              >
                                Read More
                              </button>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-500">
                            {new Date(update.createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <button
                            onClick={() => handleDeleteUpdate(update.id)}
                            className="text-red-600 hover:text-red-900 p-1 rounded hover:bg-red-50 cursor-pointer"
                            title="Delete message"
                          >
                            <Trash2 className="h-4 w-4 inline" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 rounded-b-xl shadow-sm mt-4">
                  <div className="flex flex-1 justify-between sm:hidden">
                    <button
                      onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage === 1}
                      className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                    >
                      Previous
                    </button>
                    <button
                      onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage === totalPages}
                      className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                    >
                      Next
                    </button>
                  </div>
                  <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                    <div>
                      <p className="text-sm text-gray-700">
                        Showing <span className="font-medium">{(currentPage - 1) * PAGE_SIZE + 1}</span> to{' '}
                        <span className="font-medium">
                          {Math.min(currentPage * PAGE_SIZE, sortedUpdates.length)}
                        </span>{' '}
                        of <span className="font-medium">{sortedUpdates.length}</span> results
                      </p>
                    </div>
                    <div>
                      <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
                        <button
                          onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                          disabled={currentPage === 1}
                          className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                        >
                          <span className="sr-only">Previous</span>
                          <ChevronLeft className="h-5 w-5" aria-hidden="true" />
                        </button>
                        <button
                          onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                          disabled={currentPage === totalPages}
                          className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                        >
                          <span className="sr-only">Next</span>
                          <ChevronRight className="h-5 w-5" aria-hidden="true" />
                        </button>
                      </nav>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Expanded Message Modal */}
      {expandedUpdate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[80vh]">
            <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gray-50">
              <div>
                <h3 className="text-lg font-bold text-gray-900">Message Details</h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  By {expandedUpdate.authorName} on {new Date(expandedUpdate.createdAt).toLocaleString()}
                </p>
              </div>
              <button
                onClick={() => setExpandedUpdate(null)}
                className="p-2 text-gray-400 hover:bg-gray-200 hover:text-gray-600 rounded-full transition-colors cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="p-6 overflow-y-auto">
              <p className="text-gray-800 whitespace-pre-wrap text-sm leading-relaxed">
                {expandedUpdate.content}
              </p>
            </div>
            <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end gap-3">
              <button
                onClick={() => handleDeleteUpdate(expandedUpdate.id)}
                className="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 rounded-md hover:bg-red-100 transition-colors cursor-pointer flex items-center gap-2"
              >
                <Trash2 className="h-4 w-4" />
                Delete Message
              </button>
              <button
                onClick={() => setExpandedUpdate(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
