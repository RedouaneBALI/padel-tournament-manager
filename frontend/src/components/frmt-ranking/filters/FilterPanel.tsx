// Fichier : /components/FilterPanel.tsx

'use client'

import React, { useState, useEffect } from 'react'
import { X, RotateCcw } from 'lucide-react'

type Props<T> = {
  isOpen: boolean
  onClose: () => void
  activeFilters: T
  onApply: (filters: T) => void
  children: (draftFilters: T, setDraftFilters: React.Dispatch<React.SetStateAction<T>>) => React.ReactNode
  initialFilters: T;
}

export default function FilterPanel<T>({
  isOpen,
  onClose,
  activeFilters,
  onApply,
  children,
  initialFilters,
}: Props<T>) {
  const [draftFilters, setDraftFilters] = useState<T>(activeFilters)

  useEffect(() => {
    if (isOpen) {
      setDraftFilters(activeFilters)
    }
  }, [isOpen, activeFilters])

  const handleApplyClick = () => {
    onApply(draftFilters)
    onClose()
  }

  const handleResetClick = () => {
    setDraftFilters(initialFilters);
  };

  const panelClasses = isOpen ? 'translate-x-0' : 'translate-x-full'

  return (
    <>
      <div
        className={`fixed inset-0 bg-transparent z-40 transition-opacity duration-300 ease-in-out ${isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className={`fixed top-0 right-0 h-full w-full max-w-sm bg-white shadow-xl z-50 transform transition-transform ease-in-out duration-300 ${panelClasses}`}
        role="dialog"
      >
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between p-4 border-b">
            <h2 className="text-lg font-semibold text-primary">Filtres</h2>
            <button onClick={onClose} className="p-1 rounded-full hover:bg-gray-200">
              <X size={24} />
            </button>
          </div>

          <div className="flex-grow p-4 overflow-y-auto space-y-6">
            {children(draftFilters, setDraftFilters)}
          </div>

          <div className="p-4 border-t bg-gray-50 flex items-center gap-3">
            <button
              onClick={handleResetClick}
              className="flex-1 px-4 py-2 font-semibold text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 flex items-center justify-center gap-2"
            >
              <RotateCcw size={16} />
              Réinitialiser
            </button>
            <button
              onClick={handleApplyClick}
              className="flex-1 px-4 py-2 font-semibold text-white bg-primary rounded-md hover:bg-opacity-90"
            >
              Appliquer
            </button>
          </div>
        </div>
      </div>
    </>
  )
}