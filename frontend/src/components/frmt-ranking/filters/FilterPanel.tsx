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

  const panelClasses = isOpen ? 'translate-y-0' : 'translate-y-full'

  return (
    <>
      {/* --- BACKDROP (Fond sombre) --- */}
      <div
        className={`fixed inset-0 z-40 transition-opacity duration-300 ease-in-out ${isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
        style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
        onClick={onClose}
        aria-hidden="true"
      />

      {/* --- PANEL (Conteneur Principal) --- */}
      <div
        // 1. On applique flex et flex-col directement ici
        // 2. max-h-[80vh] limite la hauteur totale
        // 3. Le footer restera collé en bas grâce à la structure flex
        className={`fixed bottom-0 left-0 right-0 z-50 flex flex-col max-h-[85vh] bg-white rounded-t-2xl transform transition-transform ease-in-out duration-300 ${panelClasses}`}
        style={{ boxShadow: '0px -4px 20px rgba(0,0,0,0.15)' }}
        role="dialog"
      >

        {/* --- HEADER (Fixe) --- */}
        <div className="flex-none flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold text-primary">Filtres</h2>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-gray-200">
            <X size={24} />
          </button>
        </div>

        {/* --- CONTENT (Scrollable) --- */}
        {/* flex-1 : prend tout l'espace disponible restant */}
        {/* overflow-y-auto : active le scroll uniquement ici */}
        {/* min-h-0 : crucial pour que le scroll flexbox fonctionne bien sur Firefox/Chrome */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6 min-h-0">
          {children(draftFilters, setDraftFilters)}
        </div>

        {/* --- FOOTER (Fixe) --- */}
        {/* flex-none : empêche le footer de s'écraser */}
        {/* Positionnement au-dessus du BottomNav avec safe-area-inset-bottom */}
        <div className="flex-none p-4 border-t bg-gray-50 flex items-center gap-3" style={{ paddingBottom: 'calc(1rem + env(safe-area-inset-bottom) + var(--bottom-nav-height, 64px))' }}>
          <button
            onClick={handleResetClick}
            className="flex-1 px-2 py-2 font-semibold text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 flex items-center justify-center gap-1"
          >
            <RotateCcw size={14} />
            Réinitialiser
          </button>
          <button
            onClick={handleApplyClick}
            className="flex-1 px-4 py-2 font-semibold text-white bg-primary rounded-md hover:bg-opacity-90 flex items-center justify-center"
          >
            Voir les résultats
          </button>
        </div>
      </div>
    </>
  )
}