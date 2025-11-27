'use client';

import { useEffect } from 'react';
import { Save, X } from 'lucide-react';
import Button from '@/src/components/ui/buttons/Button';

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
      <Button variant="secondary" disabled={isSaving} onClick={onCancel} aria-label="Annuler la modification">
        <X className="h-3 w-3 mr-1" />
        Annuler
      </Button>
      <Button variant="primary" disabled={isSaving} onClick={onSave} aria-label="Sauvegarder les modifications">
        <Save className="h-3 w-3 mr-1" />
        Sauver
      </Button>
    </div>
  );
}
