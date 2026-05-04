import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import DashboardPage from './features/dashboard/DashboardPage';
import RepositoryDetailPage from './features/repository/RepositoryDetailPage';
import AddMaterialPage from './features/material/AddMaterialPage';
import EditMaterialPage from './features/material/EditMaterialPage';
import NewRequestPage from './features/request/NewRequestPage';
import ProtectedRoute from './shared/components/ProtectedRoute';

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
