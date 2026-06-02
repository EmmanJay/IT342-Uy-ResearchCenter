import React from 'react';

interface UserAvatarProps {
  name?: string;
  email?: string;
  imageUrl?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const UserAvatar: React.FC<UserAvatarProps> = ({ 
  name, 
  email,
  imageUrl, 
  size = 'md',
  className = ''
}) => {
  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-12 h-12 text-base',
    xl: 'w-16 h-16 text-xl'
  };

  const getInitials = () => {
    if (name) {
      const parts = name.trim().split(' ');
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
      }
      return name.substring(0, 2).toUpperCase();
    }
    if (email) {
      return email.substring(0, 2).toUpperCase();
    }
    return 'U';
  };

  if (imageUrl) {
    return (
      <img 
        src={imageUrl} 
        alt={name || email || 'User Avatar'} 
        className={`${sizeClasses[size]} rounded-full object-cover border border-gray-200 ${className}`} 
      />
    );
  }

  return (
    <div 
      className={`${sizeClasses[size]} rounded-full flex items-center justify-center bg-green-100 text-green-700 font-semibold border border-green-200 ${className}`}
      title={name || email || 'User'}
    >
      {getInitials()}
    </div>
  );
};

export default UserAvatar;