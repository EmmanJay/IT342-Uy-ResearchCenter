import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import RepositoryDetailPage from './pages/RepositoryDetailPage';
import AddMaterialPage from './pages/AddMaterialPage';
import EditMaterialPage from './pages/EditMaterialPage';
import NewRequestPage from './pages/NewRequestPage';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
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
  );
}

export default App;
