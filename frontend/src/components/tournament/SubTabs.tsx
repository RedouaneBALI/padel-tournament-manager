//src/constants/views.ts
import React from 'react';
import { VIEW_CLASSEMENT, VIEW_PHASE_FINALE } from '@/src/constants/views';

type Props = {
  active: typeof VIEW_CLASSEMENT | typeof VIEW_PHASE_FINALE;
  onChange: (view: typeof VIEW_CLASSEMENT | typeof VIEW_PHASE_FINALE) => void;
};

export default function SubTabs({ active, onChange }: Props) {
  const views: Array<typeof VIEW_CLASSEMENT | typeof VIEW_PHASE_FINALE> = [VIEW_CLASSEMENT, VIEW_PHASE_FINALE];

  return (
    <div className="mb-4 border-b border-border">
      <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
        {views.map((view) => (
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