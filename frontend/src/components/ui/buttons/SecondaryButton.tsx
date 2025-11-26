// filepath: /Users/mac-RBALI19/Documents/GitHub/padel-tournament-manager/frontend/src/components/ui/buttons/SecondaryButton.tsx
'use client';

import React from 'react';

interface Props {
  children?: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
  type?: 'button' | 'submit' | 'reset';
  ariaLabel?: string;
}

export default function SecondaryButton({ children, onClick, disabled = false, className = '', type = 'button', ariaLabel }: Props) {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={[
        'inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60 disabled:cursor-not-allowed border border-input bg-background hover:bg-accent hover:text-accent-foreground h-9 rounded-md px-3',
        className,
      ].join(' ')}
      aria-label={ariaLabel}
    >
      {children}
    </button>
  );
}

