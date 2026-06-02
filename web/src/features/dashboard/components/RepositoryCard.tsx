import { useEffect, useRef, useState } from 'react';
import type { MouseEvent } from 'react';
import { MoreVertical, Pencil, Star, Trash2 } from 'lucide-react';

interface RepositoryCardProps {
  repo: {
    id: string | number;
    name: string;
    description: string;
    memberCount: number;
    materialCount: number;
    lastActivity: string;
    favorited?: boolean;
  };
  isOwner: boolean;
  onOpen: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onToggleFavorite: () => void;
}

const RepositoryCard = ({ repo, isOwner, onOpen, onEdit, onDelete, onToggleFavorite }: RepositoryCardProps) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent | globalThis.MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMenuClick = (event: MouseEvent, handler: () => void) => {
    event.stopPropagation();
    handler();
  };

  return (
    <div
      className="bg-white rounded-lg border border-gray-200 px-5 py-4 hover:shadow-sm transition-all duration-150 cursor-pointer relative h-full flex flex-col"
      onClick={onOpen}
    >
      <div className="absolute right-3 top-3 flex items-center gap-1" ref={menuRef}>
        <button
          type="button"
          className={`h-8 w-8 rounded-full flex items-center justify-center transition-colors ${
            repo.favorited ? 'bg-amber-50 text-amber-600 hover:bg-amber-100' : 'text-gray-400 hover:bg-gray-100 hover:text-gray-600'
          }`}
          onClick={(e) => {
            e.stopPropagation();
            onToggleFavorite();
          }}
          aria-label={repo.favorited ? 'Remove from favorites' : 'Add to favorites'}
        >
          <Star className={`h-4 w-4 ${repo.favorited ? 'fill-current' : ''}`} />
        </button>

      {isOwner && (
        <>
          <button
            type="button"
            className="h-8 w-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500"
            onClick={(e) => {
              e.stopPropagation();
              setMenuOpen(prev => !prev);
            }}
            aria-label="Repository actions"
          >
            <MoreVertical className="h-4 w-4" />
          </button>
          {menuOpen && (
            <div className="absolute right-0 top-9 z-20 flex rounded-lg border border-gray-200 bg-white shadow-lg p-1">
              <button
                type="button"
                onClick={(e) => handleMenuClick(e, onEdit)}
                className="h-8 w-8 rounded-full flex items-center justify-center text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                aria-label="Edit repository"
                title="Edit repository"
              >
                <Pencil className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={(e) => handleMenuClick(e, onDelete)}
                className="h-8 w-8 rounded-full flex items-center justify-center text-red-600 hover:bg-red-50 hover:text-red-800"
                aria-label="Delete repository"
                title="Delete repository"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          )}
        </>
      )}
      </div>

      <div className="flex justify-between items-start mb-3 pr-10">
        <h3 className="text-base font-semibold text-[#212121] leading-snug line-clamp-2">
          {repo.name}
        </h3>
      </div>

      <p className="text-sm text-[#757575] line-clamp-2 leading-relaxed pr-4 min-h-[40px]">
        {repo.description || 'No description yet'}
      </p>

      <div className="mt-auto pt-4">
        <div className="border-t border-gray-200 mb-4"></div>

        <div className="grid grid-cols-2 gap-4">
          <div className="min-w-0">
            <p className="text-lg font-semibold text-gray-900 leading-none">{repo.memberCount}</p>
            <p className="text-xs text-gray-500 mt-1">Members</p>
          </div>
          <div className="min-w-0">
            <p className="text-lg font-semibold text-gray-900 leading-none">{repo.materialCount}</p>
            <p className="text-xs text-gray-500 mt-1">Materials</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RepositoryCard;
