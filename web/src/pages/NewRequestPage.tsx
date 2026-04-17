import { useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { requestApi } from '../api/requestApi';
import Breadcrumbs from '../components/Breadcrumbs';

const NewRequestPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const repoName = location.state?.repoName || 'Repository';
  const isOwner = location.state?.isOwner ?? true;

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!title.trim() || !description.trim()) {
      setError('All fields are required');
      return;
    }

    try {
      setLoading(true);
      await requestApi.create({ title, description, repositoryId: id! });
      navigate(`/repositories/${id}`, { state: { activeTab: 'requests', isOwner } });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create request');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto px-6 py-8">
        <Breadcrumbs items={[
          { label: isOwner ? 'Your Repositories' : 'Invited Repositories', path: '/' },
          { label: repoName, path: `/repositories/${id}`, state: { activeTab: 'requests' } },
          { label: 'New Request' }
        ]} />

        <h1 className="text-3xl font-bold text-gray-900 mt-6 mb-6">Create Research Request</h1>

        <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border border-gray-200 space-y-4">
          {error && <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{error}</div>}

          <div>
            <label className="block text-sm font-medium text-gray-900 mb-1">Request Title *</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="What material are you looking for?"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-900 mb-1">Detailed Description *</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the material or research you need..."
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600 h-32 resize-none"
            />
          </div>

          <div className="flex gap-3 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={() => navigate(`/repositories/${id}`, { state: { activeTab: 'requests' } })}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-60 cursor-pointer"
            >
              {loading ? 'Creating...' : 'Create Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default NewRequestPage;
