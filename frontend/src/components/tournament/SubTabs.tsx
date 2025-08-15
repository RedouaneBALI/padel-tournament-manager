'use client';
import React from 'react';
import { VIEW_CLASSEMENT, VIEW_PHASE_FINALE } from './TournamentResultsTab';

type Props = {
  active: typeof VIEW_CLASSEMENT | typeof VIEW_PHASE_FINALE;
  onChange: (view: typeof VIEW_CLASSEMENT | typeof VIEW_PHASE_FINALE) => void;
};

export default function SubTabs({ active, onChange }: Props) {
  return (
    <div className="mb-4 border-b border-border">
      <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
        {[VIEW_CLASSEMENT, VIEW_PHASE_FINALE].map((view) => (
          <button
            key={view}
            onClick={() => onChange(view)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
              active === view
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-primary'
            }`}
          >
            {view === VIEW_CLASSEMENT ? 'Pools' : 'Final phase'}
          </button>
        ))}
      </nav>
    </div>
  );
}