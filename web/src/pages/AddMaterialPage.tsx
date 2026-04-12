import { useNavigate, useParams, useLocation } from 'react-router-dom';
import MaterialForm from '../components/MaterialForm';
import { materialApi } from '../api/materialApi';
import Navbar from '../components/Navbar';
import Breadcrumbs from '../components/Breadcrumbs';

const AddMaterialPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const repoName = location.state?.repoName || 'Repository';

  const handleCreate = async (payload: any) => {
    try {
      payload.repositoryId = id!;
      await materialApi.create(payload);
      
      // Always just return to materials tab
      navigate(`/repositories/${id}`, { state: { activeTab: 'materials' } });
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to add material. Please try again.');
    }
  };

  const isOwner = location.state?.isOwner ?? true;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <div className="max-w-2xl mx-auto w-full px-6 py-6 flex-1">
        <Breadcrumbs items={[
          { label: isOwner ? 'Your Repositories' : 'Invited Repositories', path: '/' },
          { label: repoName, path: `/repositories/${id}`, state: { activeTab: location.state?.activeTab || 'materials' } },
          { label: 'Add Material' }
        ]} />
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Add Material</h1>

        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <MaterialForm repositoryId={id} onSubmit={handleCreate} onCancel={() => navigate(`/repositories/${id}`, { state: { activeTab: location.state?.activeTab || 'materials' } })} submitLabel="Add Material" />
        </div>
      </div>
    </div>
  );
};

export default AddMaterialPage;
