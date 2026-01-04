'use client'

import React from 'react'
import Pagination from './Pagination'
import SearchInput from './SearchInput'

type DataPageLayoutProps = {
  search: string
  searchPlaceholder: string
  onSearchChange: (value: string) => void

  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
  totalResults: number;
  scrapedAt: string | null | undefined

  children: React.ReactNode

  renderFooterContent?: React.ReactNode;
  onFilterClick?: () => void
  hasActiveFilters?: boolean
  onExportClick?: () => void
};

export default function DataPageLayout({
  search,
  searchPlaceholder,
  onSearchChange,
  currentPage,
  totalPages,
  totalResults,
  onPageChange,
  scrapedAt,
  children,
  renderFooterContent,
  onFilterClick,
  hasActiveFilters,
  onExportClick
}: DataPageLayoutProps) {
  return (
    <div className="flex flex-col h-full bg-background overflow-hidden pb-[4.25rem] pt-4">
      {/* Fixed Header */}
      <div className="flex-none bg-background px-4 my-2">
        <div className="flex gap-4 items-center">
          <div className="w-full">
            <SearchInput
              search={search}
              placeholder={searchPlaceholder}
              onSearchChange={onSearchChange}
              onFilterClick={onFilterClick}
              hasActiveFilters={hasActiveFilters}
              onExportClick={onExportClick}
            />
          </div>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto overflow-x-auto">
        {children}
      </div>

      {/* Fixed Footer */}
      <div className="flex-none bg-background border-t border-border">
        <div className="flex flex-col items-center">
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
          />
          <div className="flex items-center justify-center gap-3 pt-1 text-xs text-gray-600">
            <span>
              <strong>{totalResults}</strong> résultat{totalResults > 1 ? 's' : ''} trouvé{totalResults > 1 ? 's' : ''}
            </span>
            <span>•</span>
            <span>
              Dernier update : {scrapedAt ? new Date(scrapedAt).toLocaleString('fr-FR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : 'Non disponible'}
            </span>
          </div>
        </div>

        <footer className="text-center text-xs text-gray-500">
          {renderFooterContent}
        </footer>
      </div>
    </div>
  )
}
