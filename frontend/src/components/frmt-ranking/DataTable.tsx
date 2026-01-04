// DataTable.tsx
'use client'

import React, { useState, useEffect } from 'react'

export interface ColumnDefinition<T> {
  key: (keyof T & string) | string
  header: string
  renderCell: (item: T) => React.ReactNode
  headerClassName?: string
  cellClassName?: string
  headerStyle?: React.CSSProperties
  cellStyle?: React.CSSProperties
  mobileWidth?: string
  desktopWidth?: string
}

interface DataTableProps<T> {
  data: T[]
  columns: ColumnDefinition<T>[]
  onSort: (key: string) => void
  sortKey: string | null
  sortOrder: 'asc' | 'desc'
  getUniqueKey: (item: T) => React.Key
  onRowClick?: (item: T) => void
}

export default function DataTable<T>({
  data,
  columns,
  onSort,
  sortKey,
  sortOrder,
  getUniqueKey,
  onRowClick,
}: DataTableProps<T>) {
  const [isDesktop, setIsDesktop] = useState(false)

  useEffect(() => {
    const checkIsDesktop = () => setIsDesktop(window.innerWidth >= 768)
    checkIsDesktop()
    window.addEventListener('resize', checkIsDesktop)
    return () => window.removeEventListener('resize', checkIsDesktop)
  }, [])

  const getWidth = (col: ColumnDefinition<T>) => {
    return isDesktop ? col.desktopWidth || col.mobileWidth : col.mobileWidth
  }

  return (
    <div className="overflow-x-auto w-full">
      <table className="text-left border-collapse" style={{ width: isDesktop ? '100%' : 'max-content', tableLayout: isDesktop ? 'auto' : 'fixed', minWidth: '100%' }}>
        <thead className="sticky top-0 bg-primary text-white z-10 shadow-md">
          <tr className="bg-primary text-white font-bold text-lg">
            {columns.map((col, index) => (
              <th
                key={col.key}
                className={`p-3 cursor-pointer text-center ${col.headerClassName || ''}`}
                style={{
                  ...col.headerStyle,
                  width: getWidth(col)
                }}
                onClick={() => onSort(col.key)}
              >
                <span className="inline-flex items-center justify-center">
                  {col.header}
                  <span className="inline-block ml-1">
                    {sortKey === col.key ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                  </span>
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item, i) => (
            <tr
              key={getUniqueKey(item)}
              className={`
                border-b border-[#1b2d5e]
                ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50'}
                ${onRowClick ? 'cursor-pointer hover:bg-blue-50' : ''}
              `}
              onClick={onRowClick ? () => onRowClick(item) : undefined}
            >
              {columns.map((col, index) => (
                <td
                  key={`${getUniqueKey(item)}-${col.key}`}
                  className={`p-3 ${col.cellClassName || ''}`}
                  style={{
                    ...col.cellStyle,
                    width: getWidth(col)
                  }}
                >
                  {col.renderCell(item)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}