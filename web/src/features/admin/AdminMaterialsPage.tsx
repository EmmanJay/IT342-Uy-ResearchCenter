import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { FileText, Trash2, Eye } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';

const PAGE_SIZE = 10;

export default function AdminMaterialsPage() {
  const [materials, setMaterials] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    fetchMaterials();
  }, []);

  const fetchMaterials = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getMaterials();
      if (res.success) {
        setMaterials(res.data);
      }
    } catch (err) {
      setError('Failed to load materials');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = (id: number) => {
    setItemToDelete(id);
    setShowConfirm(true);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [search, typeFilter]);

  const filteredMaterials = useMemo(() => {
    const query = search.trim().toLowerCase();
    return materials.filter((m) => {
      const materialType = String(m.materialType || m.fileType || 'UNKNOWN').toUpperCase();
      const searchable = `${m.id} ${m.title || ''} ${m.repositoryId || ''} ${m.repositoryName || ''} ${m.uploaderName || ''} ${materialType} ${m.status || ''}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesType = typeFilter === 'ALL' || materialType === typeFilter;
      return matchesSearch && matchesType;
    });
  }, [materials, search, typeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredMaterials.length / PAGE_SIZE));
  const paginatedMaterials = filteredMaterials.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const confirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      await adminApi.deleteMaterial(itemToDelete);
      setMaterials(materials.filter((m) => m.id !== itemToDelete));
    } catch (err) {
      setError('Failed to delete material');
    } finally {
      setShowConfirm(false);
      setItemToDelete(null);
    }
  };

  if (loading) {
    return <LoadingScreen label="Loading materials" fullScreen={false} />;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>;
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-purple-100 text-purple-600 rounded-lg">
            <FileText className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Materials</h1>
        </div>
        <div className="text-sm text-gray-500">
          Total Materials: {materials.length}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <AdminTableControls
          search={search}
          onSearchChange={setSearch}
          filterLabel="Filter materials"
          filterValue={typeFilter}
          onFilterChange={setTypeFilter}
          filterOptions={[
            { label: 'All materials', value: 'ALL' },
            { label: 'PDF', value: 'PDF' },
            { label: 'Link', value: 'LINK' },
            { label: 'Reference', value: 'REFERENCE' },
            { label: 'Unknown', value: 'UNKNOWN' },
          ]}
          resultCount={filteredMaterials.length}
          totalCount={materials.length}
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
                <th className="px-6 py-4">Repository ID</th>
                <th className="px-6 py-4">Type</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedMaterials.map((m) => (
                <tr key={m.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 font-medium text-gray-900">{m.id}</td>
                  <td className="px-6 py-4">{m.title}</td>
                  <td className="px-6 py-4">{m.repositoryId}</td>
                  <td className="px-6 py-4">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      {m.fileType || m.materialType || 'UNKNOWN'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right flex justify-end gap-2">
                    <a
                      href={m.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-900 transition-colors p-2 rounded-full hover:bg-blue-50"
                      title="View Material"
                    >
                      <Eye className="w-4 h-4" />
                    </a>
                    <button
                      onClick={() => handleDeleteClick(m.id)}
                      className="text-red-600 hover:text-red-900 transition-colors p-2 rounded-full hover:bg-red-50"
                      title="Delete Material"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredMaterials.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                    No materials found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        title="Delete Material"
        message="Are you sure you want to delete this material? This action cannot be undone."
        confirmText="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
}
