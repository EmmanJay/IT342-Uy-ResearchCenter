import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import RepositoryDetailPage from './pages/RepositoryDetailPage';
import AddMaterialPage from './pages/AddMaterialPage';
import EditMaterialPage from './pages/EditMaterialPage';
import NewRequestPage from './pages/NewRequestPage';
import ProtectedRoute from './components/ProtectedRoute';

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

function App() {

  return (
    <GoogleOAuthProvider clientId={CLIENT_ID}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/repositories/:id"
            element={
              <ProtectedRoute>
                <RepositoryDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/repositories/:id/materials/new"
            element={
              <ProtectedRoute>
                <AddMaterialPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/repositories/:id/materials/:materialId/edit"
            element={
              <ProtectedRoute>
                <EditMaterialPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/repositories/:id/requests/new"
            element={
              <ProtectedRoute>
                <NewRequestPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </GoogleOAuthProvider>
  );
}

export default App;
