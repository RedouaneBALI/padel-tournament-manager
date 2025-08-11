'use client';
import React from 'react';

type Props = {
  active: 'classement' | 'phase-finale';
  onChange: (view: 'classement' | 'phase-finale') => void;
};

export default function SubTabs({ active, onChange }: Props) {
  return (
    <div className="mb-4 border-b border-border">
      <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
        {(['classement', 'phase-finale'] as const).map((view) => (
          <button
            key={view}
            onClick={() => onChange(view)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
              active === view
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-primary'
            }`}
          >
            {view === 'classement' ? 'Poules' : 'Phase finale'}
          </button>
        ))}
      </nav>
    </div>
  );
}