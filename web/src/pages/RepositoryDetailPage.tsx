import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { SessionManager } from '../auth/sessionManager';
import { repositoryApi } from '../api/repositoryApi';
import { materialApi } from '../api/materialApi';
import { requestApi } from '../api/requestApi';
import type { RepositoryDetail, MaterialRequest } from '../types';
import Navbar from '../components/Navbar';
import Breadcrumbs from '../components/Breadcrumbs';
// import MaterialForm from '../components/MaterialForm';

const RepositoryDetailPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  const user = SessionManager.getUser();

  const [repo, setRepo] = useState<RepositoryDetail | null>(null);
  const [materials, setMaterials] = useState<any[]>([]);
  const [requests, setRequests] = useState<MaterialRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'materials' | 'requests' | 'members'>(
    location.state?.activeTab || 'materials'
  );
  const [inviteEmail, setInviteEmail] = useState('');
  const [fulfillRequestId, setFulfillRequestId] = useState<string | null>(null);
  const [fulfillMaterialId, setFulfillMaterialId] = useState('');
  const [activeTags, setActiveTags] = useState<string[]>([]);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [selectedUploaders, setSelectedUploaders] = useState<string[]>([]);
  const [isTagDropdownOpen, setIsTagDropdownOpen] = useState(false);
  const [isFiltersDropdownOpen, setIsFiltersDropdownOpen] = useState(false);
  const [tagSearch, setTagSearch] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);
  const filtersDropdownRef = useRef<HTMLDivElement>(null);
  const [selectedMaterial, setSelectedMaterial] = useState<any | null>(null);
  const [deleteCandidateId, setDeleteCandidateId] = useState<string | null>(null);
  const [deleteRequestCandidateId, setDeleteRequestCandidateId] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [sortOrder, setSortOrder] = useState<'latest' | 'oldest'>('latest');

  // Request State
  const [currentPageReq, setCurrentPageReq] = useState(1);
  const [reqSearchQuery, setReqSearchQuery] = useState('');
  const [reqSortOrder, setReqSortOrder] = useState<'latest' | 'oldest'>('latest');
  const [selectedReqStatuses, setSelectedReqStatuses] = useState<string[]>([]);
  const [selectedRequesters, setSelectedRequesters] = useState<string[]>([]);
  const [isReqFiltersDropdownOpen, setIsReqFiltersDropdownOpen] = useState(false);
  const reqFiltersDropdownRef = useRef<HTMLDivElement>(null);
  const [selectedRequest, setSelectedRequest] = useState<MaterialRequest | null>(null);
  const [isEditingReqMaterial, setIsEditingReqMaterial] = useState(false);

  // Members Tab State
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const [searchResult, setSearchResult] = useState<any | null>(null);
  const [searching, setSearching] = useState(false);
  const [confirmRemoveId, setConfirmRemoveId] = useState<number | string | null>(null);
  const [currentPageMem, setCurrentPageMem] = useState(1);
  const inviteDropdownRef = useRef<HTMLDivElement>(null);

  const isOwner = repo?.ownerId === user?.id;

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsTagDropdownOpen(false);
      }
      if (filtersDropdownRef.current && !filtersDropdownRef.current.contains(event.target as Node)) {
        setIsFiltersDropdownOpen(false);
      }
      if (reqFiltersDropdownRef.current && !reqFiltersDropdownRef.current.contains(event.target as Node)) {
        setIsReqFiltersDropdownOpen(false);
      }
      if (inviteDropdownRef.current && !inviteDropdownRef.current.contains(event.target as Node)) {
        setSearchResult(null);
        setInviteEmail('');
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (inviteEmail.length >= 3) {
      const handler = setTimeout(async () => {
        setSearching(true);
        try {
          const resp = await fetch(`http://localhost:8080/api/v1/users/search?email=${encodeURIComponent(inviteEmail)}`, {
            headers: { Authorization: `Bearer ${SessionManager.getToken()}` }
          });
          
          if (!resp.ok) {
            console.error('Search failed with status:', resp.status);
            setSearchResult({ notFound: true });
            return;
          }
          
          const json = await resp.json();
          console.log('Search response:', json);
          
          if (json && json.data) {
            console.log('User found:', json.data);
            setSearchResult(json.data);
          } else {
            console.log('No user data in response');
            setSearchResult({ notFound: true });
          }
        } catch (e) {
          console.error('Search error:', e);
          setSearchResult({ notFound: true });
        } finally {
          setSearching(false);
        }
      }, 400);
      return () => clearTimeout(handler);
    } else {
      setSearchResult(null);
    }
  }, [inviteEmail]);

  useEffect(() => {
    if (selectedMaterial || deleteCandidateId || deleteRequestCandidateId || selectedRequest || fulfillRequestId || isEditingReqMaterial) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [selectedMaterial, deleteCandidateId, deleteRequestCandidateId, selectedRequest, fulfillRequestId, isEditingReqMaterial]);

  const fetchRepository = async () => {
    try {
      setLoading(true);
      const repoData = await repositoryApi.getById(id!);
      setRepo(repoData);
      const [matsData, reqsData] = await Promise.all([
        Promise.allSettled([repositoryApi.getMaterials(id!)]),
        Promise.allSettled([repositoryApi.getRequests(id!)]),
      ]);
      if (matsData[0].status === 'fulfilled') setMaterials(matsData[0].value || []);
      if (reqsData[0].status === 'fulfilled') setRequests(reqsData[0].value || []);
    } catch (err: any) {
      setError('Failed to load repository');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Check location state for returning from fulfill/edit process
  useEffect(() => {
    if (id) fetchRepository();
  }, [id]);

  // Keep active filters in sync with current materials: remove selections that no longer exist
  useEffect(() => {
    setCurrentPage(1); // Reset page on filter change
    const availableStatuses = new Set(materials.map(m => (m.myStatus || m.status)).filter(Boolean));
    const availableTypes = new Set(materials.map(m => m.materialType).filter(Boolean));
    const availableUploaders = new Set(materials.map(m => m.uploaderName || m.uploadedByName || 'Unknown').filter(Boolean));
    const availableTags = new Set(materials.flatMap(m => m.tags || []).filter(Boolean));

    setSelectedStatuses(prev => prev.filter(s => availableStatuses.has(s)));
    setSelectedTypes(prev => prev.filter(t => availableTypes.has(t)));
    setSelectedUploaders(prev => prev.filter(u => availableUploaders.has(u)));
    setActiveTags(prev => prev.filter(t => availableTags.has(t) || t === 'NO_TAGS'));
  }, [materials]);

  const handleDeleteMaterial = async (materialId: string) => {
    try {
      await materialApi.delete(materialId);
      setMaterials(materials.filter((m) => m.id !== materialId));
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || err?.message || 'Failed to delete material. Please try again.';
      throw new Error(errorMsg);
    }
  };

  const handleFulfillRequest = async (requestId: string, overrideMaterialId?: string) => {
    const materialIdToUse = overrideMaterialId || fulfillMaterialId;
    if (!materialIdToUse) return;
    try {
      if (isEditingReqMaterial) {
        // Use updateMaterial for already fulfilled requests
        await requestApi.updateMaterial(String(requestId), { materialId: String(materialIdToUse) });
      } else {
        // Use fulfill for OPEN requests
        await requestApi.fulfill(String(requestId), { materialId: String(materialIdToUse) });
      }
      await fetchRepository();
      // Clean up all edit/fulfill states
      setFulfillRequestId(null);
      setFulfillMaterialId('');
      setSelectedRequest(null);
      setIsEditingReqMaterial(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to update request');
    }
  };

  const filterCount = selectedStatuses.length 
    + selectedTypes.length 
    + selectedUploaders.length;

  const filteredMaterials = (materials || [])
    .filter(m => activeTags.length === 0 
      || (activeTags.includes('NO_TAGS') && (!m.tags || m.tags.length === 0))
      || (m.tags || []).some((t: any) => activeTags.includes(t)))
    .filter(m => selectedStatuses.length === 0
      || selectedStatuses.includes(m.status || m.myStatus))
    .filter(m => selectedTypes.length === 0
      || selectedTypes.includes(m.materialType))
    .filter(m => selectedUploaders.length === 0
      || selectedUploaders.includes(m.uploaderName || m.uploadedByName || 'Unknown'))
    .filter(m => searchQuery.trim() === ''
      || m.title.toLowerCase().includes(searchQuery.toLowerCase()))
    .sort((a, b) => sortOrder === 'latest'
      ? new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      : new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );

  const totalPages = Math.ceil(filteredMaterials.length / 10);
  const paginatedMaterials = filteredMaterials.slice((currentPage - 1) * 10, currentPage * 10);

  // Request filtering and pagination
  useEffect(() => {
    setCurrentPageReq(1);
    const availableStatuses = new Set(requests.map(r => r.status).filter(Boolean));
    const availableRequesters = new Set(requests.map(r => r.requesterName || 'Unknown').filter(Boolean));
    setSelectedReqStatuses(prev => prev.filter(s => availableStatuses.has(s as any)));
    setSelectedRequesters(prev => prev.filter(r => availableRequesters.has(r)));
  }, [requests]);

  const reqFilterCount = selectedReqStatuses.length + selectedRequesters.length;
  const filteredRequests = (requests || [])
    .filter(r => selectedReqStatuses.length === 0 || selectedReqStatuses.includes(r.status))
    .filter(r => selectedRequesters.length === 0 || selectedRequesters.includes(r.requesterName || 'Unknown'))
    .filter(r => reqSearchQuery.trim() === '' || r.title.toLowerCase().includes(reqSearchQuery.toLowerCase()))
    .sort((a, b) => {
      // Always sort Open ahead of Fulfilled
      if (a.status === 'OPEN' && b.status !== 'OPEN') return -1;
      if (a.status !== 'OPEN' && b.status === 'OPEN') return 1;
      // Then fallback to date sorting (latest vs oldest)
      return reqSortOrder === 'latest'
        ? new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        : new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    });

  const totalPagesReq = Math.ceil(filteredRequests.length / 10);
  const paginatedRequests = filteredRequests.slice((currentPageReq - 1) * 10, currentPageReq * 10);

  // Members pagination
  const sortedMembers = [...(repo?.members ?? [])].sort((a, b) => {
    if (a.userId === repo?.ownerId) return -1;
    if (b.userId === repo?.ownerId) return 1;
    return 0;
  });
  const totalPagesMem = Math.ceil(sortedMembers.length / 10);
  const paginatedMembers = sortedMembers.slice((currentPageMem - 1) * 10, currentPageMem * 10);

  const handleDeleteRequest = async (requestId: string) => {
    await requestApi.delete(String(requestId));
    setRequests(requests.filter((r) => r.id !== requestId));
    setSelectedRequest(null);
    setDeleteRequestCandidateId(null);
  };

  const handleInviteMember = async (email: string) => {
    if (!id || !email) return;
    try {
      if (repo?.members?.some(m => m.email === email)) {
        setToast({ msg: 'Already a member.', type: 'error' });
        return;
      }
      await repositoryApi.inviteMember(id, { email });
      setInviteEmail('');
      setSearchResult(null);
      await fetchRepository();
      setToast({ msg: 'Invite sent successfully!', type: 'success' });
    } catch (err: any) {
      setToast({ msg: err?.response?.data?.message || 'Failed to send invite.', type: 'error' });
    }
  };

  const handleRemoveMember = async (userId: string | number) => {
    if (!id) return;
    try {
      await repositoryApi.removeMember(id, String(userId));
      setRepo(prev => prev ? { ...prev, members: prev.members.filter(m => String(m.userId) !== String(userId)) } : prev);
      setConfirmRemoveId(null);
      setToast({ msg: 'Member removed.', type: 'success' });
    } catch (err: any) {
      setConfirmRemoveId(null);
      setToast({ msg: err?.response?.data?.message || 'Failed to remove member.', type: 'error' });
    }
  };

  if (loading) return <div className="min-h-screen flex items-center justify-center"><p className="text-gray-500">Loading...</p></div>;
  if (!repo) return <div className="min-h-screen flex items-center justify-center"><p className="text-gray-500">Repository not found</p></div>;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <div className="max-w-6xl mx-auto px-6 py-6 flex-1 w-full">
        <Breadcrumbs items={[
          { label: isOwner ? 'Your Repositories' : 'Invited Repositories', path: '/' },
          { label: repo.name, path: `/repositories/${repo.id}` },
          { label: activeTab.charAt(0).toUpperCase() + activeTab.slice(1) }
        ]} />
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{repo.name}</h1>
        <p className="text-gray-600 mb-6">{repo.description}</p>

        {error && <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{error}</div>}

        {/* Tabs */}
        <div className="flex gap-4 border-b border-gray-200 mb-6">
          {['Materials', 'Requests', 'Members'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab.toLowerCase() as any)}
              className={`px-4 py-2 text-sm font-medium ${
                activeTab === tab.toLowerCase()
                  ? 'border-b-2 border-green-600 text-green-600 cursor-pointer'
                  : 'text-gray-600 hover:text-gray-900 cursor-pointer'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Materials Tab */}
        {activeTab === 'materials' && (
          <div>
            <div className="sticky top-0 bg-gray-50 pt-2 pb-4 z-20 flex items-center justify-between gap-2 w-full">
              <button
                onClick={() => navigate(`/repositories/${repo.id}/materials/new`, { state: { repoName: repo.name, activeTab: 'materials', isOwner: isOwner } })}
                className="px-4 py-2 bg-green-600 text-white rounded-md text-sm hover:bg-green-700 flex-shrink-0 cursor-pointer"
              >
                + Add Material
              </button>

              <div className="relative flex-1">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
                  </svg>
                </div>
                <input
                  type="text"
                  placeholder="Search materials..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                />
              </div>

              <div className="flex items-center gap-2 flex-shrink-0">
                <select 
                  value={sortOrder} 
                  onChange={(e) => setSortOrder(e.target.value as any)}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600"
                >
                  <option value="latest">Latest</option>
                  <option value="oldest">Oldest</option>
                </select>

                {/* Filters Dropdown */}
                <div className="relative" ref={filtersDropdownRef}>
                  <button
                    onClick={() => {
                      setIsFiltersDropdownOpen(!isFiltersDropdownOpen);
                      setIsTagDropdownOpen(false);
                    }}
                    className="flex items-center gap-2 px-3 py-2 text-sm font-medium border border-gray-300 rounded-lg bg-white hover:bg-gray-50 cursor-pointer relative"
                  >
                    <span>
                      Filters {filterCount > 0 && `(${filterCount})`}
                    </span>
                    <svg className={`w-4 h-4 transition-transform duration-200 ${isFiltersDropdownOpen ? 'rotate-180' : ''}`}
                      fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7"/>
                    </svg>
                  </button>

                  {isFiltersDropdownOpen && (
                    <div className="absolute top-full right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-30 w-64">
                      <div className="max-h-96 overflow-y-auto">
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide px-3 pt-3 pb-1">
                          STATUS
                        </div>
                        {[
                          { id: 'TO_READ', label: 'To Read' },
                          { id: 'IN_PROGRESS', label: 'In Progress' },
                          { id: 'COMPLETED', label: 'Completed' }
                        ].map((status) => (
                          <label key={status.id} className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700">
                            <input
                              type="checkbox"
                              className="hidden"
                              checked={selectedStatuses.includes(status.id)}
                              onChange={() => setSelectedStatuses(prev => 
                                prev.includes(status.id) ? prev.filter(s => s !== status.id) : [...prev, status.id]
                              )}
                            />
                            <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${
                              selectedStatuses.includes(status.id) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'
                            }`}>
                              {selectedStatuses.includes(status.id) && (
                                <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5">
                                  <path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                              )}
                            </div>
                            <span className="truncate">{status.label}</span>
                          </label>
                        ))}

                        <div className="border-t border-gray-100 mt-2"></div>
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide px-3 pt-3 pb-1">
                          TYPE
                        </div>
                        {[
                          { id: 'PDF', label: 'PDF' },
                          { id: 'LINK', label: 'Link' }
                        ].map((type) => (
                          <label key={type.id} className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700">
                            <input
                              type="checkbox"
                              className="hidden"
                              checked={selectedTypes.includes(type.id)}
                              onChange={() => setSelectedTypes(prev => 
                                prev.includes(type.id) ? prev.filter(t => t !== type.id) : [...prev, type.id]
                              )}
                            />
                            <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${
                              selectedTypes.includes(type.id) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'
                            }`}>
                              {selectedTypes.includes(type.id) && (
                                <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5">
                                  <path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                              )}
                            </div>
                            <span className="truncate">{type.label}</span>
                          </label>
                        ))}

                        <div className="border-t border-gray-100 mt-2"></div>
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide px-3 pt-3 pb-1">
                          UPLOADED BY
                        </div>
                        {Array.from(new Set(materials.map(m => m.uploaderName || m.uploadedByName || 'Unknown').filter(Boolean))).map((uploader) => (
                          <label key={uploader} className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700">
                            <input
                              type="checkbox"
                              className="hidden"
                              checked={selectedUploaders.includes(uploader)}
                              onChange={() => setSelectedUploaders(prev => 
                                prev.includes(uploader) ? prev.filter(u => u !== uploader) : [...prev, uploader]
                              )}
                            />
                            <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${
                              selectedUploaders.includes(uploader) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'
                            }`}>
                              {selectedUploaders.includes(uploader) && (
                                <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5">
                                  <path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                              )}
                            </div>
                            <span className="truncate">{uploader}</span>
                          </label>
                        ))}
                      </div>

                      <div className="flex items-center justify-between px-3 py-2 border-t border-gray-100 bg-gray-50 rounded-b-xl">
                        <button
                          onClick={() => {
                            setSelectedStatuses([]);
                            setSelectedTypes([]);
                            setSelectedUploaders([]);
                          }}
                          className="text-xs text-gray-500 hover:text-red-500 cursor-pointer"
                        >
                          Clear all
                        </button>
                        <span className={`${filterCount > 0 ? 'text-green-700 font-semibold' : 'text-gray-400'} text-xs`}>
                          {filterCount === 0 ? 'No filters' : `${filterCount} selected`}
                        </span>
                      </div>
                    </div>
                  )}
                </div>
                {/* Multi-select Tag Dropdown */}
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => {
                      setIsTagDropdownOpen(!isTagDropdownOpen);
                      setIsFiltersDropdownOpen(false);
                    }}
                    className="flex items-center gap-2 px-3 py-2 text-sm font-medium border border-gray-300 rounded-lg bg-white hover:bg-gray-50 cursor-pointer relative"
                  >
                    <span>
                      Tags {activeTags.length > 0 && `(${activeTags.length})`}
                    </span>
                    <svg 
                      viewBox="0 0 24 24" 
                      fill="none" 
                      stroke="currentColor" 
                      strokeWidth="2" 
                      className={`w-4 h-4 transition-transform duration-200 ${isTagDropdownOpen ? 'rotate-180' : ''}`}
                    >
                      <path d="M19 9l-7 7-7-7" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </button>

                  {isTagDropdownOpen && (
                    <div className="absolute top-full right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-30 w-56">
                      <div className="text-xs font-semibold text-gray-400 uppercase px-3 pt-3 pb-1 tracking-wide">
                        Filter by Tag
                      </div>
                      
                      <input
                        type="text"
                        placeholder="Search tags..."
                        value={tagSearch}
                        onChange={(e) => setTagSearch(e.target.value)}
                        className="w-full px-3 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500 mx-3 my-2"
                        style={{ width: 'calc(100% - 24px)' }}
                      />

                      <div className="max-h-48 overflow-y-auto">
                        {(() => {
                          const allAvailableTags = Array.from(new Set(materials.flatMap((m) => m.tags || [])))
                            .filter(Boolean)
                            .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
                          const filteredTagList = allAvailableTags.filter(tag => 
                            tag.toLowerCase().includes(tagSearch.toLowerCase())
                          );

                          if (filteredTagList.length === 0 && !'no tags'.includes(tagSearch.toLowerCase())) {
                            return <div className="px-3 py-2 text-xs text-gray-400">No tags found</div>;
                          }

                          return (
                            <>
                              {'no tags'.includes(tagSearch.toLowerCase()) && (
                                <label
                                  className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700"
                                >
                                  <input
                                    type="checkbox"
                                    className="hidden"
                                    checked={activeTags.includes('NO_TAGS')}
                                    onChange={() => setActiveTags(prev => 
                                      prev.includes('NO_TAGS') ? prev.filter(t => t !== 'NO_TAGS') : [...prev, 'NO_TAGS']
                                    )}
                                  />
                                  <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${
                                    activeTags.includes('NO_TAGS') ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'
                                  }`}>
                                    {activeTags.includes('NO_TAGS') && (
                                      <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5">
                                        <path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                      </svg>
                                    )}
                                  </div>
                                  <span className="truncate">no tags</span>
                                </label>
                              )}
                              {filteredTagList.map((tag) => (
                                <label
                                  key={tag}
                                  title={tag}
                                  className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700"
                                >
                                  <input
                                    type="checkbox"
                                    className="hidden"
                                    checked={activeTags.includes(tag)}
                                    onChange={() => setActiveTags(prev => 
                                      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
                                    )}
                                  />
                                  <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${
                                    activeTags.includes(tag) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'
                                  }`}>
                                    {activeTags.includes(tag) && (
                                      <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5">
                                        <path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                      </svg>
                                    )}
                                  </div>
                                  <span className="truncate">{tag}</span>
                                </label>
                              ))}
                            </>
                          );
                        })()}
                      </div>

                      <div className="flex items-center justify-between px-3 py-2 border-t border-gray-100 bg-gray-50 rounded-b-xl">
                        <button
                          onClick={() => {
                            setActiveTags([]);
                            setTagSearch('');
                          }}
                          className="text-xs text-gray-500 hover:text-red-500 cursor-pointer"
                        >
                          Clear all
                        </button>
                        <span className={`${activeTags.length > 0 ? 'text-green-700 font-semibold' : 'text-gray-400'} text-xs`}>
                          {activeTags.length === 0 ? 'No filters' : `${activeTags.length} selected`}
                        </span>
                      </div>
                    </div>
                  )}
                </div>

              </div>
            </div>

            {/* Active Filter Pills */}
            <div className="flex flex-col items-start gap-2 mb-4 mt-2">
              <span className="text-gray-500 text-xs">
                Showing {filteredMaterials.length} out of {materials.length} material{materials.length !== 1 ? 's' : ''}
              </span>

              {(activeTags.length > 0 || filterCount > 0) && (
                <div className="flex flex-wrap items-center gap-2">
                  {activeTags.map((tag) => (
                    <span key={tag} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {tag === 'NO_TAGS' ? 'no tags' : tag}
                      <button onClick={() => setActiveTags(prev => prev.filter(t => t !== tag))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  {selectedStatuses.map((s) => (
                    <span key={s} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {s === 'TO_READ' ? 'To Read' : s === 'IN_PROGRESS' ? 'In Progress' : 'Completed'}
                      <button onClick={() => setSelectedStatuses(prev => prev.filter(x => x !== s))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  {selectedTypes.map((t) => (
                    <span key={t} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {t === 'PDF' ? 'PDF' : 'Link'}
                      <button onClick={() => setSelectedTypes(prev => prev.filter(x => x !== t))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  {selectedUploaders.map((u) => (
                    <span key={u} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {u}
                      <button onClick={() => setSelectedUploaders(prev => prev.filter(x => x !== u))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  <button
                    onClick={() => {
                      setActiveTags([]);
                      setSelectedStatuses([]);
                      setSelectedTypes([]);
                      setSelectedUploaders([]);
                    }}
                    className="text-xs text-gray-400 hover:text-red-500 cursor-pointer ml-1"
                  >
                    Clear all filters
                  </button>
                </div>
              )}
            </div>

            <div className="grid gap-4 relative z-0 max-h-[calc(100vh-280px)] overflow-y-auto pr-2 pb-2">
              {paginatedMaterials.map((material) => (
                <div key={material.id} className="bg-white p-4 rounded-lg border border-emerald-100 flex-shrink-0">
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <h3
                        onClick={() => setSelectedMaterial(material)}
                        className="font-semibold text-gray-900 cursor-pointer hover:text-green-700 hover:underline transition-colors"
                      >
                        {material.title}
                      </h3>
                      <p className="text-xs text-gray-600 mt-1 flex items-center gap-2">
                        {
                          (() => {
                            const displayStatus = material.myStatus || material.status;
                            return (
                              <span className={`inline-block px-2 py-1 rounded-full text-xs ${
                                displayStatus === 'TO_READ'
                                  ? 'bg-yellow-100 text-yellow-700 border border-yellow-300'
                                  : displayStatus === 'IN_PROGRESS'
                                    ? 'bg-blue-100 text-blue-700 border border-blue-300'
                                    : 'bg-green-100 text-green-800 border border-green-300'
                              }`}>
                                {displayStatus}
                              </span>
                            );
                          })()
                        }
                          <span>{material.materialType} • By {material.uploaderName || material.uploadedByName || 'Unknown'}</span>
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      {(String(material.uploaderId) === String(user?.id) || isOwner) && (
                        <>
                          <button
                            onClick={() => navigate(`/repositories/${repo.id}/materials/${material.id}/edit`)}
                            className="text-blue-600 hover:text-blue-800 text-sm mr-3 cursor-pointer"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => setDeleteCandidateId(material.id)}
                            className="text-red-600 hover:text-red-800 text-sm cursor-pointer"
                          >
                            Delete
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                  {(material.tags || []).length > 0 && (
                    <div className="flex gap-2 mt-2 flex-wrap">
                      {Array.from(new Set(material.tags || [])).map((tag: any) => (
                            <span key={tag} className="text-xs rounded-full bg-emerald-50 text-emerald-700 px-3 py-1">
                              {tag}
                            </span>
                          ))}
                    </div>
                  )}
                  {(material.description || '').trim() !== '' && (
                    <p className="text-sm text-gray-700 mt-2 line-clamp-1">{material.description}</p>
                  )}
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center items-center gap-4 mt-8 pb-4">
                <button 
                  onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
                  Previous
                </button>
                <span className="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
                <button 
                  onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  Next
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Delete confirmation modal */}
        {deleteCandidateId && (
          <div className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-60">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
              <h3 className="text-lg font-semibold mb-3">Delete Material</h3>
              <p className="text-sm text-gray-600 mb-4">Are you sure you want to delete this material?</p>
              <p className="text-xs text-gray-500 mb-6">This action cannot be undone.</p>
              {error && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
                  {error}
                </div>
              )}
              <div className="flex gap-3 justify-end">
                <button onClick={() => setDeleteCandidateId(null)} className="px-4 py-2 border border-gray-300 rounded-md text-sm cursor-pointer">Cancel</button>
                <button
                  onClick={async () => {
                    try {
                      if (deleteCandidateId) {
                        setError('');
                        await handleDeleteMaterial(deleteCandidateId);
                        // refresh list from server and close modal
                        await fetchRepository();
                        setDeleteCandidateId(null);
                      }
                    } catch (err: any) {
                      const errorMsg = err?.response?.data?.message || err?.message || 'Failed to delete material. Please try again.';
                      setError(errorMsg);
                      console.error('Error deleting material:', err);
                      // Keep modal open so user can try again or cancel
                    }
                  }}
                  className="px-4 py-2 bg-red-600 text-white rounded-md text-sm cursor-pointer hover:bg-red-700"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Delete request confirmation modal */}
        {deleteRequestCandidateId && (
          <div className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-60">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
              <h3 className="text-lg font-semibold mb-3">Delete Request</h3>
              <p className="text-sm text-gray-600 mb-4">Are you sure you want to delete this request?</p>
              <p className="text-xs text-gray-500 mb-6">This action cannot be undone.</p>
              {error && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
                  {error}
                </div>
              )}
              <div className="flex gap-3 justify-end">
                <button onClick={() => setDeleteRequestCandidateId(null)} className="px-4 py-2 border border-gray-300 rounded-md text-sm cursor-pointer">Cancel</button>
                <button
                  onClick={async () => {
                    try {
                      setError('');
                      await handleDeleteRequest(deleteRequestCandidateId);
                      await fetchRepository(); // optional sync
                    } catch (err: any) {
                      const errorMsg = err?.response?.data?.message || err?.message || 'Failed to delete request. Please try again.';
                      setError(errorMsg);
                      console.error('Error deleting request:', err);
                    }
                  }}
                  className="px-4 py-2 bg-red-600 text-white rounded-md text-sm cursor-pointer hover:bg-red-700"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}

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
                  <div className="flex-1 flex gap-4 text-sm text-gray-600">
                    <span className="font-medium flex items-center h-full pt-1">Type: <span className="font-normal ml-1">{selectedMaterial.materialType}</span></span>
                  </div>
                  <div className="flex items-center gap-3">
                    {
                      (() => {
                        const displayStatus = selectedMaterial.myStatus || selectedMaterial.status;
                        if (displayStatus === 'TO_READ') return <span className="px-3 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-700 border border-yellow-200">{displayStatus}</span>;
                        if (displayStatus === 'IN_PROGRESS') return <span className="px-3 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-700 border border-blue-200">{displayStatus}</span>;
                        return <span className="px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800 border border-green-200">{displayStatus}</span>;
                      })()
                    }
                    {selectedMaterial.materialType === 'REFERENCE' && (selectedMaterial.isbn || selectedMaterial.metadata?.isbn) && (
                      <button 
                        title="Copy ISBN"
                        onClick={() => {
                          const isbnVal = selectedMaterial.isbn || selectedMaterial.metadata?.isbn;
                          navigator.clipboard.writeText(isbnVal);
                          alert('ISBN copied to clipboard!');
                        }}
                        className="text-sm px-3 py-1 bg-green-50 text-green-700 rounded-md hover:bg-green-100 font-medium cursor-pointer flex items-center gap-1.5"
                      >
                        ISBN: {selectedMaterial.isbn || selectedMaterial.metadata?.isbn}
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
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
                <div className="flex items-center gap-3 w-full sm:w-auto">
                  <label className="text-sm font-medium text-gray-700 font-semibold">My Status:</label>
                  <select id="myStatus" defaultValue={selectedMaterial.myStatus || selectedMaterial.status} className="px-3 py-2 border border-gray-300 rounded-md text-sm bg-white min-w-[140px]" onChange={(e) => setSelectedMaterial({...selectedMaterial, myStatus: e.target.value})}>
                    <option value="TO_READ">To Read</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                  </select>
                  <button className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md text-sm font-medium transition-colors cursor-pointer" onClick={async () => {
                    try {
                      await materialApi.updateStatus(selectedMaterial.id, selectedMaterial.myStatus || selectedMaterial.status);
                      await fetchRepository();
                      setSelectedMaterial(null);
                    } catch (err: any) { setError('Failed to update status'); }
                  }}>Update Status</button>
                </div>
                <p className="text-xs text-gray-400 font-medium">Added on {new Date(selectedMaterial.createdAt).toLocaleDateString()}</p>
              </div>
            </div>
          </div>
        )}

        {/* Requests Tab */}
        {activeTab === 'requests' && (
          <div>
            <div className="sticky top-0 bg-gray-50 pt-2 pb-4 z-20 flex items-center justify-between gap-2 w-full">
              <button
                onClick={() => navigate(`/repositories/${repo.id}/requests/new`, { state: { repoName: repo.name, activeTab: 'requests', isOwner: isOwner } })}
                className="px-4 py-2 bg-green-600 text-white rounded-md text-sm hover:bg-green-700 flex-shrink-0 cursor-pointer"
              >
                + Create Request
              </button>

              <div className="relative flex-1">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
                  </svg>
                </div>
                <input
                  type="text"
                  placeholder="Search requests..."
                  value={reqSearchQuery}
                  onChange={(e) => setReqSearchQuery(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                />
              </div>

              <div className="flex items-center gap-2 flex-shrink-0">
                <select 
                  value={reqSortOrder} 
                  onChange={(e) => setReqSortOrder(e.target.value as any)}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600"
                >
                  <option value="latest">Latest</option>
                  <option value="oldest">Oldest</option>
                </select>

                <div className="relative" ref={reqFiltersDropdownRef}>
                  <button
                    onClick={() => setIsReqFiltersDropdownOpen(!isReqFiltersDropdownOpen)}
                    className="flex items-center gap-2 px-3 py-2 text-sm font-medium border border-gray-300 rounded-lg bg-white hover:bg-gray-50 cursor-pointer relative"
                  >
                    <span>Filters {reqFilterCount > 0 && `(${reqFilterCount})`}</span>
                    <svg className={`w-4 h-4 transition-transform duration-200 ${isReqFiltersDropdownOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7"/>
                    </svg>
                  </button>

                  {isReqFiltersDropdownOpen && (
                    <div className="absolute top-full right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-30 w-64">
                      <div className="max-h-96 overflow-y-auto">
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide px-3 pt-3 pb-1">STATUS</div>
                        {[
                          { id: 'OPEN', label: 'Open' },
                          { id: 'FULFILLED', label: 'Fulfilled' }
                        ].map((status) => (
                          <label key={status.id} className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700">
                            <input
                              type="checkbox" className="hidden" checked={selectedReqStatuses.includes(status.id)}
                              onChange={() => setSelectedReqStatuses(prev => prev.includes(status.id) ? prev.filter(s => s !== status.id) : [...prev, status.id])}
                            />
                            <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${selectedReqStatuses.includes(status.id) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'}`}>
                              {selectedReqStatuses.includes(status.id) && (
                                <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5"><path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                              )}
                            </div>
                            <span className="truncate">{status.label}</span>
                          </label>
                        ))}
                        <div className="border-t border-gray-100 mt-2"></div>
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide px-3 pt-3 pb-1">REQUESTER</div>
                        {Array.from(new Set(requests.map(r => r.requesterName || 'Unknown').filter(Boolean))).map((reqBy) => (
                          <label key={reqBy} className="flex items-center gap-2.5 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm text-gray-700">
                            <input
                              type="checkbox" className="hidden" checked={selectedRequesters.includes(reqBy)}
                              onChange={() => setSelectedRequesters(prev => prev.includes(reqBy) ? prev.filter(r => r !== reqBy) : [...prev, reqBy])}
                            />
                            <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center ${selectedRequesters.includes(reqBy) ? 'border-green-600 bg-green-600' : 'border-gray-300 bg-white'}`}>
                              {selectedRequesters.includes(reqBy) && (
                                <svg viewBox="0 0 10 8" fill="none" className="w-2.5 h-2.5"><path d="M1 4l2.5 2.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                              )}
                            </div>
                            <span className="truncate">{reqBy}</span>
                          </label>
                        ))}
                      </div>
                      <div className="flex items-center justify-between px-3 py-2 border-t border-gray-100 bg-gray-50 rounded-b-xl">
                        <button onClick={() => { setSelectedReqStatuses([]); setSelectedRequesters([]); }} className="text-xs text-gray-500 hover:text-red-500 cursor-pointer">Clear all</button>
                        <span className={`${reqFilterCount > 0 ? 'text-green-700 font-semibold' : 'text-gray-400'} text-xs`}>{reqFilterCount === 0 ? 'No filters' : `${reqFilterCount} selected`}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="flex flex-col items-start gap-2 mb-4 mt-2">
              <span className="text-gray-500 text-xs">
                Showing {filteredRequests.length} out of {requests.length} request{requests.length !== 1 ? 's' : ''}
              </span>
              {reqFilterCount > 0 && (
                <div className="flex flex-wrap items-center gap-2">
                  {selectedReqStatuses.map((s) => (
                    <span key={s} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {s === 'OPEN' ? 'Open' : 'Fulfilled'} <button onClick={() => setSelectedReqStatuses(prev => prev.filter(x => x !== s))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  {selectedRequesters.map((r) => (
                    <span key={r} className="inline-flex items-center gap-1 px-2 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-full font-medium cursor-pointer">
                      {r} <button onClick={() => setSelectedRequesters(prev => prev.filter(x => x !== r))} className="hover:text-green-900 cursor-pointer">×</button>
                    </span>
                  ))}
                  <button onClick={() => { setSelectedReqStatuses([]); setSelectedRequesters([]); }} className="text-xs text-gray-400 hover:text-red-500 cursor-pointer ml-1">Clear all filters</button>
                </div>
              )}
            </div>

            <div className="grid gap-4 relative z-0 max-h-[calc(100vh-280px)] overflow-y-auto pr-2 pb-2">
              {paginatedRequests.map((req) => (
                <div key={req.id} className="bg-white p-4 rounded-lg border border-gray-200 flex-shrink-0">
                  <div className="flex justify-between items-start">
                    <div className="flex-1 pr-4">
                      <h3
                        onClick={() => setSelectedRequest(req)}
                        className="font-semibold text-green-700 cursor-pointer hover:underline transition-colors block"
                      >
                        {req.title}
                      </h3>
                      <p className="text-sm text-gray-600 mt-1 line-clamp-1">{req.description}</p>
                      <p className="text-xs text-gray-600 mt-2 flex items-center gap-2">
                        <span className={`inline-block px-2 py-1 rounded-full text-xs ${
                          req.status === 'OPEN' ? 'bg-blue-100 text-blue-700 border border-blue-300' :
                          req.status === 'FULFILLED' ? 'bg-green-100 text-green-800 border border-green-300' :
                          'bg-gray-100 text-gray-700 border border-gray-300'
                        }`}>
                          {req.status === 'OPEN' ? 'Open' : req.status === 'FULFILLED' ? 'Fulfilled' : req.status}
                        </span>
                        <span>Requested by {req.requesterName || 'Unknown'}</span>
                      </p>
                    </div>
                    {(isOwner || String(req.requesterId) === String(user?.id)) && (
                      <div className="flex items-center gap-3">
                        <button onClick={() => setDeleteRequestCandidateId(req.id)} className="text-red-600 hover:text-red-800 text-sm cursor-pointer">Delete</button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {totalPagesReq > 1 && (
              <div className="flex justify-center items-center gap-4 mt-6 mb-8 pt-4 border-t border-gray-100">
                <button 
                  onClick={() => setCurrentPageReq(p => Math.max(1, p - 1))} 
                  disabled={currentPageReq === 1} 
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
                  Previous
                </button>
                <span className="text-sm text-gray-600">Page {currentPageReq} of {totalPagesReq}</span>
                <button 
                  onClick={() => setCurrentPageReq(p => Math.min(totalPagesReq, p + 1))} 
                  disabled={currentPageReq === totalPagesReq} 
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  Next
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Members Tab */}
        {activeTab === 'members' && (
          <div>
            <div className={`sticky top-0 bg-gray-50 pt-2 pb-4 z-20 w-full ${!isOwner ? 'hidden' : ''}`}>
              {isOwner && (
                <div className="p-4 bg-white border border-gray-200 rounded-lg relative" ref={inviteDropdownRef}>
                  <label className="block text-sm font-medium text-gray-900 mb-2">Invite Member</label>
                  <div className="relative w-full border border-gray-300 rounded-lg text-sm focus-within:ring-2 focus-within:ring-green-600">
                    <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
                    </svg>
                    <input
                      type="text"
                      value={inviteEmail}
                      onChange={(e) => setInviteEmail(e.target.value)}
                      placeholder="Search member by email..."
                      className="w-full bg-transparent pl-9 pr-3 py-2 outline-none"
                    />
                  </div>

                  {(searching || searchResult) && inviteEmail.length >= 3 && (
                    <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-lg z-20 p-3 flex flex-col gap-3">
                      {searching ? (
                        <p className="text-sm text-gray-400 py-1">Searching...</p>
                      ) : searchResult?.notFound ? (
                        <p className="text-sm text-gray-400 py-1">No user found with that email.</p>
                      ) : (
                        <div className="flex items-center justify-between gap-3">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-green-700 text-white text-sm font-bold flex items-center justify-center shrink-0 uppercase tracking-wide">
                              {searchResult?.firstname?.[0]}{searchResult?.lastname?.[0]}
                            </div>
                            <div>
                              <p className="text-sm font-semibold text-gray-800">{searchResult?.firstname} {searchResult?.lastname}</p>
                              <p className="text-xs text-gray-400">{searchResult?.email}</p>
                            </div>
                          </div>
                          {repo.members.some(m => m.email?.toLowerCase() === searchResult?.email.toLowerCase()) ? (
                            <span className="text-xs text-gray-400 font-medium">Already a member</span>
                          ) : (
                            <button
                              onClick={() => handleInviteMember(searchResult?.email)}
                              className="bg-green-700 text-white px-3 py-1.5 text-xs font-semibold rounded-lg hover:bg-green-800 cursor-pointer"
                            >
                              Invite
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="grid gap-4 relative z-0 max-h-[calc(100vh-280px)] overflow-y-auto pr-2 pb-2">
              {paginatedMembers.map((member) => {
                const nameParts = ((member.firstname || '') + ' ' + (member.lastname || '')).trim().split(' ');
                const initials = nameParts.length >= 2 
                  ? (nameParts[0][0] + nameParts[nameParts.length - 1][0]).toUpperCase()
                  : nameParts[0] ? nameParts[0].slice(0, 2).toUpperCase() : member.email?.[0]?.toUpperCase() || '?';
                  
                return (
                  <div key={member.id || member.userId} className="bg-white rounded-xl border border-gray-100 p-4 flex items-center justify-between gap-3 flex-shrink-0">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-green-700 text-white text-sm font-bold flex items-center justify-center shrink-0 tracking-wide">
                        {initials}
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-gray-800">{member.firstname} {member.lastname}</p>
                        <p className="text-xs text-gray-400 mt-0.5">{member.email}</p>
                      </div>
                    </div>
                    <div>
                      {member.userId === repo.ownerId ? (
                      <span className="px-3 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-700 border border-green-200">
                        Owner
                      </span>
                    ) : String(member.userId) === String(user?.id) ? (
                      <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-50 text-blue-600 border border-blue-200">
                        You
                      </span>
                    ) : (
                      <div className="flex items-center gap-2">
                        <span className="px-3 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-600 border border-gray-200">
                          Member
                        </span>
                        {isOwner && (
                          confirmRemoveId === member.userId ? (
                            <div className="flex items-center gap-2 text-xs ml-2">
                              <span className="text-gray-500">Remove {member.firstname || member.email}?</span>
                              <button onClick={() => setConfirmRemoveId(null)} className="text-gray-400 hover:text-gray-600 cursor-pointer">
                                Cancel
                              </button>
                              <button onClick={() => handleRemoveMember(member.userId)} className="text-red-500 hover:text-red-700 font-semibold cursor-pointer">
                                Confirm
                              </button>
                            </div>
                          ) : (
                            <button onClick={() => setConfirmRemoveId(member.userId)} className="text-red-600 hover:text-red-800 text-sm ml-2 cursor-pointer">
                              Remove
                            </button>
                          )
                        )}
                      </div>
                    )}
                  </div>
                </div>
              );
              })}
            </div>

            {totalPagesMem > 1 && (
              <div className="flex justify-center items-center gap-4 mt-6 mb-8 pt-4 border-t border-gray-100">
                <button 
                  onClick={() => setCurrentPageMem(p => Math.max(1, p - 1))} 
                  disabled={currentPageMem === 1} 
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
                  Previous
                </button>
                <span className="text-sm text-gray-600">Page {currentPageMem} of {totalPagesMem}</span>
                <button 
                  onClick={() => setCurrentPageMem(p => Math.min(totalPagesMem, p + 1))} 
                  disabled={currentPageMem === totalPagesMem} 
                  className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 disabled:cursor-not-allowed hover:text-gray-900 cursor-pointer"
                >
                  Next
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {toast && (
        <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium flex items-center gap-2 text-white ${toast.type === 'success' ? 'bg-green-700' : 'bg-red-600'}`}>
          {toast.type === 'success' ? '✅' : '❌'}
          {toast.msg}
        </div>
      )}

      {/* Request Detail Modal */}
      {selectedRequest && (
        <div className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl p-6 relative max-h-[90vh] flex flex-col overflow-hidden">
            <div className="flex justify-between items-start mb-4 pr-6 relative">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{selectedRequest.title}</h2>
                <p className="text-sm text-gray-500 mt-1">Requested by {selectedRequest.requesterName || 'Unknown'} • {new Date(selectedRequest.createdAt).toLocaleDateString()}</p>
              </div>
              <div className="absolute top-0 right-0 flex items-center gap-3">
                <button onClick={() => { setSelectedRequest(null); setFulfillRequestId(null); setFulfillMaterialId(''); setIsEditingReqMaterial(false); }} className="text-gray-400 hover:text-gray-900 cursor-pointer p-1 text-xl leading-none">✕</button>
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

              {selectedRequest.description && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-2">Description</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">{selectedRequest.description}</p>
                  </div>
                </div>
              )}

              {selectedRequest.status === 'FULFILLED' && selectedRequest.materialId && !isEditingReqMaterial && (
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <h3 className="text-sm font-semibold text-gray-900">Attached Material</h3>
                  </div>
                  {materials.find(m => m.id === selectedRequest.materialId) ? (
                    <div className="bg-white p-3 border border-emerald-100 rounded-lg flex justify-between items-center cursor-pointer hover:border-green-300" onClick={() => { setSelectedRequest(null); setSelectedMaterial(materials.find(m => m.id === selectedRequest.materialId)); setIsEditingReqMaterial(false); }}>
                      <span className="font-medium text-green-700 hover:underline truncate">{materials.find(m => m.id === selectedRequest.materialId)?.title}</span>
                      <span className="text-xs text-gray-500 ml-2 flex-shrink-0">{materials.find(m => m.id === selectedRequest.materialId)?.materialType}</span>
                    </div>
                  ) : <span className="text-sm text-gray-500">Material loaded externally or unavailable.</span>}
                </div>
              )}

              {(selectedRequest.status === 'OPEN' && String(selectedRequest.requesterId) !== String(user?.id)) || (selectedRequest.status === 'FULFILLED' && isEditingReqMaterial) ? (
                <div className="bg-gray-50 p-4 rounded-lg border border-gray-200 mt-4">
                  <h3 className="text-sm font-semibold text-gray-900 mb-3">
                    {isEditingReqMaterial ? 'Edit Request' : 'Fulfill Request'}
                  </h3>
                  <select
                    value={fulfillMaterialId || (isEditingReqMaterial ? (selectedRequest.materialId || '') : '')}
                    onChange={(e) => {
                      if (e.target.value === 'NEW') {
                        navigate(`/repositories/${repo.id}/materials/new`, { state: { repoName: repo.name, activeTab: 'requests', isOwner: isOwner } });
                      } else {
                        setFulfillMaterialId(e.target.value);
                      }
                    }}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm mb-3 bg-white focus:ring-green-500 focus:border-green-500"
                  >
                    <option value="">Select a material to fulfill with...</option>
                    {materials
                      .map((m) => (
                        <option key={m.id} value={String(m.id)}>{m.title}</option>
                      ))}
                    <option value="NEW" className="font-semibold text-green-700">+ Attach a new material...</option>
                  </select>
                  <button
                    disabled={isEditingReqMaterial 
                      ? (!fulfillMaterialId || String(fulfillMaterialId) === String(selectedRequest.materialId))
                      : (!fulfillMaterialId || fulfillMaterialId === 'NEW')}
                    onClick={() => {
                      handleFulfillRequest(selectedRequest.id, fulfillMaterialId);
                    }}
                    className="w-full px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  >
                    {isEditingReqMaterial ? 'Update Material' : 'Fulfill Request'}
                  </button>
                </div>
              ) : null}
            </div>

            <div className="mt-6 pt-4 border-t border-gray-100 flex justify-end gap-3">
              {isEditingReqMaterial && (
                <button
                  onClick={() => {
                    setIsEditingReqMaterial(false);
                    setFulfillMaterialId('');
                  }}
                  className="px-4 py-2 bg-gray-300 text-gray-700 rounded-md text-sm font-medium hover:bg-gray-400 cursor-pointer"
                >
                  Cancel
                </button>
              )}
              {/* Temporarily disabled: Edit Attached button (re-enable when editing workflow is ready)
              {selectedRequest.status === 'FULFILLED' && (String(selectedRequest.fulfilledBy) === String(user?.id)) && !isEditingReqMaterial && (
                <button
                  onClick={() => {
                    // Initialize fulfillMaterialId with current material when entering edit mode
                    setFulfillMaterialId(String(selectedRequest.materialId || ''));
                    setIsEditingReqMaterial(true);
                  }}
                  className="px-4 py-2 bg-gray-100 text-gray-700 hover:bg-gray-200 rounded-md text-sm font-medium transition-colors cursor-pointer"
                >
                  Edit Attached
                </button>
              )}
              */}
            </div>
          </div>
        </div>
      )}

      <footer className="w-full bg-white border-t border-gray-200 mt-12 py-6">
        <div className="max-w-6xl mx-auto px-6 text-center text-gray-500 text-sm">
          &copy; {new Date().getFullYear()} ResearchCenter. All rights reserved.
        </div>
      </footer>
    </div>
  );
};

export default RepositoryDetailPage;
