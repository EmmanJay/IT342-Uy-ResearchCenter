import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import MaterialForm from '../components/MaterialForm';
import { materialApi } from '../api/materialApi';
import Navbar from '../components/Navbar';
import Breadcrumbs from '../components/Breadcrumbs';

const EditMaterialPage = () => {
  const navigate = useNavigate();
  const { id, materialId } = useParams<{ id: string; materialId: string }>();
  const [initialData, setInitialData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isOwner, setIsOwner] = useState<boolean>(true);

  useEffect(() => {
    const fetchMaterial = async () => {
      try {
        const material = await materialApi.getById(materialId!);
        setInitialData(material);
        setIsOwner(String(material.uploadedBy) === String(material.repositoryId));
      } catch (err) {
        setError('Failed to load material for editing');
      } finally {
        setLoading(false);
      }
    };
    if (materialId) {
      fetchMaterial();
    }
  }, [materialId]);

  const handleUpdate = async (payload: any) => {
    await materialApi.update(materialId!, payload);
    navigate(`/repositories/${id}`);
  };

  if (loading) return <div className="min-h-screen flex items-center justify-center text-gray-500">Loading material data...</div>;
  if (error) return <div className="min-h-screen flex items-center justify-center text-red-600">{error}</div>;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      <div className="max-w-2xl mx-auto w-full px-6 py-6 flex-1">
        <Breadcrumbs items={[
          { label: isOwner ? 'Your Repositories' : 'Invited Repositories', path: '/' },
          { label: initialData?.repositoryName || 'Repository', path: `/repositories/${id}` },
          { label: initialData?.title || 'Material', path: '#' },
          { label: 'Edit' }
        ]} />
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Edit Material</h1>

        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <MaterialForm 
            initial={initialData} 
            repositoryId={id} 
            disableType={true} 
            onSubmit={handleUpdate} 
            onCancel={() => navigate(`/repositories/${id}`)} 
            submitLabel="Edit Material" 
          />
        </div>
      </div>
    </div>
  );
};

export default EditMaterialPage;