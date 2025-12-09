// DataTable.tsx
'use client'

import React from 'react'

export interface ColumnDefinition<T> {
  key: (keyof T & string) | string
  header: string
  renderCell: (item: T) => React.ReactNode
  headerClassName?: string
  cellClassName?: string
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

  return (
    <div>
      <table className="w-full text-left border-collapse min-w-0">
        <thead className="sticky top-0 bg-primary text-white z-10">
          <tr className="bg-primary text-white font-bold text-lg">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`p-3 cursor-pointer text-center ${col.headerClassName || ''}`}
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
              {columns.map((col) => (
                <td
                  key={`${getUniqueKey(item)}-${col.key}`}
                  className={`p-3 ${col.cellClassName || ''}`}
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