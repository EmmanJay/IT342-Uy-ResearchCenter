import { ChevronLeft, ChevronRight, Search } from 'lucide-react';

type FilterOption = {
  label: string;
  value: string;
};

type AdminTableControlsProps = {
  search: string;
  onSearchChange: (value: string) => void;
  filterLabel: string;
  filterValue: string;
  onFilterChange: (value: string) => void;
  filterOptions: FilterOption[];
  resultCount: number;
  totalCount: number;
  currentPage: number;
  totalPages: number;
  onPreviousPage: () => void;
  onNextPage: () => void;
};

export default function AdminTableControls({
  search,
  onSearchChange,
  filterLabel,
  filterValue,
  onFilterChange,
  filterOptions,
  resultCount,
  totalCount,
  currentPage,
  totalPages,
  onPreviousPage,
  onNextPage,
}: AdminTableControlsProps) {
  return (
    <div className="flex flex-col gap-3 border-b border-gray-200 bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative w-full sm:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="search"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search"
            className="w-full rounded-md border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm text-gray-900 outline-none transition focus:border-green-600 focus:ring-2 focus:ring-green-100"
          />
        </div>
        <select
          value={filterValue}
          onChange={(event) => onFilterChange(event.target.value)}
          aria-label={filterLabel}
          className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 outline-none transition focus:border-green-600 focus:ring-2 focus:ring-green-100 sm:w-44"
        >
          {filterOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <span className="text-xs font-medium text-gray-500">
          {resultCount} of {totalCount}
        </span>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={onPreviousPage}
            disabled={currentPage === 1}
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-gray-500 transition hover:bg-gray-100 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-40"
            title="Previous page"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="min-w-16 text-center text-xs font-medium text-gray-600">
            {currentPage} / {totalPages}
          </span>
          <button
            type="button"
            onClick={onNextPage}
            disabled={currentPage === totalPages}
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-gray-500 transition hover:bg-gray-100 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-40"
            title="Next page"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
