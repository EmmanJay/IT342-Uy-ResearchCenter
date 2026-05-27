import { useEffect, useState } from 'react';
import Navbar from '../../shared/components/Navbar';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { Bookmark, Search } from 'lucide-react';
import materialApi from '../material/api/materialApi';
import type { Material } from '../../shared/types';
import { useNavigate } from 'react-router-dom';

const PAGE_SIZE = 10;

export default function BookmarksPage() {
  const [materials, setMaterials] = useState<Material[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchBookmarks = async () => {
      try {
        const data = await materialApi.getBookmarked();
        setMaterials(data);
      } catch {
        setError('Failed to load bookmarked materials');
      } finally {
        setLoading(false);
      }
    };
    fetchBookmarks();
  }, []);

  const filteredMaterials = materials.filter(m => 
    m.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
    m.materialType?.toLowerCase().includes(searchQuery.toLowerCase())
  );
  const totalPages = Math.max(1, Math.ceil(filteredMaterials.length / PAGE_SIZE));
  const paginatedMaterials = filteredMaterials.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      
      <main className="max-w-6xl mx-auto px-6 py-8 w-full flex-1 flex flex-col gap-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Bookmark className="w-6 h-6 text-green-600" />
            My Bookmarks
          </h1>
          <p className="text-sm text-gray-600 mt-1">Quickly access materials you've saved for later.</p>
        </div>

        {error && <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{error}</div>}

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex-1 flex flex-col">
          <div className="mb-6 relative w-full md:w-96">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
            <input 
              type="text" 
              placeholder="Search bookmarks..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
            />
          </div>

          {loading ? (
            <div className="flex-1 flex justify-center py-12">
              <LoadingScreen label="Loading bookmarks" fullScreen={false} />
            </div>
          ) : filteredMaterials.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center py-12 text-gray-500">
              <Bookmark className="w-12 h-12 text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No bookmarks found</h3>
              <p className="text-sm max-w-sm">
                {searchQuery ? "We couldn't find any bookmarks matching your search." : "You haven't bookmarked any materials yet. Click the bookmark icon on any material to save it here."}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {paginatedMaterials.map((mat) => (
                <div key={mat.id} className="border border-gray-200 rounded-lg p-4 hover:border-green-300 hover:shadow-sm transition-all bg-gray-50 cursor-pointer" onClick={() => navigate(`/repositories/${mat.repositoryId}`)}>
                  <div className="flex justify-between items-start mb-2">
                    <h4 className="font-semibold text-gray-900 text-sm line-clamp-2 pr-2 leading-tight">{mat.title}</h4>
                    <Bookmark className="h-4 w-4 flex-shrink-0 text-green-700 fill-current" aria-label="Bookmarked material" />
                  </div>
                  <div className="text-xs text-gray-500 mb-3">{mat.materialType} • From Repository</div>
                  <p className="text-xs text-gray-600 line-clamp-2">{mat.description}</p>
                </div>
              ))}
            </div>
          )}

          {!loading && filteredMaterials.length > 0 && (
            <div className="flex justify-center items-center gap-4 pt-4 mt-6 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                disabled={currentPage === 1}
                className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-green-700 cursor-pointer"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
                Previous
              </button>
              <span className="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
              <button
                type="button"
                onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                disabled={currentPage === totalPages}
                className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-green-700 cursor-pointer"
              >
                Next
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
              </button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
