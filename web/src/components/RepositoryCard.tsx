interface RepositoryCardProps {
  repo: {
    id: number;
    name: string;
    description: string;
    memberCount: number;
    materialCount: number;
    lastActivity: string;
  };
  isOwner: boolean;
}

const RepositoryCard = ({ repo, isOwner }: RepositoryCardProps) => (
  <div className="bg-white rounded-lg border border-[#E0E0E0] p-4 hover:shadow-md transition-shadow duration-150 cursor-pointer">
    {/* Header row */}
    <div className="flex justify-between items-start mb-2">
      <h3 className="text-sm font-semibold text-[#212121] leading-snug">
        {repo.name}
      </h3>
      {isOwner && (
        <span className="bg-[#E8F5E9] text-[#2E7D32] text-xs font-medium px-2 py-0.5 rounded ml-2 shrink-0">
          Owner
        </span>
      )}
    </div>

    {/* Description */}
    <p className="text-xs text-[#757575] mb-4 line-clamp-2 leading-relaxed">
      {repo.description}
    </p>

    {/* Divider */}
    <div className="border-t border-[#E0E0E0] mb-3"></div>

    {/* Stats row */}
    <div className="flex items-end justify-between">
      <div className="flex gap-5">
        <div>
          <p className="text-sm font-semibold text-[#212121]">{repo.memberCount}</p>
          <p className="text-xs text-[#757575]">Members</p>
        </div>
        <div>
          <p className="text-sm font-semibold text-[#212121]">{repo.materialCount}</p>
          <p className="text-xs text-[#757575]">Materials</p>
        </div>
      </div>
      <div className="text-right">
        <p className="text-xs text-[#757575]">{repo.lastActivity}</p>
        <p className="text-xs text-[#757575]">Last activity</p>
      </div>
    </div>
  </div>
);

export default RepositoryCard;
