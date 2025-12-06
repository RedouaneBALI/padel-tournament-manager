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
  hasActiveFilters
}: DataPageLayoutProps) {
  return (
    <div className="flex flex-col h-[calc(100vh-56px)] bg-background overflow-hidden">
      {/* Sticky Header */}
      <div className="sticky top-0 z-20 bg-background border-b border-border px-4 py-4 shadow-sm">
        <div className="flex gap-4 items-center">
          <div className="w-full">
            <SearchInput
              search={search}
              placeholder={searchPlaceholder}
              onSearchChange={onSearchChange}
              onFilterClick={onFilterClick}
              hasActiveFilters={hasActiveFilters}
            />
          </div>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto">
        {children}
      </div>

      {/* Sticky Footer */}
      <div className="sticky bottom-16 z-20 bg-background border-t border-border px-4 py-0 space-y-0">
        <div className="flex flex-col items-center gap-1">
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
          />
          <div className="text-sm text-gray-600 text-center">
            <strong>{totalResults}</strong> résultat{totalResults > 1 ? 's' : ''} trouvé{totalResults > 1 ? 's' : ''}
          </div>
        </div>

        <footer className="text-center text-xs text-gray-500">
          {renderFooterContent}
          Last update : {scrapedAt ? new Date(scrapedAt).toLocaleString('fr-FR') : 'Non disponible'}
        </footer>
      </div>
    </div>
  )
}
