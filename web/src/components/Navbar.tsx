interface NavbarProps {
  showSearch?: boolean;
  showIcons?: boolean;
}

const Navbar = ({ showSearch = false, showIcons = false }: NavbarProps) => (
  <nav className="bg-white border-b border-[#E0E0E0] h-14 flex items-center px-6 gap-4">
    {!showSearch && !showIcons ? (
      // Centered navbar for auth pages
      <div className="w-full flex items-center justify-center">
        <span className="text-lg font-semibold text-[#2E7D32]">ResearchCenter</span>
      </div>
    ) : (
      <>
        <span className="text-lg font-semibold text-[#2E7D32] mr-auto">
          ResearchCenter
        </span>
        {showSearch && (
          <input
            type="text"
            placeholder="Search repositories..."
            className="px-3 py-2 rounded-md border border-[#E0E0E0] text-sm w-64 focus:outline-none focus:ring-2 focus:ring-[#2E7D32] placeholder-[#9E9E9E]"
          />
        )}
        {showIcons && (
          <div className="flex items-center gap-3">
            <button aria-label="Notifications" className="text-[#757575] hover:text-[#212121] p-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
            </button>
            <button aria-label="Profile" className="text-[#757575] hover:text-[#212121] p-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/></svg>
            </button>
          </div>
        )}
      </>
    )}
  </nav>
);

export default Navbar;
