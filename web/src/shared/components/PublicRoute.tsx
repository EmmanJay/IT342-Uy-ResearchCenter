import { Navigate } from 'react-router-dom';
import { SessionManager } from '../auth/sessionManager';

interface PublicRouteProps {
  children: React.ReactNode;
}

export const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
  if (SessionManager.isLoggedIn()) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

export default PublicRoute;