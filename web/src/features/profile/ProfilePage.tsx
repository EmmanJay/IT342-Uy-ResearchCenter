import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { SessionManager } from '../../shared/auth/sessionManager';
import Navbar from '../../shared/components/Navbar';
import { User, Mail, LogOut, ArrowLeft, Edit2, X, Check, Camera, Shield, Activity } from 'lucide-react';
import authApi from '../auth/api/authApi';
import { supabaseUpload } from '../material/api/supabaseUpload';
import { repositoryApi } from '../repository/api/repositoryApi';
import { ActivityFeed } from '../activity/ActivityFeed';
import LoadingScreen from '../../shared/components/LoadingScreen';

export default function ProfilePage() {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState(SessionManager.getUser());
  const user = currentUser;
  const [isEditing, setIsEditing] = useState(false);
  const [firstname, setFirstname] = useState(user?.firstname || '');
  const [lastname, setLastname] = useState(user?.lastname || '');
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [removingAvatar, setRemovingAvatar] = useState(false);
  const [error, setError] = useState('');
  const [profileStats, setProfileStats] = useState({ dateJoined: 'N/A', reposCreated: 0, daysActive: 0 });
  const [pageLoading, setPageLoading] = useState(true);

  useEffect(() => {
    const loadStats = async () => {
      try {
        const [profileRes, reposRes] = await Promise.all([
          authApi.getMe(),
          repositoryApi.getAll().catch(() => [])
        ]);
        
        if (profileRes) {
          const joinedDate = new Date(profileRes.createdAt || Date.now());
          const days = Math.floor((Date.now() - joinedDate.getTime()) / (1000 * 60 * 60 * 24));
          const dateJoinedStr = joinedDate.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
          const createdCount = Array.isArray(reposRes) ? reposRes.filter(r => r.ownerId === profileRes.id).length : 0;
          setProfileStats({ dateJoined: profileRes.createdAt ? dateJoinedStr : 'N/A', reposCreated: createdCount, daysActive: days });
          
          if (!currentUser?.createdAt && profileRes.createdAt) {
             const newUser = { ...currentUser, ...profileRes };
             SessionManager.saveUser(newUser);
             setCurrentUser(newUser);
          }
        }
      } catch (err) {
        console.error('Failed to load profile stats');
      } finally {
        setPageLoading(false);
      }
    };
    loadStats();
  }, []);

  const handleLogout = () => {
    SessionManager.clear();
    navigate('/login');
  };

  const handleSave = async () => {
    if (!firstname.trim() || !lastname.trim()) {
      setError('Name fields cannot be empty');
      return;
    }

    try {
      setSaving(true);
      setError('');
      const updatedUser = await authApi.updateProfile({ 
        firstname, 
        lastname,
        profilePicture: user?.profilePicture
      });
      
      // Update session with new user data
      if (user) {
        const newUser = { ...user, ...updatedUser };
        SessionManager.saveUser(newUser);
        setCurrentUser(newUser);
      }
      setIsEditing(false);
    } catch (err) {
      setError('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploadingAvatar(true);
      setError('');
      
      // 1. Upload to Supabase Storage
      const publicUrl = await supabaseUpload.uploadImageToSupabase(file);
      
      // 2. Save profile picture in backend database
      const updatedUser = await authApi.updateProfile({ 
        firstname: user?.firstname || '', 
        lastname: user?.lastname || '', 
        profilePicture: publicUrl 
      });
      
      // 3. Update localStorage session
      if (user) {
        const newUser = { ...user, ...updatedUser };
        SessionManager.saveUser(newUser);
        setCurrentUser(newUser);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to upload profile picture');
    } finally {
      setUploadingAvatar(false);
    }
  };

  const initials = `${user?.firstname?.[0] || ''}${user?.lastname?.[0] || ''}`.toUpperCase();

  if (pageLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Navbar />
        <div className="flex-1 flex items-center justify-center">
          <LoadingScreen label="Loading profile" fullScreen={false} />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />
      
      <main className="max-w-4xl mx-auto px-6 py-8 w-full flex-1 flex flex-col gap-8">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="flex items-center text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors cursor-pointer mb-6"
          >
            <ArrowLeft className="w-4 h-4 mr-1" />
            Back
          </button>
          
          <h1 className="text-2xl font-bold text-gray-900">Your Profile</h1>
          <p className="text-sm text-gray-600 mt-1">Manage your account details and view your activity</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Profile Card */}
          <div className="md:col-span-1">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex flex-col items-center text-center relative">
              
              {/* Profile Picture Upload Avatar Overlay */}
              <div className="relative mb-4 group">
                <div className="w-24 h-24 rounded-full bg-green-50 text-green-700 flex items-center justify-center text-3xl font-bold shadow-inner overflow-hidden border-2 border-green-100">
                  {user?.profilePicture ? (
                    <img src={user.profilePicture} alt="Profile" className="w-full h-full object-cover" />
                  ) : (
                    initials || <User className="w-10 h-10" />
                  )}
                </div>
                
                {isEditing && (
                  <label
                    htmlFor="avatar-upload"
                    className="absolute bottom-0 right-0 bg-green-600 hover:bg-green-700 text-white p-2 rounded-full shadow-md cursor-pointer transition-transform hover:scale-110 flex items-center justify-center border-2 border-white"
                    title="Upload profile picture"
                  >
                    <Camera className="w-3.5 h-3.5" />
                    <input
                      type="file"
                      id="avatar-upload"
                      className="hidden"
                      accept="image/*"
                      onChange={handleAvatarUpload}
                      disabled={uploadingAvatar}
                    />
                  </label>
                )}
              </div>
              
              {uploadingAvatar && (
                <span className="text-xs text-green-600 font-medium mb-3 animate-pulse">Uploading photo...</span>
              )}
              {removingAvatar && (
                <span className="text-xs text-red-600 font-medium mb-3 animate-pulse">Removing photo...</span>
              )}
              
              {isEditing && user?.profilePicture && !uploadingAvatar && !removingAvatar && (
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      setRemovingAvatar(true);
                      const updatedUser = await authApi.updateProfile({ 
                        firstname: user?.firstname || '', 
                        lastname: user?.lastname || '', 
                        profilePicture: '' 
                      });
                      if (user) {
                        const newUser = { ...user, ...updatedUser };
                        SessionManager.saveUser(newUser);
                        setCurrentUser(newUser);
                      }
                    } catch (err) {
                      setError('Failed to remove photo. Please try again.');
                    } finally {
                      setRemovingAvatar(false);
                    }
                  }}
                  className="text-xs text-red-500 hover:text-red-700 font-medium mb-3 cursor-pointer"
                >
                  Remove photo
                </button>
              )}
              
              {isEditing ? (
                <div className="w-full space-y-3 mb-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 text-left mb-1">First Name</label>
                    <input 
                      type="text" 
                      value={firstname}
                      onChange={(e) => setFirstname(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600 bg-white text-gray-900"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 text-left mb-1">Last Name</label>
                    <input 
                      type="text" 
                      value={lastname}
                      onChange={(e) => setLastname(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-green-600 bg-white text-gray-900"
                    />
                  </div>
                  {error && <p className="text-red-500 text-xs">{error}</p>}
                  <div className="flex gap-2 pt-2">
                    <button 
                      onClick={() => setIsEditing(false)}
                      className="flex-1 flex items-center justify-center px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 text-sm font-medium cursor-pointer bg-white transition-colors"
                    >
                      <X className="w-4 h-4 mr-1" /> Cancel
                    </button>
                    <button 
                      onClick={handleSave}
                      disabled={saving}
                      className="flex-1 flex items-center justify-center px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm font-medium cursor-pointer disabled:opacity-60 transition-colors"
                    >
                      <Check className="w-4 h-4 mr-1" /> Save
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <h2 className="text-xl font-bold text-gray-900 mb-1">
                    {user?.firstname} {user?.lastname}
                  </h2>
                  <div className="flex items-center justify-center text-sm text-gray-500 mb-6 w-full mt-2">
                    <Mail className="w-4 h-4 mr-1 flex-shrink-0" />
                    <span className="truncate">{user?.email}</span>
                  </div>
                  
                  <button 
                    onClick={() => setIsEditing(true)}
                    className="w-full flex items-center justify-center px-4 py-2 mb-4 border border-green-200 text-green-600 rounded-md hover:bg-green-50 hover:text-green-700 hover:border-green-300 transition-colors font-medium text-sm cursor-pointer bg-white"
                    title="Edit Profile"
                  >
                    <Edit2 className="w-4 h-4 mr-2" />
                    Edit Profile Details
                  </button>
                </>
              )}
              
              <div className="w-full pt-4 border-t border-gray-100">
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center justify-center px-4 py-2 border border-red-200 text-red-600 rounded-md hover:bg-red-50 hover:text-red-700 hover:border-red-300 transition-colors font-medium text-sm cursor-pointer bg-white"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  Logout
                </button>
              </div>
            </div>
          </div>

          {/* Account Details */}
          <div className="md:col-span-2 flex flex-col gap-6">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8">
              <h3 className="text-lg font-bold text-gray-900 mb-6 flex items-center">
                <Shield className="w-5 h-5 mr-2 text-green-600" />
                Account Details
              </h3>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">

                
                <div className="bg-gray-50 rounded-lg p-5 border border-gray-100">
                  <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Date Joined</p>
                  <p className="text-base font-medium text-gray-900">{profileStats.dateJoined}</p>
                </div>

                {user?.role?.toUpperCase() === 'ADMIN' ? (
                  <div className="bg-gray-50 rounded-lg p-5 border border-gray-100">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Days Active</p>
                    <p className="text-base font-medium text-gray-900">{profileStats.daysActive} days</p>
                  </div>
                ) : (
                  <div className="bg-gray-50 rounded-lg p-5 border border-gray-100">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Repositories Created</p>
                    <p className="text-base font-medium text-gray-900">{profileStats.reposCreated}</p>
                  </div>
                )}
              </div>
            </div>

            {/* Recent Activity */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8">
              <ActivityFeed limit={3} />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
