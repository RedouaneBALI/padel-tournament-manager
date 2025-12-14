'use client'

import { useState, useMemo, ReactNode } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'

type Option = string

interface Props {
  label: string
  availableOptions: Option[]
  selectedOptions: Option[]
  onChange: (newSelection: Option[]) => void
  allItems?: Record<string, any>[]  // items with the field `keyField`
  keyField: string                  // e.g., "nationality" or "club"
  renderPrefix?: (option: string) => ReactNode
}

export default function MultiSelectFilter({
  label,
  availableOptions,
  selectedOptions,
  onChange,
  allItems = [],
  keyField,
  renderPrefix
}: Props) {
  const [open, setOpen] = useState(false)

  const toggleOption = (opt: string) => {
    const updated = selectedOptions.includes(opt)
      ? selectedOptions.filter(o => o !== opt)
      : [...selectedOptions, opt]
    onChange(updated)
  }

  const handleToggleAll = () => {
    if (selectedOptions.length === availableOptions.length) {
      onChange([])
    } else {
      onChange(availableOptions)
    }
  }

  const isAllSelected = selectedOptions.length === availableOptions.length

  const counts = useMemo(() => {
    return allItems.reduce<Record<string, number>>((acc, item) => {
      const key = item[keyField]
      if (!key) return acc
      acc[key] = (acc[key] || 0) + 1
      return acc
    }, {})
  }, [allItems, keyField])

  return (
    <div>
      <h3
        role="button"
        tabIndex={0}
        onClick={() => setOpen(!open)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            setOpen(!open);
          }
        }}
        className="text-base font-semibold text-gray-800 mb-2 flex justify-between items-center cursor-pointer"
      >
        <span>{label}</span>
        {open ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
      </h3>

      {open && (
        <div className="space-y-2 max-h-60 overflow-y-auto border p-2 rounded">
          <div className="mb-3 pb-2 border-b border-gray-200">
            <button
              onClick={handleToggleAll}
              className={`
                w-full px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200
                flex items-center justify-center gap-2 hover:shadow-sm
                ${isAllSelected
                  ? 'bg-red-50 text-red-700 border border-red-200 hover:bg-red-100'
                  : 'bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100'
                }
              `}
            >
              <input
                type="checkbox"
                checked={isAllSelected}
                readOnly
                className="w-4 h-4 rounded border-gray-300"
              />
              <span>{isAllSelected ? 'Tout désélectionner' : 'Tout sélectionner'}</span>
            </button>
          </div>

          {availableOptions.map((opt) => (
            <label
              key={opt}
              className="flex items-center gap-2 text-sm text-gray-800 cursor-pointer hover:bg-gray-50 p-1 rounded transition-colors duration-150"
            >
              <input
                type="checkbox"
                checked={selectedOptions.includes(opt)}
                onChange={() => toggleOption(opt)}
                onClick={(e) => e.stopPropagation()}
                className="w-4 h-4 rounded border-gray-300"
              />
              <span>
                {renderPrefix?.(opt)} {opt} ({counts[opt] || 0})
              </span>
            </label>
          ))}
        </div>
      )}
    </div>
  )
}