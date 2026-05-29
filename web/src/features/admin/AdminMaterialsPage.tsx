import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { FileText, Trash2, Eye, X } from 'lucide-react';
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
  const [sortOrder, setSortOrder] = useState('LATEST');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedMaterial, setSelectedMaterial] = useState<any>(null);
  const [isbnCopied, setIsbnCopied] = useState(false);

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
    const filtered = materials.filter((m) => {
      const materialType = String(m.materialType || m.fileType || 'UNKNOWN').toUpperCase();
      const searchable = `${m.id} ${m.title || ''} ${m.repositoryId || ''} ${m.repositoryName || ''} ${m.uploaderName || ''} ${materialType} ${m.status || ''}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesType = typeFilter === 'ALL' || materialType === typeFilter;
      return matchesSearch && matchesType;
    });

    if (sortOrder === 'LATEST') {
      return filtered.sort((a, b) => b.id - a.id);
    } else {
      return filtered.sort((a, b) => a.id - b.id);
    }
  }, [materials, search, typeFilter, sortOrder]);

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
          ]}
          sortValue={sortOrder}
          onSortChange={setSortOrder}
          sortOptions={[
            { label: 'Latest First', value: 'LATEST' },
            { label: 'Oldest First', value: 'OLDEST' },
          ]}
          resultCount={filteredMaterials.length}
          totalCount={materials.length}
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
                <tr key={m.id} className="hover:bg-gray-50 transition-colors cursor-pointer" onClick={() => setSelectedMaterial(m)}>
                  <td className="px-6 py-4 font-medium text-gray-900">{m.id}</td>
                  <td className="px-6 py-4">{m.title}</td>
                  <td className="px-6 py-4">{m.repositoryId}</td>
                  <td className="px-6 py-4">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      {m.fileType || m.materialType || 'UNKNOWN'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right flex justify-end gap-2">
                    <button
                      onClick={(e) => { e.stopPropagation(); handleDeleteClick(m.id); }}
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
        title="Delete Material"
        message="Are you sure you want to delete this material? This action cannot be undone."
        confirmText="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setShowConfirm(false)}
      />

      {/* Read-Only Modal for Material */}
      {selectedMaterial && (
        <div className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl p-6 relative max-h-[90vh] flex flex-col overflow-hidden">
            {/* Header */}
            <div className="flex justify-between items-start mb-4 pr-6">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{selectedMaterial.title}</h2>
                <p className="text-sm text-gray-500 mt-1">Uploaded by {selectedMaterial.uploaderName || 'Unknown'}</p>
              </div>
              <button onClick={() => setSelectedMaterial(null)} className="absolute top-6 right-6 text-gray-400 hover:text-gray-900 cursor-pointer p-1">
                ✕
              </button>
            </div>

            {/* Scrollable Content Area */}
            <div className="flex-1 overflow-y-auto pr-2 space-y-6">
              {/* Meta & Status Bar */}
              <div className="flex flex-wrap items-center gap-4 bg-gray-50 p-3 rounded-lg">
                <div className="flex-1 flex gap-4 text-sm text-gray-600 items-center">
                  <span className="font-medium flex items-center pt-1">
                    Type: <span className="font-normal ml-1">{selectedMaterial.materialType}</span>
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  {selectedMaterial.materialType === 'REFERENCE' && (selectedMaterial.isbn || selectedMaterial.metadata?.isbn) && (
                    <button 
                      title="Copy ISBN"
                      onClick={() => {
                        const isbnVal = selectedMaterial.isbn || selectedMaterial.metadata?.isbn;
                        navigator.clipboard.writeText(isbnVal);
                        setIsbnCopied(true);
                        setTimeout(() => setIsbnCopied(false), 2000);
                      }}
                      className="text-sm px-3 py-1.5 bg-green-50 text-green-700 rounded-md hover:bg-green-100 font-medium cursor-pointer flex items-center gap-1.5 transition-colors"
                    >
                      {isbnCopied ? (
                        <>
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                          Copied!
                        </>
                      ) : (
                        <>
                          ISBN: {selectedMaterial.isbn || selectedMaterial.metadata?.isbn}
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
                        </>
                      )}
                    </button>
                  )}
                  {selectedMaterial.fileUrl && (
                    <a href={selectedMaterial.fileUrl} target="_blank" rel="noreferrer" className="text-sm px-3 py-1 bg-green-50 text-green-700 rounded-md hover:bg-green-100 font-medium cursor-pointer">View PDF</a>
                  )}
                  {selectedMaterial.url && (
                    <a href={selectedMaterial.url} target="_blank" rel="noreferrer" className="text-sm px-3 py-1 bg-green-50 text-green-700 rounded-md hover:bg-green-100 font-medium cursor-pointer">Open Link</a>
                  )}
                </div>
              </div>

              {/* Description */}
              {selectedMaterial.description && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-2">Description</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">{selectedMaterial.description}</p>
                </div>
              )}

              {/* Tags */}
              {(selectedMaterial.tags || []).length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-2">Tags</h3>
                  <div className="flex gap-2 flex-wrap">
                    {Array.from(new Set(selectedMaterial.tags || [])).map((t: any) => (
                      <span key={t} className="text-xs font-medium rounded-full bg-emerald-50 text-emerald-700 px-3 py-1 border border-emerald-100">{t}</span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Sticky Footer */}
            <div className="mt-6 pt-4 border-t border-gray-100 flex flex-col sm:flex-row justify-between items-center gap-4">
              <div className="flex items-center gap-3 w-full sm:w-auto"></div>
              <p className="text-xs text-gray-400 font-medium">Added on {new Date(selectedMaterial.createdAt).toLocaleDateString()}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
