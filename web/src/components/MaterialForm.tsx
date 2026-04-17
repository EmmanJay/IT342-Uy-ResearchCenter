import React, { useEffect, useState } from 'react';
import { MaterialType } from '../types';
import type { MaterialTypeValue } from '../types';
import { supabaseUpload } from '../api/supabaseUpload';
import { googleBooksApi } from '../api/googleBooksApi';

type Props = {
  initial?: any;
  repositoryId?: string;
  onSubmit: (payload: any) => Promise<any>;
  onCancel?: () => void;
  submitLabel?: string;
  disableType?: boolean;
};

const MaterialForm: React.FC<Props> = ({ initial, repositoryId, onSubmit, onCancel, submitLabel = 'Save', disableType = false }) => {
  const [title, setTitle] = useState(initial?.title || '');
  const [description, setDescription] = useState(initial?.description || '');
  const [type, setType] = useState<MaterialTypeValue>(initial?.materialType || initial?.type || MaterialType.LINK);
  
  const initialTags: string[] = initial?.tags || [];
  const uniqueTags = Array.from(new Set(initialTags));
  const [tags, setTags] = useState(uniqueTags.join(', '));
  
  const [url, setUrl] = useState(initial?.url || '');

  let parsedIsbn = initial?.isbn || '';
  if (!parsedIsbn && initial?.metadata && typeof initial.metadata === 'string') {
    try {
      const parsedMeta = JSON.parse(initial.metadata);
      parsedIsbn = parsedMeta.isbn || '';
    } catch(e) {}
  } else if (!parsedIsbn && initial?.metadata?.isbn) {
    parsedIsbn = initial.metadata.isbn;
  }

  const [originalIsbn, setOriginalIsbn] = useState(parsedIsbn);
  const [isbn, setIsbn] = useState(parsedIsbn);
  const [file, setFile] = useState<File | null>(null);
  const [existingFileUrl, setExistingFileUrl] = useState(initial?.fileUrl || '');
  const [fileDeleted, setFileDeleted] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [metadataLoaded, setMetadataLoaded] = useState(false);

  useEffect(() => {
    setTitle(initial?.title || '');
    setDescription(initial?.description || '');
    setType(initial?.materialType || initial?.type || MaterialType.LINK);
    
    const initTags: string[] = initial?.tags || [];
    setTags(Array.from(new Set(initTags)).join(', '));
    
    setUrl(initial?.url || '');
    
    const initIsbn = initial?.metadata?.isbn || initial?.isbn || '';
    setIsbn(initIsbn);
    setOriginalIsbn(initIsbn);

    setExistingFileUrl(initial?.fileUrl || '');
    setFileDeleted(false);
    setFile(null);
    setSearchQuery('');
    setMetadataLoaded(false);
    setIsSearching(false);
  }, [initial]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (!selected) return;
    if (selected.size > 10 * 1024 * 1024) return setError('File size exceeds 10MB limit');
    if (selected.type !== 'application/pdf') return setError('Only PDF files are allowed');
    setFile(selected);
    setError('');
  };

  const handleGoogleSearch = async () => {
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    setError('');
    
    try {
      const data = await googleBooksApi.fetchBookMetadata(searchQuery);
      if (data) {
        setTitle(data.title || '');
        setDescription(data.description || '');
        setIsbn(data.isbn || '');
        setMetadataLoaded(true);
      } else {
        setError('No book found for this search');
        setMetadataLoaded(false);
      }
    } catch (err: any) {
      setError('Failed to fetch from Google Books');
    } finally {
      setIsSearching(false);
    }
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setError('');
    if (!title.trim()) return setError('Title is required');
    try {
      setUploading(true);
      let finalFileUrl = existingFileUrl;
      // if user deleted existing file
      if (fileDeleted) finalFileUrl = '';
      // if a new file selected, upload it (needs repositoryId)
      if (type === MaterialType.PDF && file) {
        if (!repositoryId) throw new Error('Repository ID required to upload file');
        finalFileUrl = await supabaseUpload.uploadPdfToSupabase(file, repositoryId);
      }

      const tagsArr = tags ? Array.from(new Set(tags.split(',').map((t) => t.trim()).filter(Boolean))) : [];

      const payload: any = {
        title: title.trim(),
        description: description?.trim() || undefined,
        type,
        tags: tagsArr,
        url: url || undefined,
        fileUrl: finalFileUrl || undefined,
      };

      if (type === MaterialType.REFERENCE && isbn.trim()) {
        payload.metadata = { isbn: isbn.trim() };
      }

      await onSubmit(payload);
    } catch (err: any) {
      setError(err?.message || 'Failed to submit');
    } finally {
      setUploading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="p-3 bg-red-50 text-red-700 rounded">{error}</div>}

      <div className="border-b border-gray-200 pb-4 mb-4">
        <label className="block text-sm font-medium text-gray-900 mb-2">Search Google Books (Optional)</label>
        <div className="flex gap-2">
          <input 
            type="text" 
            value={searchQuery} 
            onChange={(e) => setSearchQuery(e.target.value)} 
            placeholder="Search by title, author, or ISBN" 
            className="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm"
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                handleGoogleSearch();
              }
            }}
          />
          <button 
            type="button" 
            onClick={handleGoogleSearch} 
            disabled={isSearching || !searchQuery.trim()} 
            className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md text-sm disabled:opacity-50 cursor-pointer"
          >
            {isSearching ? 'Searching...' : 'Search'}
          </button>
        </div>
        {metadataLoaded && <p className="text-xs text-green-600 mt-2">Metadata loaded from Google Books</p>}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-900 mb-1">Title *</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-900 mb-1">Description</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm h-24" />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-900 mb-1">Material Type *</label>
        <select value={type} onChange={(e) => setType(e.target.value as MaterialTypeValue)} disabled={disableType} className={`w-full px-3 py-2 border ${disableType ? 'border-gray-200 bg-gray-50 text-gray-700' : 'border-gray-300'} rounded-md text-sm`}>
          <option value={MaterialType.PDF}>PDF</option>
          <option value={MaterialType.LINK}>Link</option>
          <option value={MaterialType.REFERENCE}>Reference</option>
        </select>
      </div>

      {type === MaterialType.REFERENCE && (
        <div>
          <label className="block text-sm font-medium text-gray-900 mb-1">
            ISBN {disableType && originalIsbn && <span className="text-gray-500 font-normal ml-2">(Original: {originalIsbn})</span>}
          </label>
          <input type="text" value={isbn} onChange={(e) => setIsbn(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="Optional ISBN" />
        </div>
      )}

      {type === MaterialType.PDF && (
        <div>
          <label className="block text-sm font-medium text-gray-900 mb-1">Upload PDF {existingFileUrl && !fileDeleted ? '(keeps existing)' : ''}</label>
          <div className="flex items-center gap-3">
            <label className="inline-flex items-center px-3 py-2 bg-white border border-gray-300 rounded-md text-sm cursor-pointer hover:bg-gray-50">
              <input type="file" accept=".pdf" onChange={handleFileChange} className="hidden" />
              <span className="text-sm text-gray-700">Choose File</span>
            </label>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-600">{file ? file.name : (existingFileUrl ? 'Uploaded file present' : 'No file selected')}</span>
              {(file || (existingFileUrl && !fileDeleted)) && (
                <button type="button" onClick={() => { setFile(null); setFileDeleted(true); setExistingFileUrl(''); }} className="text-red-600 ml-2 text-sm hover:text-red-800 cursor-pointer">✕</button>
              )}
            </div>
          </div>
        </div>
      )}

      {type === MaterialType.LINK && (
        <div>
          <label className="block text-sm font-medium text-gray-900 mb-1">URL *</label>
          <input type="url" value={url} onChange={(e) => setUrl(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" />
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-900 mb-1">Tags (comma-separated)</label>
        <input value={tags} onChange={(e) => setTags(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" />
      </div>

      <div className="flex gap-3 pt-2 justify-end">
        {onCancel && <button type="button" onClick={onCancel} className="px-4 py-2 border border-gray-300 rounded-md text-sm cursor-pointer">Cancel</button>}
        <button type="submit" disabled={uploading} className="px-4 py-2 bg-green-600 text-white rounded-md text-sm cursor-pointer">{uploading ? 'Saving...' : submitLabel}</button>
      </div>
    </form>
  );
};

export default MaterialForm;
