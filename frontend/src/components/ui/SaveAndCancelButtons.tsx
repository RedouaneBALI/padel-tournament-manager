'use client';

import { useEffect } from 'react';
import { Save, X } from 'lucide-react';
import PrimaryButton from '@/src/components/ui/buttons/PrimaryButton';
import SecondaryButton from '@/src/components/ui/buttons/SecondaryButton';

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
      <SecondaryButton disabled={isSaving} onClick={onCancel} ariaLabel="Annuler la modification">
        <X className="h-3 w-3 mr-1" />
        Annuler
      </SecondaryButton>
      <PrimaryButton disabled={isSaving} onClick={onSave} ariaLabel="Sauvegarder les modifications">
        <Save className="h-3 w-3 mr-1" />
        Sauver
      </PrimaryButton>
    </div>
  );
}
