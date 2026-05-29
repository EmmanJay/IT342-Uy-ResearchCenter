import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { ClipboardList, Trash2, X } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';

const PAGE_SIZE = 10;

export default function AdminRequestsPage() {
  const [requests, setRequests] = useState<any[]>([]);
  const [materials, setMaterials] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [requestToDelete, setRequestToDelete] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('LATEST');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedRequest, setSelectedRequest] = useState<any>(null);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const [reqRes, matRes] = await Promise.all([
        adminApi.getRequests(),
        adminApi.getMaterials()
      ]);
      
      if (reqRes.success) {
        setRequests(reqRes.data);
      }
      if (matRes.success) {
        setMaterials(matRes.data);
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
    const filtered = requests.filter((r) => {
      const status = String(r.status || '').toUpperCase();
      const searchable = `${r.id} ${r.title || ''} ${r.repositoryName || ''} ${r.repositoryId || ''} ${r.requesterName || ''} ${r.userId || ''} ${r.reason || ''} ${status}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesStatus = statusFilter === 'ALL' || status === statusFilter;
      return matchesSearch && matchesStatus;
    });

    if (sortOrder === 'LATEST') {
      return filtered.sort((a, b) => b.id - a.id);
    } else {
      return filtered.sort((a, b) => a.id - b.id);
    }
  }, [requests, search, statusFilter, sortOrder]);

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
          ]}
          sortValue={sortOrder}
          onSortChange={setSortOrder}
          sortOptions={[
            { label: 'Latest First', value: 'LATEST' },
            { label: 'Oldest First', value: 'OLDEST' },
          ]}
          resultCount={filteredRequests.length}
          totalCount={requests.length}
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
                <tr key={r.id} className="hover:bg-gray-50 transition-colors cursor-pointer" onClick={() => setSelectedRequest(r)}>
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
                      onClick={(e) => { e.stopPropagation(); handleDeleteClick(r.id); }}
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
        {totalPages > 1 && (
          <div className="flex justify-center items-center gap-4 py-4 border-t border-gray-200">
            <button
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage === 1}
              className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
              Previous
            </button>
            <span className="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
            <button
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage === totalPages}
              className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
            >
              Next
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
            </button>
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        title="Delete Request"
        message="Are you sure you want to delete this request? This action cannot be undone."
        confirmText="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setShowConfirm(false)}
      />

      {/* Read-Only Modal for Request */}
      {selectedRequest && (
        <div className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl p-6 relative max-h-[90vh] flex flex-col overflow-hidden">
            <div className="flex justify-between items-start mb-4 pr-6 relative">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{selectedRequest.title}</h2>
                <p className="text-sm text-gray-500 mt-1">Requested by {selectedRequest.requesterName || 'Unknown'} • {new Date(selectedRequest.createdAt).toLocaleDateString()}</p>
              </div>
              <div className="absolute top-0 right-0 flex items-center gap-3">
                <button onClick={() => setSelectedRequest(null)} className="text-gray-400 hover:text-gray-900 cursor-pointer p-1 text-xl leading-none">✕</button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto pr-2 space-y-6">
              <div className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg">
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${selectedRequest.status === 'OPEN' ? 'bg-blue-100 text-blue-700 border border-blue-200' : selectedRequest.status === 'FULFILLED' ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-gray-100 text-gray-700 border border-gray-200'}`}>
                  {selectedRequest.status === 'OPEN' ? 'Open' : selectedRequest.status === 'FULFILLED' ? 'Fulfilled' : selectedRequest.status}
                </span>
                {selectedRequest.status === 'FULFILLED' && selectedRequest.fulfilledByName && (
                  (() => {
                    const isEdited = selectedRequest.fulfilledAt && selectedRequest.updatedAt && new Date(selectedRequest.updatedAt).getTime() > new Date(selectedRequest.fulfilledAt).getTime() + 1000;
                    const timestamp = isEdited ? selectedRequest.updatedAt : (selectedRequest.fulfilledAt || selectedRequest.updatedAt);
                    const formattedTime = new Date(timestamp as string).toLocaleDateString('en-US', { 
                      month: 'short', 
                      day: 'numeric', 
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    });
                    return (
                      <span className="text-sm text-gray-600 font-medium">
                        {isEdited ? 'Edited' : 'Fulfilled'} by {selectedRequest.fulfilledByName} • {formattedTime}
                      </span>
                    );
                  })()
                )}
              </div>

              {(selectedRequest.description || selectedRequest.reason) && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-2">Description</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">{selectedRequest.description || selectedRequest.reason}</p>
                  </div>
                </div>
              )}

              {selectedRequest.status === 'FULFILLED' && (
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <h3 className="text-sm font-semibold text-gray-900">Attached Material</h3>
                  </div>
                  {selectedRequest.materialId ? (
                    materials.find(m => m.id === selectedRequest.materialId) ? (
                      <div className="bg-white p-3 border border-emerald-100 rounded-lg flex justify-between items-center cursor-default">
                        <span className="font-medium text-green-700 truncate">{materials.find(m => m.id === selectedRequest.materialId)?.title || selectedRequest.materialTitle}</span>
                        <span className="text-xs text-gray-500 ml-2 flex-shrink-0">{materials.find(m => m.id === selectedRequest.materialId)?.materialType || 'MATERIAL'}</span>
                      </div>
                    ) : (
                      <div className="bg-white p-3 border border-emerald-100 rounded-lg flex justify-between items-center cursor-default">
                        <span className="font-medium text-green-700 truncate">{selectedRequest.materialTitle || 'Attached Material'}</span>
                        <span className="text-xs text-gray-500 ml-2 flex-shrink-0">MATERIAL</span>
                      </div>
                    )
                  ) : (
                    <div className="bg-red-50 p-3 border border-red-100 rounded-lg flex justify-between items-center text-red-700 text-sm font-medium">
                      Material Deleted
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
