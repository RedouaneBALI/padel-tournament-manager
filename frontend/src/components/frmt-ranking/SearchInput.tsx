import { SlidersHorizontal, Share, Loader2 } from 'lucide-react'
import { useState } from 'react'

type SearchInputProps = {
  search: string
  onSearchChange: (value: string) => void
  placeholder?: string
  onFilterClick?: () => void
  hasActiveFilters?: boolean
  onExportClick?: () => void
}

export default function SearchInput({
  search,
  onSearchChange,
  placeholder = 'Rechercher...',
  onFilterClick,
  hasActiveFilters,
  onExportClick,
}: SearchInputProps) {
  const [isExporting, setIsExporting] = useState(false)

  const handleExportClick = () => {
    setIsExporting(true)
    onExportClick?.()
    // Reset state after 3 seconds (should be done by then)
    setTimeout(() => setIsExporting(false), 3000)
  }

  return (
    <div className="flex gap-2 items-center">
      <div className="relative flex-1">
        <input
          type="text"
          placeholder={placeholder}
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className={`h-10 w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-[#1b2d5e] ${
            onFilterClick ? 'pr-10' : 'pr-4'
          }`}
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
      {onExportClick && (
        <button
          onClick={handleExportClick}
          disabled={isExporting}
          className={`h-10 px-4 flex items-center justify-center rounded-md focus:outline-none transition-all duration-150 ${
            isExporting
              ? 'text-[#1b2d5e] cursor-wait'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100 active:bg-gray-200'
          }`}
          aria-label="Exporter"
        >
          {isExporting ? (
            <Loader2 size={18} className="animate-spin" />
          ) : (
            <Share size={18} />
          )}
        </button>
      )}
    </div>
  )
}