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
  filterOptions?: FilterOption[];
  sortValue?: string;
  onSortChange?: (value: string) => void;
  sortOptions?: FilterOption[];
  resultCount: number;
  totalCount: number;
};

export default function AdminTableControls({
  search,
  onSearchChange,
  filterLabel,
  filterValue,
  onFilterChange,
  filterOptions,
  sortValue,
  onSortChange,
  sortOptions,
  resultCount: _resultCount,
  totalCount: _totalCount,
}: AdminTableControlsProps) {
  return (
    <div className="flex flex-col gap-3 border-b border-gray-200 bg-white px-4 py-3 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          type="search"
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Search"
          className="w-full rounded-md border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm text-gray-900 outline-none transition focus:border-green-600 focus:ring-2 focus:ring-green-100"
        />
      </div>
      {filterOptions && filterOptions.length > 0 && (
        <select
          value={filterValue}
          onChange={(event) => onFilterChange(event.target.value)}
          aria-label={filterLabel}
          className="w-full sm:w-48 rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 outline-none transition focus:border-green-600 focus:ring-2 focus:ring-green-100"
        >
          {filterOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      )}
      {sortOptions && sortOptions.length > 0 && onSortChange && sortValue !== undefined && (
        <select
          value={sortValue}
          onChange={(event) => onSortChange(event.target.value)}
          aria-label="Sort"
          className="w-full sm:w-48 rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 outline-none transition focus:border-green-600 focus:ring-2 focus:ring-green-100"
        >
          {sortOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
