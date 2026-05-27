import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { ClipboardList, Trash2 } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';

const PAGE_SIZE = 10;

export default function AdminRequestsPage() {
  const [requests, setRequests] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [requestToDelete, setRequestToDelete] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getRequests();
      if (res.success) {
        setRequests(res.data);
      }
    } catch (err) {
      setError('Failed to load requests');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = (id: number) => {
    setRequestToDelete(id);
    setShowConfirm(true);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [search, statusFilter]);

  const filteredRequests = useMemo(() => {
    const query = search.trim().toLowerCase();
    return requests.filter((r) => {
      const status = String(r.status || '').toUpperCase();
      const searchable = `${r.id} ${r.title || ''} ${r.repositoryName || ''} ${r.repositoryId || ''} ${r.requesterName || ''} ${r.userId || ''} ${r.reason || ''} ${status}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesStatus = statusFilter === 'ALL' || status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [requests, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRequests.length / PAGE_SIZE));
  const paginatedRequests = filteredRequests.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const confirmDelete = async () => {
    if (!requestToDelete) return;
    try {
      await adminApi.deleteRequest(requestToDelete);
      setRequests(requests.filter((r) => r.id !== requestToDelete));
    } catch (err) {
      setError('Failed to delete request');
    } finally {
      setShowConfirm(false);
      setRequestToDelete(null);
    }
  };

  if (loading) {
    return <LoadingScreen label="Loading requests" fullScreen={false} />;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>;
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-orange-100 text-orange-600 rounded-lg">
            <ClipboardList className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Requests</h1>
        </div>
        <div className="text-sm text-gray-500">
          Total Requests: {requests.length}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <AdminTableControls
          search={search}
          onSearchChange={setSearch}
          filterLabel="Filter requests"
          filterValue={statusFilter}
          onFilterChange={setStatusFilter}
          filterOptions={[
            { label: 'All requests', value: 'ALL' },
            { label: 'Open', value: 'OPEN' },
            { label: 'Fulfilled', value: 'FULFILLED' },
            { label: 'Closed', value: 'CLOSED' },
            { label: 'Cancelled', value: 'CANCELLED' },
          ]}
          resultCount={filteredRequests.length}
          totalCount={requests.length}
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
                <th className="px-6 py-4">Title</th>
                <th className="px-6 py-4">Repository</th>
                <th className="px-6 py-4">Requester</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Reason</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedRequests.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 font-medium text-gray-900">{r.id}</td>
                  <td className="px-6 py-4 font-medium text-gray-900">{r.title}</td>
                  <td className="px-6 py-4">{r.repositoryName || r.repositoryId}</td>
                  <td className="px-6 py-4">{r.requesterName || r.userId}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${r.status === 'FULFILLED' ? 'bg-green-100 text-green-800' : 
                        r.status === 'CANCELLED' || r.status === 'CLOSED' ? 'bg-red-100 text-red-800' : 
                        'bg-yellow-100 text-yellow-800'}`}>
                      {r.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 max-w-xs truncate" title={r.reason}>{r.reason}</td>
                  <td className="px-6 py-4 text-right flex justify-end gap-2">
                    <button
                      onClick={() => handleDeleteClick(r.id)}
                      className="text-red-600 hover:text-red-900 transition-colors p-2 rounded-full hover:bg-red-50"
                      title="Delete Request"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredRequests.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-gray-500">
                    No requests found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        title="Delete Request"
        message="Are you sure you want to delete this request? This action cannot be undone."
        confirmText="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
}
