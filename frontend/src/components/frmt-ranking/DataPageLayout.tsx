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

  controls?: React.ReactNode
  renderFooterContent?: React.ReactNode;
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
  controls,
  renderFooterContent
}: DataPageLayoutProps) {
  return (
    <div className="py-4 space-y-4">
      <div className="flex flex-col md:flex-row gap-4 md:items-center md:justify-end">
        <div className="w-full md:w-auto md:max-w-xs">
          <SearchInput
            search={search}
            placeholder={searchPlaceholder}
            onSearchChange={onSearchChange}
          />
        </div>
        {controls}
      </div>

      <div>{children}</div>

      <div className="flex flex-col items-center gap-2 pt-2">
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={onPageChange}
        />
        <div className="text-sm text-gray-600 text-center">
          <strong>{totalResults}</strong> résultat{totalResults > 1 ? 's' : ''} trouvé{totalResults > 1 ? 's' : ''}
        </div>
      </div>

      <footer className="text-center text-sm text-gray-500">
        {renderFooterContent}
        Last update : {scrapedAt ? new Date(scrapedAt).toLocaleString('fr-FR') : 'Non disponible'} — By Redouane Bali
      </footer>
    </div>
  )
}
