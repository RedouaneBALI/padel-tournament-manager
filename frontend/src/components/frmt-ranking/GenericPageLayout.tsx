'use client'

import React, { useState } from 'react'
import DataPageLayout from './DataPageLayout'
import FilterButton from './filters/FilterButton'
import FilterPanel from './filters/FilterPanel'

type UseDataHookResult<TData, TFilters, TSortKey extends keyof TData | string = string> = {
  data: TData[];
  allData: TData[];
  totalPages: number;
  currentPage: number;
  setCurrentPage: React.Dispatch<React.SetStateAction<number>>;
  totalResults: number;
  search: string;
  setSearch: React.Dispatch<React.SetStateAction<string>>;
  scrapedAt?: string | null;
  sortKey: TSortKey | null;
  sortOrder: 'asc' | 'desc';
  handleSort: (key: TSortKey) => void;
  activeFilters: TFilters;
  initialFilters: TFilters;
  setActiveFilters: React.Dispatch<React.SetStateAction<TFilters>>;
};

type UseDataHook<TData, TFilters, TSortKey extends keyof TData | string = string> = (
  jsonUrl: string,
  pageSize: number
) => UseDataHookResult<TData, TFilters, TSortKey>;

type Props<TData, TFilters, TSortKey extends keyof TData | string = string> = {
  useDataHook: UseDataHook<TData, TFilters, TSortKey>;
  jsonUrl: string;
  searchPlaceholder: string;
  renderTable: (props: {
    data: TData[];
    sortKey: TSortKey | null;
    sortOrder: 'asc' | 'desc';
    onSort: (key: TSortKey) => void;
  }) => React.ReactNode;
  renderFilterContent?: (props: {
      allData: TData[];
      draftFilters: TFilters;
      setDraftFilters: React.Dispatch<React.SetStateAction<TFilters>>;
  }) => React.ReactNode;
  renderFooterContent?: React.ReactNode;
};

export default function GenericPageLayout<TData, TFilters, TSortKey extends keyof TData | string = string>({
  useDataHook,
  jsonUrl,
  searchPlaceholder,
  renderTable,
  renderFilterContent,
  renderFooterContent,
}: Props<TData, TFilters, TSortKey>) {
  const [isFilterPanelOpen, setFilterPanelOpen] = useState(false);

  const hookResult = useDataHook(jsonUrl, 100);
  const {
    data,
    allData,
    totalPages,
    currentPage,
    setCurrentPage,
    totalResults,
    search,
    setSearch,
    scrapedAt,
    sortKey,
    sortOrder,
    handleSort,
    activeFilters,
    setActiveFilters,
    initialFilters
  } = hookResult;

  const handleApplyFilters = (newFilters: TFilters) => {
    setActiveFilters(newFilters);
    setCurrentPage(1);
  };

  const filterControls = renderFilterContent ? <FilterButton onClick={() => setFilterPanelOpen(true)} /> : null;

  return (
    <>
      <DataPageLayout
        search={search}
        searchPlaceholder={searchPlaceholder}
        onSearchChange={(value) => { setSearch(value); setCurrentPage(1); }}
        currentPage={currentPage}
        totalPages={totalPages}
        totalResults={totalResults}
        onPageChange={(page) => { setCurrentPage(page); window.scrollTo({ top: 0, behavior: 'instant' }); }}
        scrapedAt={scrapedAt}
        controls={filterControls}
        renderFooterContent={renderFooterContent}
      >
        {renderTable({ data, sortKey, sortOrder, onSort: handleSort })}
      </DataPageLayout>

      {renderFilterContent && (
        <FilterPanel
          isOpen={isFilterPanelOpen}
          onClose={() => setFilterPanelOpen(false)}
          activeFilters={activeFilters}
          onApply={handleApplyFilters}
          initialFilters={initialFilters}
        >
          {(draftFilters, setDraftFilters) =>
            renderFilterContent({ allData, draftFilters, setDraftFilters })
          }
        </FilterPanel>
      )}
    </>
  );
}
