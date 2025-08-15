'use client';

import { useEffect } from 'react';
import { Save, X } from 'lucide-react';

interface SaveAndCancelButtonsProps {
  isSaving?: boolean;
  bindEnter?: boolean;
  onSave: () => void;
  onCancel: () => void;
}

export default function SaveAndCancelButtons({ isSaving = false, onSave, onCancel, bindEnter = false }: SaveAndCancelButtonsProps) {
  useEffect(() => {
    if (!bindEnter || isSaving) return;

    const handler = (e: KeyboardEvent) => {
      if (e.key !== 'Enter' && e.key !== 'NumpadEnter') return;
      if (e.ctrlKey || e.metaKey || e.altKey || e.shiftKey) return;
      const target = e.target as HTMLElement | null;
      const tag = target?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'select' || tag === 'textarea') {
        e.preventDefault();
        onSave();
      }
    };

    window.addEventListener('keydown', handler, true);
    return () => {
      window.removeEventListener('keydown', handler, true);
    };
  }, [bindEnter, isSaving, onSave]);

  return (
    <div className="flex gap-2">
      <button
        disabled={isSaving}
        onClick={onCancel}
        className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60 disabled:cursor-not-allowed border border-input bg-background hover:bg-accent hover:text-accent-foreground h-9 rounded-md px-3"
        aria-label="Annuler la modification"
        title="Annuler"
        type="button"
      >
        <X className="h-3 w-3 mr-1" />
        Annuler
      </button>
      <button
        disabled={isSaving}
        onClick={onSave}
        className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60 disabled:cursor-not-allowed bg-green-600 text-on-primary hover:bg-green-700 h-9 rounded-md px-3 shadow-md"
        aria-label="Sauvegarder les modifications"
        title="Sauver"
        type="button"
      >
        <Save className="h-3 w-3 mr-1" />
        Sauver
      </button>
    </div>
  );
}
