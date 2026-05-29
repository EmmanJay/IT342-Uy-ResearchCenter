import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { useEffect, useState } from 'react';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import DashboardPage from './features/dashboard/DashboardPage';
import RepositoryDetailPage from './features/repository/RepositoryDetailPage';
import AddMaterialPage from './features/material/AddMaterialPage';
import EditMaterialPage from './features/material/EditMaterialPage';
import NewRequestPage from './features/request/NewRequestPage';
import ProfilePage from './features/profile/ProfilePage';
import AcceptInvitePage from './features/invite/AcceptInvitePage';
import ActivitiesPage from './features/activity/ActivitiesPage';
import NotificationsPage from './features/activity/NotificationsPage';
import BookmarksPage from './features/bookmarks/BookmarksPage';
import ProtectedRoute from './shared/components/ProtectedRoute';
import PublicRoute from './shared/components/PublicRoute';
import { SessionManager } from './shared/auth/sessionManager';
import { AdminLayout } from './features/admin/AdminLayout';
import AdminStatsPage from './features/admin/AdminStatsPage';
import AdminUsersPage from './features/admin/AdminUsersPage';
import AdminRepositoriesPage from './features/admin/AdminRepositoriesPage';
import AdminRequestsPage from './features/admin/AdminRequestsPage';
import AdminMaterialsPage from './features/admin/AdminMaterialsPage';
import AdminAnnouncementsPage from './features/admin/AdminAnnouncementsPage';
import AdminActivityPage from './features/admin/AdminActivityPage';

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

function App() {
  const [showSuspendedModal, setShowSuspendedModal] = useState(false);

  useEffect(() => {
    const onAccountSuspended = () => setShowSuspendedModal(true);
    window.addEventListener('rc-account-suspended', onAccountSuspended);
    return () => window.removeEventListener('rc-account-suspended', onAccountSuspended);
  }, []);

  const handleSuspendedAcknowledge = () => {
    SessionManager.clear();
    window.location.replace('/login');
  };

  return (
    <GoogleOAuthProvider clientId={CLIENT_ID}>
      <BrowserRouter>
        {showSuspendedModal && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/60 px-4">
            <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
              <h2 className="text-lg font-bold text-gray-900">Account Suspended</h2>
              <p className="mt-2 text-sm text-gray-600">
                Your account is suspended. Please contact administrator.
              </p>
              <button
                type="button"
                onClick={handleSuspendedAcknowledge}
                className="mt-5 w-full rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700 cursor-pointer"
              >
                Go to Login
              </button>
            </div>
          </div>
        )}
        <Routes>
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicRoute>
                <RegisterPage />
              </PublicRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
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
            path="/invite/accept"
            element={<AcceptInvitePage />}
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
          <Route
            path="/activities"
            element={
              <ProtectedRoute>
                <ActivitiesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/bookmarks"
            element={
              <ProtectedRoute>
                <BookmarksPage />
              </ProtectedRoute>
            }
          />
          <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
            <Route path="stats" element={<AdminStatsPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="repositories" element={<AdminRepositoriesPage />} />
            <Route path="materials" element={<AdminMaterialsPage />} />
            <Route path="requests" element={<AdminRequestsPage />} />
            <Route path="announcements" element={<AdminAnnouncementsPage />} />
            <Route path="activity" element={<AdminActivityPage />} />
            <Route index element={<Navigate to="/admin/stats" replace />} />
          </Route>
          <Route path="/" element={<Navigate to={SessionManager.getUser()?.role === 'ADMIN' ? '/admin/stats' : '/dashboard'} replace />} />
        </Routes>
      </BrowserRouter>
    </GoogleOAuthProvider>
  );
}

export default App;
