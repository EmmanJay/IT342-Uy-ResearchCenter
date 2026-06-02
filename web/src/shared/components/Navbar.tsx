import { useNavigate } from 'react-router-dom';
import { SessionManager } from '../auth/sessionManager';
import AppLogo from './AppLogo';
import UserAvatar from './UserAvatar';
import NotificationDropdown from './NotificationDropdown';

const Navbar = () => {
  const navigate = useNavigate();
  const user = SessionManager.getUser();

  const handleLogout = () => {
    SessionManager.clear();
    navigate('/login', { replace: true });
  };

  return (
    <nav className="bg-white shadow-sm h-14 flex items-center px-6 gap-4 border-b border-gray-200">
      <div className="flex items-center mr-auto overflow-hidden text-sm">
        <button type="button" onClick={() => navigate('/dashboard')} className="cursor-pointer">
          <AppLogo />
        </button>
      </div>

      <NotificationDropdown />

      <div
        title={user ? `${user.firstname} ${user.lastname}` : 'Profile'}
        onClick={() => navigate('/profile')}
        className="cursor-pointer ml-1"
      >
        <UserAvatar
          name={`${user?.firstname || ''} ${user?.lastname || ''}`}
          email={user?.email}
          imageUrl={user?.profilePicture}
          size="sm"
          className="hover:bg-green-800 transition-colors"
        />
      </div>

    </nav>
  );
};

export default Navbar;
