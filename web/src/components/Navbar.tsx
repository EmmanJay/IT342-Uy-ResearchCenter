import { useNavigate } from 'react-router-dom';
import { SessionManager } from '../auth/sessionManager';

const Navbar = () => {
  const navigate = useNavigate();
  const user = SessionManager.getUser();
  const initials = `${user?.firstname?.[0] || ''}${user?.lastname?.[0] || ''}`.toUpperCase();

  const handleLogout = () => {
    SessionManager.clear();
    navigate('/login');
  };

  return (
    <nav className="bg-white shadow-sm h-14 flex items-center px-6 gap-4 border-b border-gray-200">
      <div className="flex items-center mr-auto overflow-hidden text-sm">
        <span 
          onClick={() => navigate('/')} 
          className="text-green-700 cursor-pointer hover:underline font-bold text-lg flex-shrink-0"
        >
          ResearchCenter
        </span>
      </div>

      <div
        title={user ? `${user.firstname} ${user.lastname}` : 'Profile'}
        className="w-9 h-9 rounded-full bg-green-700 text-white font-bold text-sm flex items-center justify-center cursor-pointer"
      >
        {initials}
      </div>

      <button onClick={handleLogout} className="text-sm text-red-600 hover:underline font-medium ml-2 cursor-pointer">
        Logout
      </button>
    </nav>
  );
};

export default Navbar;

