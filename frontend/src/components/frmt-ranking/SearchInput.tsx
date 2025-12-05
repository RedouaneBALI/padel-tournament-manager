import { SlidersHorizontal } from 'lucide-react'

type SearchInputProps = {
  search: string
  onSearchChange: (value: string) => void
  placeholder?: string
  onFilterClick?: () => void
  hasActiveFilters?: boolean
}

export default function SearchInput({
  search,
  onSearchChange,
  placeholder = 'Rechercher...',
  onFilterClick,
  hasActiveFilters,
}: SearchInputProps) {
  return (
    <div className="relative">
      <input
        type="text"
        placeholder={placeholder}
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        className="h-10 w-full px-4 py-2 pr-10 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-[#1b2d5e]"
      />
      {onFilterClick && (
        <button
          onClick={onFilterClick}
          className="absolute right-2 top-1/2 transform -translate-y-1/2 p-1 rounded-md hover:bg-gray-100 focus:outline-none"
        >
          <div className="relative">
            <SlidersHorizontal size={16} className={hasActiveFilters ? 'text-[#1b2d5e]' : 'text-gray-500'} />
            {hasActiveFilters && (
              <div className="absolute -top-1 -right-1 w-2 h-2 bg-[#1b2d5e] rounded-full"></div>
            )}
          </div>
        </button>
      )}
    </div>
  )
}